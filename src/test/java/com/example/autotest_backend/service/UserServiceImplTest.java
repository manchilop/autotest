package com.example.autotest_backend.service;

import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserServiceImpl}.
 *
 * <p>The only real logic here is registration, where the raw password must be
 * hashed before it ever reaches the repository.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("registerUser hashes the password and never stores the raw one")
    void registerUserHashesPassword() {
        // Arrange
        when(passwordEncoder.encode("PlainPassword123!")).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        User result = userService.registerUser(
                "new@example.com", "PlainPassword123!", UserRole.STUDENT, "New Student");

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getPassword()).isEqualTo("hashed-value");
        assertThat(saved.getPassword()).isNotEqualTo("PlainPassword123!");
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(saved.getName()).isEqualTo("New Student");
        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("registerUser keeps the requested role, including TEACHER")
    void registerUserKeepsRequestedRole() {
        // Arrange
        when(passwordEncoder.encode(any())).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        User result = userService.registerUser(
                "teacher@example.com", "PlainPassword123!", UserRole.TEACHER, "New Teacher");

        // Assert
        assertThat(result.getRole()).isEqualTo(UserRole.TEACHER);
    }

    @Test
    @DisplayName("getUserByEmail delegates to the repository")
    void getUserByEmailDelegates() {
        // Arrange
        User user = User.builder()
                .id(1L).email("found@example.com").role(UserRole.STUDENT)
                .password("hashed").name("Found User").build();
        when(userRepository.findByEmail("found@example.com")).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.getUserByEmail("found@example.com");

        // Assert
        assertThat(result).contains(user);
    }

    @Test
    @DisplayName("getUserByEmail returns empty when the repository finds nothing")
    void getUserByEmailReturnsEmpty() {
        // Arrange
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.getUserByEmail("missing@example.com");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserById delegates to the repository")
    void getUserByIdDelegates() {
        // Arrange
        User user = User.builder()
                .id(7L).email("seven@example.com").role(UserRole.STUDENT)
                .password("hashed").name("Seven").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.getUserById(7L);

        // Assert
        assertThat(result).contains(user);
    }

    @Test
    @DisplayName("existsByEmail delegates to the repository")
    void existsByEmailDelegates() {
        // Arrange
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        // Act + Assert
        assertThat(userService.existsByEmail("taken@example.com")).isTrue();
    }
}
