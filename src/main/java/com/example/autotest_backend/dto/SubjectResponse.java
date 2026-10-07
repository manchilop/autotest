package com.example.autotest_backend.dto.subject;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SubjectResponse {
    private Long id;
    private String name;
    private String inviteCode;
    private List<TopicResponse> topics;
}
