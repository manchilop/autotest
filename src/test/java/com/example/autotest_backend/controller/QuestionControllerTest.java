package com.example.autotest_backend.controller;

import com.example.autotest_backend.config.SecurityConfig;
import com.example.autotest_backend.mapper.QuestionMapperImpl;
import com.example.autotest_backend.model.Choice;
import com.example.autotest_backend.model.Question;
import com.example.autotest_backend.model.QuestionStatus;
import com.example.autotest_backend.model.Subject;
import com.example.autotest_backend.model.Topic;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.repository.TopicRepository;
import com.example.autotest_backend.security.JwtFilter;
import com.example.autotest_backend.security.JwtUtil;
import com.example.autotest_backend.service.QuestionService;
import com.example.autotest_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link QuestionController}.
 *
 * <p>The real MapStruct mapper is used instead of a mock, so the JSON a client
 * actually receives is asserted, including the guarantee that the practice
 * endpoint never reveals which choice is the correct one.
 */
@WebMvcTest(controllers = QuestionController.class)
@Import({SecurityConfig.class, JwtFilter.class, QuestionMapperImpl.class})
@DisplayName("QuestionController")
class QuestionControllerTest {

    private static final String TEACHER_EMAIL = "teacher@example.com";
    private static final String STUDENT_EMAIL = "student@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService questionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TopicRepository topicRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User student;
    private User teacher;
    private Topic algebra;

    @BeforeEach
    void setUp() {
        teacher = User.builder()
                .id(1L).email(TEACHER_EMAIL).role(UserRole.TEACHER)
                .password("hashed").name("Teacher Tester").build();
        student = User.builder()
                .id(2L).email(STUDENT_EMAIL).role(UserRole.STUDENT)
                .password("hashed").name("Student Tester").build();

        Subject mathematics = Subject.builder()
                .id(10L).name("Mathematics").inviteCode("ABC123").owner(teacher).build();
        algebra = Topic.builder().id(20L).name("Algebra").subject(mathematics).build();

        // Every endpoint now resolves the caller from the token, because the
        // service layer needs it to authorise access to the resource itself.
        lenient().when(userService.getUserByEmail(TEACHER_EMAIL)).thenReturn(Optional.of(teacher));
        lenient().when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
    }

