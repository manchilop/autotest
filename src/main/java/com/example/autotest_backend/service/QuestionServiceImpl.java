package com.example.autotest_backend.service;

import com.example.autotest_backend.model.Choice;
import com.example.autotest_backend.model.Question;
import com.example.autotest_backend.model.QuestionStatus;
import com.example.autotest_backend.model.Subject;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.repository.QuestionRepository;
import com.example.autotest_backend.repository.SubjectMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final SubjectMembershipRepository membershipRepository;
    private final UserQuestionServiceImpl userQuestionServiceImpl;

    @Override
    public Question createQuestion(Question question, User requester) {
        requireMembership(requester, subjectOf(question));
        requireExactlyOneCorrectChoice(question);
        return questionRepository.save(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> getAllQuestions(User requester) {
        return questionRepository.findAllForUser(requester.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> getQuestionsByStatus(QuestionStatus status, User requester) {
        return questionRepository.findByStatusForUser(status, requester.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Question getNextQuestion(Long userId, Long subjectId) {
        if (subjectId != null) {
            return getNextQuestionForSubject(userId, subjectId);
        }
        return getNextQuestionAcrossSubjects(userId);
    }

    private Question getNextQuestionAcrossSubjects(Long userId) {
        Optional<Question> approved = questionRepository.findRandomUnansweredApprovedByUser(userId);
        if (approved.isPresent()) return approved.get();

        Optional<Question> pending = questionRepository.findRandomUnansweredPendingByUser(userId);
        if (pending.isPresent()) return pending.get();

        return questionRepository.findRandomApprovedForUser(userId)
                .orElseThrow(() -> new IllegalStateException("No questions available"));
    }

    private Question getNextQuestionForSubject(Long userId, Long subjectId) {
        Optional<Question> approved =
                questionRepository.findRandomUnansweredApprovedByUserAndSubject(userId, subjectId);
        if (approved.isPresent()) return approved.get();

        Optional<Question> pending =
                questionRepository.findRandomUnansweredPendingByUserAndSubject(userId, subjectId);
        if (pending.isPresent()) return pending.get();

        return questionRepository.findRandomApprovedForUserAndSubject(userId, subjectId)
                .orElseThrow(() -> new IllegalStateException("No questions available"));
    }

    @Override
    public boolean answerQuestion(Long questionId, Long choiceId, User user) {
        Question question = getQuestionOrThrow(questionId);
        requireMembership(user, subjectOf(question));

        boolean correct = question.getChoices().stream()
                .filter(c -> c.getId().equals(choiceId))
                .findFirst()
                .map(Choice::isCorrect)
                .orElseThrow(() -> new IllegalArgumentException("Choice not found"));

        if (!userQuestionServiceImpl.hasUserCompletedQuestion(user, question)) {
            userQuestionServiceImpl.markAsCompleted(user, question);
        }

        return correct;
    }

    @Override
    public Question approveQuestion(Long questionId, User requester) {
        Question question = getQuestionOrThrow(questionId);
        requireOwnership(requester, subjectOf(question));
        question.setStatus(QuestionStatus.APPROVED);
        return question;
    }

    @Override
    public Question rejectQuestion(Long questionId, User requester) {
        Question question = getQuestionOrThrow(questionId);
        requireOwnership(requester, subjectOf(question));
        question.setStatus(QuestionStatus.REJECTED);
        return question;
    }

    // ──────────────────────────────────────────────
    // Resource-level authorisation
    // ──────────────────────────────────────────────

    /**
     * The subject a question belongs to, reached through its topic. A question
     * without a topic is not attached to any subject and therefore cannot be
     * authorised against one.
     */
    private Subject subjectOf(Question question) {
        if (question.getTopic() == null) {
            throw new IllegalArgumentException("Question is not attached to any subject");
        }
        return question.getTopic().getSubject();
    }

    /**
     * A multiple-choice question is only answerable if exactly one of its choices
     * is the right one. With none, nobody could ever answer it correctly; with
     * several, any of them would be reported as correct. Neither case can be
     * expressed with bean validation on a single choice, because the rule is about
     * the collection as a whole, so it is enforced here.
     */
    private void requireExactlyOneCorrectChoice(Question question) {
        long correct = question.getChoices().stream().filter(Choice::isCorrect).count();
        if (correct != 1) {
            throw new IllegalArgumentException(
                    "A question must have exactly one correct choice, but " + correct + " were marked");
        }
    }

    /** The requester must belong to the subject, whatever their role. */
    private void requireMembership(User requester, Subject subject) {
        if (!membershipRepository.existsByUserAndSubject(requester, subject)) {
            throw new IllegalStateException("You are not a member of this subject");
        }
    }

    /** Moderating a question is reserved to the teacher who owns its subject. */
    private void requireOwnership(User requester, Subject subject) {
        if (subject.getOwner() == null || !subject.getOwner().getId().equals(requester.getId())) {
            throw new IllegalStateException("You do not own this subject");
        }
    }

    private Question getQuestionOrThrow(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
    }
}
