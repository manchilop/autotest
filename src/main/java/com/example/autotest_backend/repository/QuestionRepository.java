package com.example.autotest_backend.repository;

import com.example.autotest_backend.model.Question;
import com.example.autotest_backend.model.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByStatus(QuestionStatus status);

    /* =========================================================
       Listings restricted to the subjects the user belongs to.
       A teacher must never see or moderate questions that belong
       to another teacher's subject.
       ========================================================= */

    @Query("""
    SELECT q FROM Question q
    WHERE q.status = :status
    AND q.topic IS NOT NULL
    AND q.topic.subject.id IN (
        SELECT sm.subject.id FROM SubjectMembership sm WHERE sm.user.id = :userId
    )
    """)
    List<Question> findByStatusForUser(@Param("status") QuestionStatus status,
                                       @Param("userId") Long userId);

    @Query("""
    SELECT q FROM Question q
    WHERE q.topic IS NOT NULL
    AND q.topic.subject.id IN (
        SELECT sm.subject.id FROM SubjectMembership sm WHERE sm.user.id = :userId
    )
    """)
    List<Question> findAllForUser(@Param("userId") Long userId);

    /* =========================================================
       Across ALL subjects the user belongs to
       ========================================================= */

    @Query("""
    SELECT q FROM Question q
    WHERE q.status = 'APPROVED'
    AND q.topic IS NOT NULL
    AND q.topic.subject.id IN (
        SELECT sm.subject.id FROM SubjectMembership sm WHERE sm.user.id = :userId
    )
    AND q.id NOT IN (
        SELECT uq.question.id FROM UserQuestion uq WHERE uq.user.id = :userId
    )
    ORDER BY RANDOM()
    LIMIT 1
    """)
    Optional<Question> findRandomUnansweredApprovedByUser(@Param("userId") Long userId);

    @Query("""
    SELECT q FROM Question q
    WHERE q.status = 'PENDING'
    AND q.topic IS NOT NULL
    AND q.topic.subject.id IN (
        SELECT sm.subject.id FROM SubjectMembership sm WHERE sm.user.id = :userId
    )
    AND q.id NOT IN (
        SELECT uq.question.id FROM UserQuestion uq WHERE uq.user.id = :userId
    )
    ORDER BY RANDOM()
    LIMIT 1
    """)
    Optional<Question> findRandomUnansweredPendingByUser(@Param("userId") Long userId);

    @Query("""
    SELECT q FROM Question q
    WHERE q.status = 'APPROVED'
    AND q.topic IS NOT NULL
    AND q.topic.subject.id IN (
        SELECT sm.subject.id FROM SubjectMembership sm WHERE sm.user.id = :userId
    )
    ORDER BY RANDOM()
    LIMIT 1
    """)
    Optional<Question> findRandomApprovedForUser(@Param("userId") Long userId);

    /* =========================================================
       Scoped to ONE specific subject (still validating membership)
       ========================================================= */

    @Query("""
    SELECT q FROM Question q
    WHERE q.status = 'APPROVED'
    AND q.topic IS NOT NULL
    AND q.topic.subject.id = :subjectId
    AND q.topic.subject.id IN (
        SELECT sm.subject.id FROM SubjectMembership sm WHERE sm.user.id = :userId
    )
    AND q.id NOT IN (
        SELECT uq.question.id FROM UserQuestion uq WHERE uq.user.id = :userId
    )
    ORDER BY RANDOM()
    LIMIT 1
    """)
    Optional<Question> findRandomUnansweredApprovedByUserAndSubject(
            @Param("userId") Long userId, @Param("subjectId") Long subjectId);

    @Query("""
    SELECT q FROM Question q
    WHERE q.status = 'PENDING'
    AND q.topic IS NOT NULL
    AND q.topic.subject.id = :subjectId
    AND q.topic.subject.id IN (
        SELECT sm.subject.id FROM SubjectMembership sm WHERE sm.user.id = :userId
    )
    AND q.id NOT IN (
        SELECT uq.question.id FROM UserQuestion uq WHERE uq.user.id = :userId
    )
    ORDER BY RANDOM()
    LIMIT 1
    """)
    Optional<Question> findRandomUnansweredPendingByUserAndSubject(
            @Param("userId") Long userId, @Param("subjectId") Long subjectId);

    @Query("""
    SELECT q FROM Question q
    WHERE q.status = 'APPROVED'
    AND q.topic IS NOT NULL
    AND q.topic.subject.id = :subjectId
    AND q.topic.subject.id IN (
        SELECT sm.subject.id FROM SubjectMembership sm WHERE sm.user.id = :userId
    )
    ORDER BY RANDOM()
    LIMIT 1
    """)
    Optional<Question> findRandomApprovedForUserAndSubject(
            @Param("userId") Long userId, @Param("subjectId") Long subjectId);
}