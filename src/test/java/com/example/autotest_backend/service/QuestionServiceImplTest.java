package com.example.autotest_backend.service;

import com.example.autotest_backend.model.Choice;
import com.example.autotest_backend.model.Question;
import com.example.autotest_backend.model.QuestionStatus;
import com.example.autotest_backend.model.Subject;
import com.example.autotest_backend.model.Topic;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.repository.QuestionRepository;
import com.example.autotest_backend.repository.SubjectMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QuestionServiceImpl}.
 *
 * <p>The interesting behaviour is the fallback chain used to pick the next
 * practice question, the answer flow, which must report correctness and record
 * completion exactly once, and the resource-level authorisation that keeps one
 * subject isolated from another: belonging to the subject is required to create
 * or answer a question, and owning it is required to moderate.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionServiceImpl")
class QuestionServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long SUBJECT_ID = 42L;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SubjectMembershipRepository membershipRepository;

    @Mock
    private UserQuestionServiceImpl userQuestionService;

    private QuestionServiceImpl questionService;

    private User student;
    private User owner;
    private User outsiderTeacher;
    private Subject subject;
    private Topic topic;

    @BeforeEach
    void setUp() {
        questionService = new QuestionServiceImpl(questionRepository, membershipRepository, userQuestionService);

        student = User.builder()
                .id(USER_ID).email("student@example.com").role(UserRole.STUDENT)
                .password("hashed").name("Student Tester").build();

        owner = User.builder()
                .id(50L).email("owner@example.com").role(UserRole.TEACHER)
                .password("hashed").name("Owning Teacher").build();

        // A teacher of a different subject: same role, no relation to this resource
        outsiderTeacher = User.builder()
                .id(51L).email("outsider@example.com").role(UserRole.TEACHER)
                .password("hashed").name("Outsider Teacher").build();

        subject = Subject.builder()
                .id(SUBJECT_ID).name("Mathematics").inviteCode("ABC123").owner(owner).build();
        topic = Topic.builder().id(60L).name("Algebra").subject(subject).build();
    }

    /** Marks the user as belonging, or not, to the subject under test. */
    private void memberOfSubject(User user, boolean member) {
        when(membershipRepository.existsByUserAndSubject(user, subject)).thenReturn(member);
    }

    private Question questionWithChoices() {
        Question question = Question.builder()
                .id(7L)
                .questionText("What is 2 + 2?")
                .status(QuestionStatus.APPROVED)
                .topic(topic)
                .build();
        question.addChoice(Choice.builder().id(100L).choiceText("4").correct(true).build());
        question.addChoice(Choice.builder().id(101L).choiceText("5").correct(false).build());
        return question;
    }

    @Nested
    @DisplayName("createQuestion")
    class CreateQuestion {

        @Test
        @DisplayName("persists the question through the repository")
        void persistsQuestion() {
            // Arrange
            Question question = questionWithChoices();
            memberOfSubject(student, true);
            when(questionRepository.save(question)).thenReturn(question);

            // Act
            Question result = questionService.createQuestion(question, student);

            // Assert
            assertThat(result).isSameAs(question);
            verify(questionRepository).save(question);
        }

        @Test
        @DisplayName("refuses a question with no correct choice")
        void rejectsQuestionWithoutCorrectChoice() {
            // Arrange: every choice marked as wrong, so nobody could ever answer it
            Question question = Question.builder()
                    .id(7L).questionText("Q").status(QuestionStatus.PENDING).topic(topic).build();
            question.addChoice(Choice.builder().id(100L).choiceText("a").correct(false).build());
            question.addChoice(Choice.builder().id(101L).choiceText("b").correct(false).build());
            memberOfSubject(student, true);

            // Act + Assert
            assertThatThrownBy(() -> questionService.createQuestion(question, student))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one correct choice");

            verify(questionRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses a question with more than one correct choice")
        void rejectsQuestionWithSeveralCorrectChoices() {
            // Arrange: two right answers would make any of them report success
            Question question = Question.builder()
                    .id(7L).questionText("Q").status(QuestionStatus.PENDING).topic(topic).build();
            question.addChoice(Choice.builder().id(100L).choiceText("a").correct(true).build());
            question.addChoice(Choice.builder().id(101L).choiceText("b").correct(true).build());
            memberOfSubject(student, true);

            // Act + Assert
            assertThatThrownBy(() -> questionService.createQuestion(question, student))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one correct choice");

            verify(questionRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses to create a question in a subject the student does not belong to")
        void rejectsCreationOutsideOwnSubjects() {
            // Arrange: the topic id belongs to a subject this student never joined
            Question question = questionWithChoices();
            memberOfSubject(student, false);

            // Act + Assert
            assertThatThrownBy(() -> questionService.createQuestion(question, student))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not a member of this subject");

            // Nothing may be persisted when the membership check fails
            verify(questionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("read operations")
    class ReadOperations {

        @Test
        @DisplayName("getAllQuestions only returns questions of the requester's subjects")
        void getAllQuestionsIsScopedToTheRequester() {
            // Arrange
            Question question = questionWithChoices();
            when(questionRepository.findAllForUser(owner.getId())).thenReturn(List.of(question));

            // Act + Assert
            assertThat(questionService.getAllQuestions(owner)).containsExactly(question);
            verify(questionRepository).findAllForUser(owner.getId());
            verify(questionRepository, never()).findAll();
        }

        @Test
        @DisplayName("getQuestionsByStatus filters by status within the requester's subjects")
        void getQuestionsByStatusIsScopedToTheRequester() {
            // Arrange
            Question question = questionWithChoices();
            when(questionRepository.findByStatusForUser(QuestionStatus.PENDING, owner.getId()))
                    .thenReturn(List.of(question));

            // Act + Assert
            assertThat(questionService.getQuestionsByStatus(QuestionStatus.PENDING, owner))
                    .containsExactly(question);
            verify(questionRepository).findByStatusForUser(QuestionStatus.PENDING, owner.getId());
            verify(questionRepository, never()).findByStatus(any());
        }
    }

    @Nested
    @DisplayName("getNextQuestion across all subjects")
    class GetNextQuestionAcrossSubjects {

        @Test
        @DisplayName("prefers an unanswered approved question")
        void prefersUnansweredApproved() {
            // Arrange
            Question approved = questionWithChoices();
            when(questionRepository.findRandomUnansweredApprovedByUser(USER_ID))
                    .thenReturn(Optional.of(approved));

            // Act
            Question result = questionService.getNextQuestion(USER_ID, null);

            // Assert: the cheaper fallbacks are not even consulted
            assertThat(result).isSameAs(approved);
            verify(questionRepository, never()).findRandomUnansweredPendingByUser(any());
            verify(questionRepository, never()).findRandomApprovedForUser(any());
        }

        @Test
        @DisplayName("falls back to an unanswered pending question")
        void fallsBackToUnansweredPending() {
            // Arrange
            Question pending = questionWithChoices();
            when(questionRepository.findRandomUnansweredApprovedByUser(USER_ID))
                    .thenReturn(Optional.empty());
            when(questionRepository.findRandomUnansweredPendingByUser(USER_ID))
                    .thenReturn(Optional.of(pending));

            // Act + Assert
            assertThat(questionService.getNextQuestion(USER_ID, null)).isSameAs(pending);
            verify(questionRepository, never()).findRandomApprovedForUser(any());
        }

        @Test
        @DisplayName("falls back to repeating an already answered approved question")
        void fallsBackToAlreadyAnsweredApproved() {
            // Arrange
            Question repeated = questionWithChoices();
            when(questionRepository.findRandomUnansweredApprovedByUser(USER_ID))
                    .thenReturn(Optional.empty());
            when(questionRepository.findRandomUnansweredPendingByUser(USER_ID))
                    .thenReturn(Optional.empty());
            when(questionRepository.findRandomApprovedForUser(USER_ID))
                    .thenReturn(Optional.of(repeated));

            // Act + Assert
            assertThat(questionService.getNextQuestion(USER_ID, null)).isSameAs(repeated);
        }

        @Test
        @DisplayName("throws IllegalStateException when no question is available at all")
        void throwsWhenNothingAvailable() {
            // Arrange
            when(questionRepository.findRandomUnansweredApprovedByUser(USER_ID))
                    .thenReturn(Optional.empty());
            when(questionRepository.findRandomUnansweredPendingByUser(USER_ID))
                    .thenReturn(Optional.empty());
            when(questionRepository.findRandomApprovedForUser(USER_ID))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> questionService.getNextQuestion(USER_ID, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No questions available");
        }
    }

    @Nested
    @DisplayName("getNextQuestion scoped to one subject")
    class GetNextQuestionForSubject {

        @Test
        @DisplayName("uses the subject-scoped queries when a subject id is given")
        void usesSubjectScopedQueries() {
            // Arrange
            Question approved = questionWithChoices();
            when(questionRepository.findRandomUnansweredApprovedByUserAndSubject(USER_ID, SUBJECT_ID))
                    .thenReturn(Optional.of(approved));

            // Act
            Question result = questionService.getNextQuestion(USER_ID, SUBJECT_ID);

            // Assert: the unscoped queries must not be used
            assertThat(result).isSameAs(approved);
            verify(questionRepository, never()).findRandomUnansweredApprovedByUser(any());
        }

        @Test
        @DisplayName("falls back to pending, then to an already answered approved question")
        void fallsBackWithinSubject() {
            // Arrange
            Question repeated = questionWithChoices();
            when(questionRepository.findRandomUnansweredApprovedByUserAndSubject(USER_ID, SUBJECT_ID))
                    .thenReturn(Optional.empty());
            when(questionRepository.findRandomUnansweredPendingByUserAndSubject(USER_ID, SUBJECT_ID))
                    .thenReturn(Optional.empty());
            when(questionRepository.findRandomApprovedForUserAndSubject(USER_ID, SUBJECT_ID))
                    .thenReturn(Optional.of(repeated));

            // Act + Assert
            assertThat(questionService.getNextQuestion(USER_ID, SUBJECT_ID)).isSameAs(repeated);
        }

        @Test
        @DisplayName("throws IllegalStateException when the subject has no usable question")
        void throwsWhenSubjectHasNothing() {
            // Arrange
            when(questionRepository.findRandomUnansweredApprovedByUserAndSubject(USER_ID, SUBJECT_ID))
                    .thenReturn(Optional.empty());
            when(questionRepository.findRandomUnansweredPendingByUserAndSubject(USER_ID, SUBJECT_ID))
                    .thenReturn(Optional.empty());
            when(questionRepository.findRandomApprovedForUserAndSubject(USER_ID, SUBJECT_ID))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> questionService.getNextQuestion(USER_ID, SUBJECT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No questions available");
        }
    }

    @Nested
    @DisplayName("answerQuestion")
    class AnswerQuestion {

        @Test
        @DisplayName("reports true and records completion for the correct choice")
        void reportsCorrectAnswer() {
            // Arrange
            Question question = questionWithChoices();
            when(questionRepository.findById(7L)).thenReturn(Optional.of(question));
            memberOfSubject(student, true);
            when(userQuestionService.hasUserCompletedQuestion(student, question)).thenReturn(false);

            // Act
            boolean correct = questionService.answerQuestion(7L, 100L, student);

            // Assert
            assertThat(correct).isTrue();
            verify(userQuestionService).markAsCompleted(student, question);
        }

        @Test
        @DisplayName("reports false for a wrong choice but still records completion")
        void reportsWrongAnswer() {
            // Arrange
            Question question = questionWithChoices();
            when(questionRepository.findById(7L)).thenReturn(Optional.of(question));
            memberOfSubject(student, true);
            when(userQuestionService.hasUserCompletedQuestion(student, question)).thenReturn(false);

            // Act
            boolean correct = questionService.answerQuestion(7L, 101L, student);

            // Assert
            assertThat(correct).isFalse();
            verify(userQuestionService).markAsCompleted(student, question);
        }

        @Test
        @DisplayName("does not record completion twice for the same question")
        void doesNotRecordCompletionTwice() {
            // Arrange
            Question question = questionWithChoices();
            when(questionRepository.findById(7L)).thenReturn(Optional.of(question));
            memberOfSubject(student, true);
            when(userQuestionService.hasUserCompletedQuestion(student, question)).thenReturn(true);

            // Act
            boolean correct = questionService.answerQuestion(7L, 100L, student);

            // Assert
            assertThat(correct).isTrue();
            verify(userQuestionService, never()).markAsCompleted(any(), any());
        }

        @Test
        @DisplayName("rejects a choice id that does not belong to the question")
        void rejectsUnknownChoice() {
            // Arrange
            Question question = questionWithChoices();
            when(questionRepository.findById(7L)).thenReturn(Optional.of(question));
            memberOfSubject(student, true);

            // Act + Assert
            assertThatThrownBy(() -> questionService.answerQuestion(7L, 999L, student))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Choice not found");

            verify(userQuestionService, never()).markAsCompleted(any(), any());
        }

        @Test
        @DisplayName("refuses to answer a question of a subject the student does not belong to")
        void rejectsAnsweringOutsideOwnSubjects() {
            // Arrange: a valid question id, but from somebody else's subject
            Question question = questionWithChoices();
            when(questionRepository.findById(7L)).thenReturn(Optional.of(question));
            memberOfSubject(student, false);

            // Act + Assert
            assertThatThrownBy(() -> questionService.answerQuestion(7L, 100L, student))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not a member of this subject");

            // The attempt must leave no trace in the student's progress
            verify(userQuestionService, never()).markAsCompleted(any(), any());
        }

        @Test
        @DisplayName("rejects an unknown question id")
        void rejectsUnknownQuestion() {
            // Arrange
            when(questionRepository.findById(404L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> questionService.answerQuestion(404L, 100L, student))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Question not found");
        }
    }

    @Nested
    @DisplayName("moderation")
    class Moderation {

        private Question pendingQuestion() {
            return Question.builder()
                    .id(7L).questionText("Q").status(QuestionStatus.PENDING).topic(topic).build();
        }

        @Test
        @DisplayName("approveQuestion moves the question to APPROVED")
        void approvesQuestion() {
            // Arrange
            Question question = pendingQuestion();
            when(questionRepository.findById(7L)).thenReturn(Optional.of(question));

            // Act: the teacher who owns the subject
            Question result = questionService.approveQuestion(7L, owner);

            // Assert
            assertThat(result.getStatus()).isEqualTo(QuestionStatus.APPROVED);
        }

        @Test
        @DisplayName("rejectQuestion moves the question to REJECTED")
        void rejectsQuestion() {
            // Arrange
            Question question = pendingQuestion();
            when(questionRepository.findById(7L)).thenReturn(Optional.of(question));

            // Act
            Question result = questionService.rejectQuestion(7L, owner);

            // Assert
            assertThat(result.getStatus()).isEqualTo(QuestionStatus.REJECTED);
        }

        @Test
        @DisplayName("approveQuestion rejects an unknown question id")
        void approveRejectsUnknownQuestion() {
            // Arrange
            when(questionRepository.findById(404L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> questionService.approveQuestion(404L, owner))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Question not found");
        }

        @Test
        @DisplayName("a teacher cannot approve a question of another teacher's subject")
        void approveRejectsForeignSubject() {
            // Arrange: a valid question id, moderated by a teacher who owns nothing here
            Question question = pendingQuestion();
            when(questionRepository.findById(7L)).thenReturn(Optional.of(question));

            // Act + Assert
            assertThatThrownBy(() -> questionService.approveQuestion(7L, outsiderTeacher))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("do not own this subject");

            // The state must remain untouched after a refused moderation
            assertThat(question.getStatus()).isEqualTo(QuestionStatus.PENDING);
        }

        @Test
        @DisplayName("a teacher cannot reject a question of another teacher's subject")
        void rejectRejectsForeignSubject() {
            // Arrange
            Question question = pendingQuestion();
            when(questionRepository.findById(7L)).thenReturn(Optional.of(question));

            // Act + Assert
            assertThatThrownBy(() -> questionService.rejectQuestion(7L, outsiderTeacher))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("do not own this subject");

            assertThat(question.getStatus()).isEqualTo(QuestionStatus.PENDING);
        }
    }
}
