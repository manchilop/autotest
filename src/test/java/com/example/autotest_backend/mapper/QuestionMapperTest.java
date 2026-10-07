package com.example.autotest_backend.mapper;

import com.example.autotest_backend.dto.question.PracticeQuestionResponse;
import com.example.autotest_backend.dto.question.QuestionResponse;
import com.example.autotest_backend.model.Choice;
import com.example.autotest_backend.model.Question;
import com.example.autotest_backend.model.QuestionStatus;
import com.example.autotest_backend.model.Subject;
import com.example.autotest_backend.model.Topic;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the MapStruct mapper, exercised through its generated
 * implementation so no Spring context is needed.
 *
 * <p>The mapper carries three pieces of real behaviour worth protecting: it
 * flattens the topic and subject of a question into the response, it strips the
 * {@code correct} flag from the practice response a student receives, and it
 * keeps the review status so the client can warn the student when a question has
 * not been validated by a teacher yet.
 */
@DisplayName("QuestionMapper")
class QuestionMapperTest {

    private QuestionMapper questionMapper;

    @BeforeEach
    void setUp() {
        questionMapper = new QuestionMapperImpl();
    }

    private static Question questionWithTopic() {
        User teacher = User.builder()
                .id(1L).email("teacher@example.com").role(UserRole.TEACHER)
                .password("hashed").name("Teacher Tester").build();
        Subject subject = Subject.builder()
                .id(10L).name("Mathematics").inviteCode("ABC123").owner(teacher).build();
        Topic topic = Topic.builder().id(20L).name("Algebra").subject(subject).build();

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

    @Test
    @DisplayName("toResponse flattens topic and subject into the response")
    void toResponseFlattensTopicAndSubject() {
        // Act
        QuestionResponse response = questionMapper.toResponse(questionWithTopic());

        // Assert
        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getQuestionText()).isEqualTo("What is 2 + 2?");
        assertThat(response.getTopicId()).isEqualTo(20L);
        assertThat(response.getTopicName()).isEqualTo("Algebra");
        assertThat(response.getSubjectId()).isEqualTo(10L);
        assertThat(response.getSubjectName()).isEqualTo("Mathematics");
    }

    @Test
    @DisplayName("toResponse keeps the correct flag, since teachers review answers")
    void toResponseKeepsCorrectFlag() {
        // Act
        QuestionResponse response = questionMapper.toResponse(questionWithTopic());

        // Assert
        assertThat(response.getChoices())
                .extracting("choiceText", "correct")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("4", true),
                        org.assertj.core.groups.Tuple.tuple("5", false));
    }

    @Test
    @DisplayName("toResponse leaves topic and subject fields empty for an orphan question")
    void toResponseHandlesMissingTopic() {
        // Arrange: topic is nullable in the entity
        Question orphan = Question.builder()
                .id(8L).questionText("Unassigned question").status(QuestionStatus.PENDING).build();

        // Act
        QuestionResponse response = questionMapper.toResponse(orphan);

        // Assert
        assertThat(response.getTopicId()).isNull();
        assertThat(response.getTopicName()).isNull();
        assertThat(response.getSubjectId()).isNull();
        assertThat(response.getSubjectName()).isNull();
    }

    @Test
    @DisplayName("toPracticeResponse never exposes which choice is correct")
    void practiceResponseHidesCorrectAnswer() {
        // Act
        PracticeQuestionResponse response = questionMapper.toPracticeResponse(questionWithTopic());

        // Assert: the practice DTO carries ids and texts only
        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getChoices())
                .extracting("id", "choiceText")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(100L, "4"),
                        org.assertj.core.groups.Tuple.tuple(101L, "5"));
        assertThat(response.getChoices().get(0).getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("correct");
    }

    @Test
    @DisplayName("toPracticeResponse keeps the review status so the client can warn the student")
    void practiceResponseKeepsReviewStatus() {
        // Arrange: a question still awaiting the teacher's review
        Question pending = questionWithTopic();
        pending.setStatus(QuestionStatus.PENDING);

        // Act + Assert
        assertThat(questionMapper.toPracticeResponse(pending).getStatus())
                .isEqualTo(QuestionStatus.PENDING);
        assertThat(questionMapper.toPracticeResponse(questionWithTopic()).getStatus())
                .isEqualTo(QuestionStatus.APPROVED);
    }

    @Test
    @DisplayName("mapping a null question yields null instead of throwing")
    void mapsNullSafely() {
        // Act + Assert
        assertThat(questionMapper.toResponse((Question) null)).isNull();
        assertThat(questionMapper.toResponse((Choice) null)).isNull();
        assertThat(questionMapper.toPracticeResponse(null)).isNull();
    }
}
