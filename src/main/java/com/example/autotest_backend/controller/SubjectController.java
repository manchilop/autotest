package com.example.autotest_backend.controller;

import com.example.autotest_backend.dto.subject.*;
import com.example.autotest_backend.model.Subject;
import com.example.autotest_backend.model.Topic;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.service.SubjectService;
import com.example.autotest_backend.service.TopicService;
import com.example.autotest_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;
    private final TopicService topicService;
    private final UserService userService;

    // ── TEACHER: create a subject ──────────────────────────────────────────
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse createSubject(
            @RequestBody @Valid CreateSubjectRequest request,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        Subject subject = subjectService.createSubject(request.getName(), user);
        return toSubjectResponse(subject);
    }

    // ── TEACHER + STUDENT: list my subjects ───────────────────────────────
    @GetMapping
    public List<SubjectResponse> getMySubjects(Authentication authentication) {
        User user = getUser(authentication);
        return subjectService.getSubjectsForUser(user)
                .stream()
                .map(this::toSubjectResponse)
                .collect(Collectors.toList());
    }

    // ── STUDENT: join a subject with invite code ───────────────────────────
    @PostMapping("/join")
    public SubjectResponse joinSubject(
            @RequestBody @Valid JoinSubjectRequest request,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        Subject subject = subjectService.joinByCode(request.getInviteCode(), user);
        return toSubjectResponse(subject);
    }

    // ── TEACHER: create a topic inside a subject ───────────────────────────
    @PostMapping("/{id}/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicResponse createTopic(
            @PathVariable Long id,
            @RequestBody @Valid CreateTopicRequest request,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        Topic topic = topicService.createTopic(id, request.getName(), user);
        return toTopicResponse(topic);
    }

    // ── TEACHER + STUDENT: list topics of a subject ───────────────────────
    @GetMapping("/{id}/topics")
    public List<TopicResponse> getTopics(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        return topicService.getTopicsForSubject(id, user)
                .stream()
                .map(this::toTopicResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private User getUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).orElseThrow();
    }

    private SubjectResponse toSubjectResponse(Subject subject) {
        List<TopicResponse> topics = subject.getTopics().stream()
                .map(this::toTopicResponse)
                .collect(Collectors.toList());

        return SubjectResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .inviteCode(subject.getInviteCode())
                .topics(topics)
                .build();
    }

    private TopicResponse toTopicResponse(Topic topic) {
        return TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .build();
    }
}