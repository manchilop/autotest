package com.example.autotest_backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String role;
    private String name;
}