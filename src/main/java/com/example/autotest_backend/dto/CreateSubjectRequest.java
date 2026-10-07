package com.example.autotest_backend.dto.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateSubjectRequest {

    @NotBlank
    @Size(max = 100, message = "Subject name must not exceed 100 characters")
    private String name;
}