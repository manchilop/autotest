package com.example.autotest_backend.controller;

import com.example.autotest_backend.config.SecurityConfig;
import com.example.autotest_backend.model.Subject;
import com.example.autotest_backend.model.Topic;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.security.JwtFilter;
import com.example.autotest_backend.security.JwtUtil;
import com.example.autotest_backend.service.SubjectService;
import com.example.autotest_backend.service.TopicService;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link SubjectController}.
 *
 * <p>Besides the request and response contract, these tests cover the role rules
 * declared in {@link SecurityConfig}: creating subjects and topics is reserved for
 * teachers, joining is reserved for students, and listing is open to both.
 */
@WebMvcTest(controllers = SubjectController.class)
@Import({SecurityConfig.class, JwtFilter.class})
@DisplayName("SubjectController")
class SubjectControllerTest {

    private static final String TEACHER_EMAIL = "teacher@example.com";
    private static final String STUDENT_EMAIL = "student@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubjectService subjectService;

    @MockitoBean
    private TopicService topicService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User teacher;
    private User student;
    private Subject subject;

    @BeforeEach
    void setUp() {
        teacher = User.builder()
                .id(1L).email(TEACHER_EMAIL).role(UserRole.TEACHER)
                .password("hashed").name("Teacher Tester").build();
        student = User.builder()
                .id(2L).email(STUDENT_EMAIL).role(UserRole.STUDENT)
                .password("hashed").name("Student Tester").build();
        subject = Subject.builder()
                .id(10L).name("Mathematics").inviteCode("ABC123").owner(teacher).build();
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL, roles = "TEACHER")
    @DisplayName("POST /api/subjects answers 201 with the created subject")
    void teacherCreatesSubject() throws Exception {
        // Arrange
        when(userService.getUserByEmail(TEACHER_EMAIL)).thenReturn(Optional.of(teacher));
        when(subjectService.createSubject("Mathematics", teacher)).thenReturn(subject);

        // Act + Assert
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mathematics"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Mathematics"))
                .andExpect(jsonPath("$.inviteCode").value("ABC123"))
                .andExpect(jsonPath("$.topics").isArray());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/subjects is forbidden for students")
    void studentCannotCreateSubject() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mathematics"}
                                """))
                .andExpect(status().isForbidden());

        verify(subjectService, never()).createSubject(any(), any());
    }

    @Test
    @DisplayName("POST /api/subjects is unauthorised without a token")
    void anonymousCannotCreateSubject() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mathematics"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL, roles = "TEACHER")
    @DisplayName("POST /api/subjects answers 400 when the name is blank")
    void rejectsBlankSubjectName() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  "}
                                """))
                .andExpect(status().isBadRequest());

        verify(subjectService, never()).createSubject(any(), any());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("GET /api/subjects lists the subjects of the caller")
    void listsSubjectsOfCaller() throws Exception {
        // Arrange
        Topic algebra = Topic.builder().id(20L).name("Algebra").subject(subject).build();
        Subject withTopics = Subject.builder()
                .id(10L).name("Mathematics").inviteCode("ABC123").owner(teacher)
                .topics(List.of(algebra)).build();
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(subjectService.getSubjectsForUser(student)).thenReturn(List.of(withTopics));

        // Act + Assert
        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Mathematics"))
                .andExpect(jsonPath("$[0].topics[0].name").value("Algebra"));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/subjects/join enrols the student with an invite code")
    void studentJoinsSubject() throws Exception {
        // Arrange
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(subjectService.joinByCode("ABC123", student)).thenReturn(subject);

        // Act + Assert
        mockMvc.perform(post("/api/subjects/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode":"ABC123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mathematics"));
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL, roles = "TEACHER")
    @DisplayName("POST /api/subjects/join is forbidden for teachers")
    void teacherCannotJoinSubject() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/subjects/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode":"ABC123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/subjects/join answers 409 when already enrolled")
    void joinAnswersConflictWhenAlreadyEnrolled() throws Exception {
        // Arrange
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(subjectService.joinByCode("ABC123", student))
                .thenThrow(new IllegalStateException("You are already a member of this subject"));

        // Act + Assert
        mockMvc.perform(post("/api/subjects/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode":"ABC123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("You are already a member of this subject"));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/subjects/join answers 400 for an unknown invite code")
    void joinAnswersBadRequestForUnknownCode() throws Exception {
        // Arrange
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(subjectService.joinByCode("NOPE12", student))
                .thenThrow(new IllegalArgumentException("Invalid invite code"));

        // Act + Assert
        mockMvc.perform(post("/api/subjects/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode":"NOPE12"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid invite code"));
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL, roles = "TEACHER")
    @DisplayName("POST /api/subjects/{id}/topics answers 201 with the created topic")
    void teacherCreatesTopic() throws Exception {
        // Arrange
        Topic algebra = Topic.builder().id(20L).name("Algebra").subject(subject).build();
        when(userService.getUserByEmail(TEACHER_EMAIL)).thenReturn(Optional.of(teacher));
        when(topicService.createTopic(eq(10L), eq("Algebra"), eq(teacher))).thenReturn(algebra);

        // Act + Assert
        mockMvc.perform(post("/api/subjects/10/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Algebra"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.name").value("Algebra"));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("POST /api/subjects/{id}/topics is forbidden for students")
    void studentCannotCreateTopic() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/subjects/10/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Algebra"}
                                """))
                .andExpect(status().isForbidden());

        verify(topicService, never()).createTopic(any(), any(), any());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("GET /api/subjects/{id}/topics lists the topics for a member")
    void listsTopicsForMember() throws Exception {
        // Arrange
        Topic algebra = Topic.builder().id(20L).name("Algebra").subject(subject).build();
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(topicService.getTopicsForSubject(10L, student)).thenReturn(List.of(algebra));

        // Act + Assert
        mockMvc.perform(get("/api/subjects/10/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].name").value("Algebra"));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
    @DisplayName("GET /api/subjects/{id}/topics answers 409 for a non-member")
    void topicsAnswerConflictForNonMember() throws Exception {
        // Arrange
        when(userService.getUserByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student));
        when(topicService.getTopicsForSubject(10L, student))
                .thenThrow(new IllegalStateException("You are not a member of this subject"));

        // Act + Assert
        mockMvc.perform(get("/api/subjects/10/topics"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You are not a member of this subject"));
    }
}
