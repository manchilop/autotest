package com.example.autotest_backend.controller;

import com.example.autotest_backend.config.SecurityConfig;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.security.JwtFilter;
import com.example.autotest_backend.security.JwtUtil;
import com.example.autotest_backend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link AuthController}.
 *
 * <p>Only the web slice is started: the services below the controller are mocked,
 * while the real security configuration and the real JSON serialisation run, so
 * the HTTP contract of the login and registration endpoints is what gets tested.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtFilter.class})
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserService userService;

    // Required by JwtFilter, which sits in the security chain of every request.
    @MockitoBean
    private UserDetailsService userDetailsService;

    private static User teacher() {
        return User.builder()
                .id(1L).email("teacher@example.com").role(UserRole.TEACHER)
                .password("hashed").name("Teacher Tester").build();
    }

    @Test
    @DisplayName("POST /auth/login returns a token and the user profile")
    void loginReturnsToken() throws Exception {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("teacher@example.com", "pw"));
        when(userService.getUserByEmail("teacher@example.com")).thenReturn(Optional.of(teacher()));
        when(jwtUtil.generateToken("teacher@example.com", "TEACHER")).thenReturn("signed-jwt");
        when(jwtUtil.getExpirationSeconds()).thenReturn(3600L);

        // Act + Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"teacher@example.com","password":"Password123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed-jwt"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.email").value("teacher@example.com"))
                .andExpect(jsonPath("$.user.role").value("TEACHER"))
                .andExpect(jsonPath("$.user.name").value("Teacher Tester"));
    }

    @Test
    @DisplayName("POST /auth/login never returns the password hash")
    void loginDoesNotLeakPassword() throws Exception {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("teacher@example.com", "pw"));
        when(userService.getUserByEmail("teacher@example.com")).thenReturn(Optional.of(teacher()));
        when(jwtUtil.generateToken(any(), any())).thenReturn("signed-jwt");

        // Act + Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"teacher@example.com","password":"Password123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("hashed"))));
    }

    @Test
    @DisplayName("POST /auth/login answers 401 for wrong credentials")
    void loginRejectsWrongCredentials() throws Exception {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        // Act + Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"teacher@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login answers 400 when the body fails validation")
    void loginRejectsInvalidBody() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /auth/register creates a student account")
    void registerCreatesStudent() throws Exception {
        // Arrange
        when(userService.existsByEmail("new@example.com")).thenReturn(false);

        // Act
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","password":"Password123!","name":"New Student"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("created"));

        // Assert: registration through this endpoint may only ever create students
        verify(userService).registerUser(
                eq("new@example.com"), eq("Password123!"), eq(UserRole.STUDENT), eq("New Student"));
    }

    @Test
    @DisplayName("POST /auth/register answers 400 when the email is taken")
    void registerRejectsDuplicateEmail() throws Exception {
        // Arrange
        when(userService.existsByEmail("taken@example.com")).thenReturn(true);

        // Act + Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"taken@example.com","password":"Password123!","name":"Someone"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already exists"));

        verify(userService, never()).registerUser(any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /auth/register answers 400 when required fields are missing")
    void registerRejectsInvalidBody() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","password":"Password123!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));

        verify(userService, never()).registerUser(any(), any(), any(), any());
    }
}
