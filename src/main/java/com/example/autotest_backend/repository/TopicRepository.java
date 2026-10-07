package com.example.autotest_backend.repository;

import com.example.autotest_backend.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findBySubjectId(Long subjectId);
}