package com.example.autotest_backend.repository;

import com.example.autotest_backend.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByInviteCode(String inviteCode);
}