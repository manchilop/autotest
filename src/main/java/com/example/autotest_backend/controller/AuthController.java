package com.example.autotest_backend.controller;

import com.example.autotest_backend.dto.AuthRequest;
import com.example.autotest_backend.dto.RegisterRequest;
import com.example.autotest_backend.dto.AuthResponse;
import com.example.autotest_backend.dto.UserDTO;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.security.JwtUtil;
import com.example.autotest_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthRequest r) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(r.getEmail(), r.getPassword()));
            User u = userService.getUserByEmail(r.getEmail()).orElseThrow();
            String token = jwtUtil.generateToken(u.getEmail(), u.getRole().name());
            
            UserDTO userDto = UserDTO.builder()
                    .id(u.getId())
                    .email(u.getEmail())
                    .role(u.getRole().name())
                    .name(u.getName())
                    .build();

            return ResponseEntity.ok(new AuthResponse(token, jwtUtil.getExpirationSeconds(), userDto));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest r) {
        if (userService.existsByEmail(r.getEmail())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        userService.registerUser(r.getEmail(), r.getPassword(), UserRole.STUDENT, r.getName());
        return ResponseEntity.ok("created");
    }
}
