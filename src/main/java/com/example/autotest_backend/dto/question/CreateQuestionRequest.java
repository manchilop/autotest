package com.example.autotest_backend.dto.question;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class CreateQuestionRequest {

    @NotBlank
    @Size(max = 500, message = "Question text must not exceed 500 characters")
    String questionText;

    // @Valid is what makes the constraints of each choice run; without it the
    // nested objects are accepted unchecked.
    @NotNull(message = "A question must include its choices")
    @Size(min = 2, message = "A question must have at least two choices")
    @Valid
    List<CreateChoiceRequest> choices;

    private Long topicId;
}
