package com.example.autotest_backend.config;

import com.example.autotest_backend.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\": 401, \"message\": \"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\": 403, \"message\": \"Access denied\"}");
                }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()

                // ── STUDENT ────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,  "/api/questions/next").hasRole("STUDENT")
                .requestMatchers(HttpMethod.POST, "/api/questions/*/answer").hasRole("STUDENT")
                .requestMatchers(HttpMethod.POST, "/api/questions").hasRole("STUDENT")
                .requestMatchers(HttpMethod.GET,  "/api/user-questions/completed").hasRole("STUDENT")
                .requestMatchers(HttpMethod.POST, "/api/subjects/join").hasRole("STUDENT")

                // ── TEACHER ────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,  "/api/questions").hasRole("TEACHER")
                .requestMatchers(HttpMethod.GET,  "/api/questions/{id}").hasRole("TEACHER")
                .requestMatchers(HttpMethod.PATCH, "/api/questions/*/approve").hasRole("TEACHER")
                .requestMatchers(HttpMethod.PATCH, "/api/questions/*/reject").hasRole("TEACHER")
                .requestMatchers(HttpMethod.POST, "/api/subjects").hasRole("TEACHER")
                .requestMatchers(HttpMethod.POST, "/api/subjects/*/topics").hasRole("TEACHER")

                // ── BOTH ───────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/subjects").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/subjects/*/topics").authenticated()

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:8081"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}