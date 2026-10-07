package com.example.autotest_backend.service;

import com.example.autotest_backend.model.Subject;
import com.example.autotest_backend.model.User;

import java.util.List;

public interface SubjectService {

    Subject createSubject(String name, User owner);

    Subject joinByCode(String inviteCode, User user);

    List<Subject> getSubjectsForUser(User user);
}