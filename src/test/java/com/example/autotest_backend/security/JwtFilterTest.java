package com.example.autotest_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtFilter}.
 *
 * <p>The filter is the single place where a bearer token becomes an authenticated
 * request, so each test asserts two things: what ends up in the security context,
 * and that the request is always passed further down the chain.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter")
class JwtFilterTest {

    private static final String TOKEN = "a.valid.token";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private Claims claims;

    private JwtFilter jwtFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static UserDetails userDetails() {
        return new User("teacher@example.com", "hashed", List.of());
    }

    @Test
    @DisplayName("authenticates the request carrying a valid bearer token")
    void authenticatesValidToken() throws Exception {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtUtil.validateToken(TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("teacher@example.com");
        when(claims.get("role", String.class)).thenReturn("TEACHER");
        when(userDetailsService.loadUserByUsername("teacher@example.com")).thenReturn(userDetails());

        // Act
        jwtFilter.doFilter(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TEACHER");
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("leaves the request anonymous when no Authorization header is present")
    void ignoresMissingHeader() throws Exception {
        // Act
        jwtFilter.doFilter(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("ignores an Authorization header that is not a bearer token")
    void ignoresNonBearerHeader() throws Exception {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");

        // Act
        jwtFilter.doFilter(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("clears the context and continues the chain for an invalid token")
    void clearsContextForInvalidToken() throws Exception {
        // Arrange
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtUtil.validateToken(TOKEN)).thenThrow(new JwtException("expired"));

        // Act
        jwtFilter.doFilter(request, response, filterChain);

        // Assert: an invalid token must never authenticate, and must not abort the request
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("keeps an authentication that a previous filter already established")
    void keepsExistingAuthentication() throws Exception {
        // Arrange
        Authentication existing = new UsernamePasswordAuthenticationToken(
                "already@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);

        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtUtil.validateToken(TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("teacher@example.com");
        when(claims.get("role", String.class)).thenReturn("TEACHER");

        // Act
        jwtFilter.doFilter(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verifyNoInteractions(userDetailsService);
    }
}