    private Question storedQuestion() {
        Question question = Question.builder()
                .id(7L)
                .questionText("What is 2 + 2?")
                .status(QuestionStatus.APPROVED)
                .topic(algebra)
                .build();
        question.addChoice(Choice.builder().id(100L).choiceText("4").correct(true).build());
        question.addChoice(Choice.builder().id(101L).choiceText("5").correct(false).build());
        return question;
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/questions answers 201 and attaches choices and topic")
    void createsQuestionWithChoicesAndTopic() throws Exception {
        // Arrange
        when(topicRepository.findById(20L)).thenReturn(Optional.of(algebra));
        when(questionService.createQuestion(any(Question.class), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionText": "What is 2 + 2?",
                                  "topicId": 20,
                                  "choices": [
                                    {"choiceText": "4", "correct": true},
                                    {"choiceText": "5", "correct": false}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionText").value("What is 2 + 2?"))
                .andExpect(jsonPath("$.topicId").value(20))
                .andExpect(jsonPath("$.topicName").value("Algebra"))
                .andExpect(jsonPath("$.subjectId").value(10))
                .andExpect(jsonPath("$.subjectName").value("Mathematics"))
                .andExpect(jsonPath("$.choices", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.choices[0].choiceText").value("4"))
                .andExpect(jsonPath("$.choices[0].correct").value(true));

        // Assert: the entity handed to the service carries both choices and the topic
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionService).createQuestion(captor.capture(), any(User.class));
        Question submitted = captor.getValue();
        assertThat(submitted.getTopic()).isEqualTo(algebra);
        assertThat(submitted.getChoices()).hasSize(2);
        assertThat(submitted.getChoices().get(0).getQuestion()).isSameAs(submitted);
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/questions answers 400 for an unknown topic")
    void rejectsUnknownTopic() throws Exception {
        // Arrange
        when(topicRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionText": "Orphan question",
                                  "topicId": 999,
                                  "choices": [{"choiceText": "4", "correct": true},
                                              {"choiceText": "5", "correct": false}]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Topic not found"));

        verify(questionService, never()).createQuestion(any(), any());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/questions answers 400 when the text is blank")
    void rejectsBlankQuestionText() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionText": "  ", "choices": [{"choiceText": "4", "correct": true}]}
                                """))
                .andExpect(status().isBadRequest());

        verify(questionService, never()).createQuestion(any(), any());
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL, roles = "TEACHER")
    @DisplayName("POST /api/questions is forbidden for teachers")
    void teacherCannotSubmitQuestion() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionText": "Q", "choices": [{"choiceText": "4", "correct": true}]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL, roles = "TEACHER")
    @DisplayName("GET /api/questions returns every question for a teacher")
    void teacherListsAllQuestions() throws Exception {
        // Arrange
        when(questionService.getAllQuestions(any(User.class))).thenReturn(List.of(storedQuestion()));

        // Act + Assert
        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].questionText").value("What is 2 + 2?"));

        verify(questionService).getAllQuestions(any(User.class));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/questions rejects a body without choices with 400")
    void rejectsMissingChoices() throws Exception {
        // Regression test: this used to surface as a 500 because the field was
        // not constrained, so the failure happened deeper than the validation.
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionText": "No choices at all",
                                  "topicId": 20
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("choices")));

        verify(questionService, never()).createQuestion(any(), any());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/questions rejects a blank choice text with 400")
    void rejectsBlankChoiceText() throws Exception {
        // Regression test: without @Valid on the collection the constraints of
        // each choice were never evaluated and an empty option was accepted.
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionText": "Blank option",
                                  "topicId": 20,
                                  "choices": [{"choiceText": "", "correct": true},
                                              {"choiceText": "5", "correct": false}]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(questionService, never()).createQuestion(any(), any());
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL, roles = "TEACHER")
    @DisplayName("GET /api/questions rejects an unknown status value with 400")
    void rejectsInvalidStatusValue() throws Exception {
        // Regression test: an unconvertible enum used to end up as a 500
        mockMvc.perform(get("/api/questions").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));

        verify(questionService, never()).getQuestionsByStatus(any(), any());
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL, roles = "TEACHER")
    @DisplayName("GET /api/questions?status=PENDING filters by status")
    void teacherFiltersQuestionsByStatus() throws Exception {
        // Arrange
        when(questionService.getQuestionsByStatus(eq(QuestionStatus.PENDING), any(User.class)))
                .thenReturn(List.of(storedQuestion()));

        // Act + Assert
        mockMvc.perform(get("/api/questions").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

        verify(questionService).getQuestionsByStatus(eq(QuestionStatus.PENDING), any(User.class));
        verify(questionService, never()).getAllQuestions(any());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("GET /api/questions is forbidden for students")
    void studentCannotListAllQuestions() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("GET /api/questions/next hides which choice is correct")
    void nextQuestionHidesCorrectChoice() throws Exception {
        // Arrange
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(questionService.getNextQuestion(2L, null)).thenReturn(storedQuestion());

        // Act + Assert
        mockMvc.perform(get("/api/questions/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.questionText").value("What is 2 + 2?"))
                .andExpect(jsonPath("$.choices", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.choices[0].choiceText").value("4"))
                .andExpect(jsonPath("$.choices[0].correct").doesNotExist())
                .andExpect(jsonPath("$.choices[1].correct").doesNotExist());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("GET /api/questions/next passes the subject filter through")
    void nextQuestionAcceptsSubjectFilter() throws Exception {
        // Arrange
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(questionService.getNextQuestion(2L, 10L)).thenReturn(storedQuestion());

        // Act + Assert
        mockMvc.perform(get("/api/questions/next").param("subjectId", "10"))
                .andExpect(status().isOk());

        verify(questionService).getNextQuestion(eq(2L), eq(10L));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("GET /api/questions/next answers 409 when nothing is available")
    void nextQuestionAnswersConflictWhenEmpty() throws Exception {
        // Arrange
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(questionService.getNextQuestion(2L, null))
                .thenThrow(new IllegalStateException("No questions available"));

        // Act + Assert
        mockMvc.perform(get("/api/questions/next"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No questions available"));
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL, roles = "TEACHER")
    @DisplayName("GET /api/questions/next is forbidden for teachers")
    void teacherCannotPractise() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/questions/next"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/questions/{id}/answer reports whether the choice was correct")
    void answerReportsCorrectness() throws Exception {
        // Arrange
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(questionService.answerQuestion(7L, 100L, student)).thenReturn(true);

        // Act + Assert
        mockMvc.perform(post("/api/questions/7/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"choiceId": 100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/questions/{id}/answer answers 400 for an unknown choice")
    void answerRejectsUnknownChoice() throws Exception {
        // Arrange
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(questionService.answerQuestion(7L, 999L, student))
                .thenThrow(new IllegalArgumentException("Choice not found"));

        // Act + Assert
        mockMvc.perform(post("/api/questions/7/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"choiceId": 999}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Choice not found"));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/questions/{id}/answer answers 400 when no choice is sent")
    void answerRejectsMissingChoiceId() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/questions/7/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(questionService, never()).answerQuestion(any(), any(), any());
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL, roles = "TEACHER")
    @DisplayName("PATCH /api/questions/{id}/approve approves the question")
    void teacherApprovesQuestion() throws Exception {
        // Arrange
        when(questionService.approveQuestion(eq(7L), any(User.class))).thenReturn(storedQuestion());

        // Act + Assert
        mockMvc.perform(patch("/api/questions/7/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(questionService).approveQuestion(eq(7L), any(User.class));
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL, roles = "TEACHER")
    @DisplayName("PATCH /api/questions/{id}/reject rejects the question")
    void teacherRejectsQuestion() throws Exception {
        // Arrange
        when(questionService.rejectQuestion(eq(7L), any(User.class))).thenReturn(storedQuestion());

        // Act + Assert
        mockMvc.perform(patch("/api/questions/7/reject"))
                .andExpect(status().isOk());

        verify(questionService).rejectQuestion(eq(7L), any(User.class));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("PATCH /api/questions/{id}/approve is forbidden for students")
    void studentCannotModerate() throws Exception {
        // Act + Assert
        mockMvc.perform(patch("/api/questions/7/approve"))
                .andExpect(status().isForbidden());

        verify(questionService, never()).approveQuestion(any(), any());
    }
}
