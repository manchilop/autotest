package com.example.autotest_backend.service;

import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomUserDetailsService}.
 *
 * <p>The mapping from our own {@code UserRole} to a Spring Security authority is
 * what the whole authorisation layer depends on, so it is pinned down here: the
 * authority must carry the {@code ROLE_} prefix that {@code hasRole(...)} expects.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    @DisplayName("maps a stored student to user details with ROLE_STUDENT")
    void mapsStudentToUserDetails() {
        // Arrange
        User student = User.builder()
                .id(1L).email("student@example.com").role(UserRole.STUDENT)
                .password("hashed-password").name("Student Tester").build();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));

        // Act
        UserDetails details = userDetailsService.loadUserByUsername("student@example.com");

        // Assert
        assertThat(details.getUsername()).isEqualTo("student@example.com");
        assertThat(details.getPassword()).isEqualTo("hashed-password");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    @DisplayName("maps a stored teacher to user details with ROLE_TEACHER")
    void mapsTeacherToUserDetails() {
        // Arrange
        User teacher = User.builder()
                .id(2L).email("teacher@example.com").role(UserRole.TEACHER)
                .password("hashed-password").name("Teacher Tester").build();
        when(userRepository.findByEmail("teacher@example.com")).thenReturn(Optional.of(teacher));

        // Act
        UserDetails details = userDetailsService.loadUserByUsername("teacher@example.com");

        // Assert
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TEACHER");
    }

    @Test
    @DisplayName("throws UsernameNotFoundException for an unknown email")
    void throwsForUnknownEmail() {
        // Arrange
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
