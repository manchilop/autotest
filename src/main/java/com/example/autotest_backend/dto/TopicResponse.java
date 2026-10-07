package com.example.autotest_backend.dto.subject;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopicResponse {
    private Long id;
    private String name;
}
