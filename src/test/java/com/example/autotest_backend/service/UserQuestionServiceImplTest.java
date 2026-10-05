package com.example.autotest_backend.service;

import com.example.autotest_backend.model.Question;
import com.example.autotest_backend.model.QuestionStatus;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserQuestion;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.repository.UserQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserQuestionServiceImpl}, which records that a user has
 * completed a question and protects that record against duplicates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserQuestionServiceImpl")
class UserQuestionServiceImplTest {

    @Mock
    private UserQuestionRepository userQuestionRepository;

    private UserQuestionServiceImpl userQuestionService;

    private User student;
    private Question question;

    @BeforeEach
    void setUp() {
        userQuestionService = new UserQuestionServiceImpl(userQuestionRepository);

        student = User.builder()
                .id(1L).email("student@example.com").role(UserRole.STUDENT)
                .password("hashed").name("Student Tester").build();

        question = Question.builder()
                .id(7L).questionText("What is 2 + 2?").status(QuestionStatus.APPROVED).build();
    }

    @Test
    @DisplayName("markAsCompleted links the given user and question")
    void marksQuestionAsCompleted() {
        // Arrange
        when(userQuestionRepository.save(any(UserQuestion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        UserQuestion result = userQuestionService.markAsCompleted(student, question);

        // Assert
        ArgumentCaptor<UserQuestion> captor = ArgumentCaptor.forClass(UserQuestion.class);
        verify(userQuestionRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(student);
        assertThat(captor.getValue().getQuestion()).isEqualTo(question);
        assertThat(result.getUser()).isEqualTo(student);
    }

    @Test
    @DisplayName("markAsCompleted translates a duplicate insert into IllegalStateException")
    void translatesDuplicateIntoIllegalState() {
        // Arrange: the unique constraint on (user_id, question_id) rejects the insert
        when(userQuestionRepository.save(any(UserQuestion.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        // Act + Assert
        assertThatThrownBy(() -> userQuestionService.markAsCompleted(student, question))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Question already completed by user");
    }

    @Test
    @DisplayName("hasUserCompletedQuestion delegates to the repository")
    void hasUserCompletedQuestionDelegates() {
        // Arrange
        when(userQuestionRepository.existsByUserAndQuestion(student, question)).thenReturn(true);

        // Act + Assert
        assertThat(userQuestionService.hasUserCompletedQuestion(student, question)).isTrue();
    }

    @Test
    @DisplayName("getCompletedQuestionsByUser returns the user's completion records")
    void getCompletedQuestionsByUserDelegates() {
        // Arrange
        UserQuestion completed = UserQuestion.builder()
                .id(1L).user(student).question(question).build();
        when(userQuestionRepository.findByUser(student)).thenReturn(List.of(completed));

        // Act + Assert
        assertThat(userQuestionService.getCompletedQuestionsByUser(student))
                .containsExactly(completed);
    }
}
