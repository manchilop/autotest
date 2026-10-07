package com.example.autotest_backend.service;

import com.example.autotest_backend.model.*;
import com.example.autotest_backend.repository.SubjectMembershipRepository;
import com.example.autotest_backend.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectMembershipRepository membershipRepository;

    @Override
    public Subject createSubject(String name, User owner) {
        if (owner.getRole() != UserRole.TEACHER) {
            throw new IllegalStateException("Only teachers can create subjects");
        }

        String inviteCode = generateUniqueInviteCode();

        Subject subject = Subject.builder()
                .name(name)
                .inviteCode(inviteCode)
                .owner(owner)
                .build();

        subject = subjectRepository.save(subject);

        // Auto-enroll the teacher so GET /api/subjects works uniformly for all roles
        membershipRepository.save(SubjectMembership.builder()
                .user(owner)
                .subject(subject)
                .build());

        return subject;
    }

    @Override
    public Subject joinByCode(String inviteCode, User user) {
        Subject subject = subjectRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));

        if (membershipRepository.existsByUserAndSubject(user, subject)) {
            throw new IllegalStateException("You are already a member of this subject");
        }

        try {
            membershipRepository.save(SubjectMembership.builder()
                    .user(user)
                    .subject(subject)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("You are already a member of this subject");
        }

        return subject;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsForUser(User user) {
        return membershipRepository.findSubjectsByUserId(user.getId());
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();
        } while (subjectRepository.findByInviteCode(code).isPresent());
        return code;
    }
}