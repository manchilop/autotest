package com.example.autotest_backend.controller;

import com.example.autotest_backend.dto.question.*;
import com.example.autotest_backend.mapper.QuestionMapper;
import com.example.autotest_backend.model.Question;
import com.example.autotest_backend.model.QuestionStatus;
import com.example.autotest_backend.model.Topic;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.repository.TopicRepository;
import com.example.autotest_backend.service.QuestionService;
import com.example.autotest_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionMapper questionMapper;
    private final UserService userService;
    private final TopicRepository topicRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse createQuestion(
            @RequestBody @Valid CreateQuestionRequest request,
            Authentication authentication
    ) {
        User requester = getUser(authentication);
        Question question = questionMapper.toEntity(request);

        // Resolve topic if provided
        if (request.getTopicId() != null) {
            Topic topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new IllegalArgumentException("Topic not found"));
            question.setTopic(topic);
        }

        request.getChoices().forEach(choiceRequest ->
                question.addChoice(questionMapper.toEntity(choiceRequest))
        );

        Question saved = questionService.createQuestion(question, requester);
        return questionMapper.toResponse(saved);
    }

    @GetMapping
    public List<QuestionResponse> getQuestions(
            @RequestParam(required = false) QuestionStatus status,
            Authentication authentication
    ) {
        User requester = getUser(authentication);
        List<Question> questions = status != null
                ? questionService.getQuestionsByStatus(status, requester)
                : questionService.getAllQuestions(requester);

        return questions.stream()
                .map(questionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/next")
    public PracticeQuestionResponse getNextQuestion(
            @RequestParam(required = false) Long subjectId,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        return questionMapper.toPracticeResponse(questionService.getNextQuestion(user.getId(), subjectId));
    }

    @PostMapping("/{id}/answer")
    public AnswerResponse answerQuestion(
            @PathVariable Long id,
            @RequestBody @Valid AnswerRequest request,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        boolean correct = questionService.answerQuestion(id, request.getChoiceId(), user);
        return new AnswerResponse(correct);
    }

    @PatchMapping("/{id}/approve")
    public QuestionResponse approveQuestion(@PathVariable Long id, Authentication authentication) {
        return questionMapper.toResponse(questionService.approveQuestion(id, getUser(authentication)));
    }

    @PatchMapping("/{id}/reject")
    public QuestionResponse rejectQuestion(@PathVariable Long id, Authentication authentication) {
        return questionMapper.toResponse(questionService.rejectQuestion(id, getUser(authentication)));
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private User getUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).orElseThrow();
    }
}