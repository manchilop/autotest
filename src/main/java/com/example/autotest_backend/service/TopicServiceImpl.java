package com.example.autotest_backend.service;

import com.example.autotest_backend.model.*;
import com.example.autotest_backend.repository.SubjectMembershipRepository;
import com.example.autotest_backend.repository.SubjectRepository;
import com.example.autotest_backend.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectMembershipRepository membershipRepository;

    @Override
    public Topic createTopic(Long subjectId, String name, User requester) {
        Subject subject = getSubjectOrThrow(subjectId);

        if (requester.getRole() != UserRole.TEACHER) {
            throw new IllegalStateException("Only teachers can create topics");
        }
        if (!membershipRepository.existsByUserAndSubject(requester, subject)) {
            throw new IllegalStateException("You are not a member of this subject");
        }

        return topicRepository.save(Topic.builder()
                .name(name)
                .subject(subject)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> getTopicsForSubject(Long subjectId, User requester) {
        Subject subject = getSubjectOrThrow(subjectId);

        if (!membershipRepository.existsByUserAndSubject(requester, subject)) {
            throw new IllegalStateException("You are not a member of this subject");
        }

        return topicRepository.findBySubjectId(subjectId);
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private Subject getSubjectOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
    }
}