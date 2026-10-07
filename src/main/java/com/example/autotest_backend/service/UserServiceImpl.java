package com.example.autotest_backend.service;

import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User registerUser(String email, String rawPassword, UserRole role, String name) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .name(name)
                .build();
        return userRepository.save(user);
    }
}
