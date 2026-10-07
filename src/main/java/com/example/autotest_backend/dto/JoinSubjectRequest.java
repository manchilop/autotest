package com.example.autotest_backend.dto.subject;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class JoinSubjectRequest {

    @NotBlank
    private String inviteCode;
}
