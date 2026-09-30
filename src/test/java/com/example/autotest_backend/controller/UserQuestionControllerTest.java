package com.example.autotest_backend.controller;

import com.example.autotest_backend.config.SecurityConfig;
import com.example.autotest_backend.mapper.QuestionMapperImpl;
import com.example.autotest_backend.model.Choice;
import com.example.autotest_backend.model.Question;
import com.example.autotest_backend.model.QuestionStatus;
import com.example.autotest_backend.model.Subject;
import com.example.autotest_backend.model.Topic;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserQuestion;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.security.JwtFilter;
import com.example.autotest_backend.security.JwtUtil;
import com.example.autotest_backend.service.UserQuestionService;
import com.example.autotest_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link UserQuestionController}, the endpoint a student uses
 * to review the questions they have already completed.
 */
@WebMvcTest(controllers = UserQuestionController.class)
@Import({SecurityConfig.class, JwtFilter.class, QuestionMapperImpl.class})
@DisplayName("UserQuestionController")
class UserQuestionControllerTest {

    private static final String STUDENT_EMAIL = "student@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserQuestionService userQuestionService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User student;
    private Question question;

    @BeforeEach
    void setUp() {
        User teacher = User.builder()
                .id(1L).email("teacher@example.com").role(UserRole.TEACHER)
                .password("hashed").name("Teacher Tester").build();
        student = User.builder()
                .id(2L).email(STUDENT_EMAIL).role(UserRole.STUDENT)
                .password("hashed").name("Student Tester").build();

        Subject mathematics = Subject.builder()
                .id(10L).name("Mathematics").inviteCode("ABC123").owner(teacher).build();
        Topic algebra = Topic.builder().id(20L).name("Algebra").subject(mathematics).build();

        question = Question.builder()
                .id(7L).questionText("What is 2 + 2?")
                .status(QuestionStatus.APPROVED).topic(algebra).build();
        question.addChoice(Choice.builder().id(100L).choiceText("4").correct(true).build());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("GET /api/user-questions/completed lists the questions already answered")
    void listsCompletedQuestions() throws Exception {
        // Arrange
        UserQuestion completed = UserQuestion.builder()
                .id(1L).user(student).question(question).build();
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(userQuestionService.getCompletedQuestionsByUser(student))
                .thenReturn(List.of(completed));

        // Act + Assert
        mockMvc.perform(get("/api/user-questions/completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].questionText").value("What is 2 + 2?"))
                .andExpect(jsonPath("$[0].topicName").value("Algebra"))
                .andExpect(jsonPath("$[0].subjectName").value("Mathematics"));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("GET /api/user-questions/completed returns an empty list for a new student")
    void returnsEmptyListForNewStudent() throws Exception {
        // Arrange
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(userQuestionService.getCompletedQuestionsByUser(student)).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/api/user-questions/completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
    @DisplayName("GET /api/user-questions/completed is forbidden for teachers")
    void teacherCannotReadStudentHistory() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/user-questions/completed"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/user-questions/completed is unauthorised without a token")
    void anonymousCannotReadHistory() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/user-questions/completed"))
                .andExpect(status().isUnauthorized());
    }
}
