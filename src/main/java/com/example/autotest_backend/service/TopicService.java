package com.example.autotest_backend.service;

import com.example.autotest_backend.model.Topic;
import com.example.autotest_backend.model.User;

import java.util.List;

public interface TopicService {

    Topic createTopic(Long subjectId, String name, User requester);

    List<Topic> getTopicsForSubject(Long subjectId, User requester);
}