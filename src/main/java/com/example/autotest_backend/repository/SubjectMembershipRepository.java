package com.example.autotest_backend.repository;

import com.example.autotest_backend.model.Subject;
import com.example.autotest_backend.model.SubjectMembership;
import com.example.autotest_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubjectMembershipRepository extends JpaRepository<SubjectMembership, Long> {

    boolean existsByUserAndSubject(User user, Subject subject);

    // Returns all subjects a user belongs to (works for both TEACHER and STUDENT)
    @Query("SELECT sm.subject FROM SubjectMembership sm WHERE sm.user.id = :userId")
    List<Subject> findSubjectsByUserId(@Param("userId") Long userId);
}