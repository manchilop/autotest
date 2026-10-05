package com.example.autotest_backend.dto.question;

import com.example.autotest_backend.model.QuestionStatus;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PracticeQuestionResponse {
    Long id;
    String questionText;
    List<PracticeChoiceResponse> choices;
    // Lets the client warn the student when the question has not been reviewed yet
    QuestionStatus status;
}
