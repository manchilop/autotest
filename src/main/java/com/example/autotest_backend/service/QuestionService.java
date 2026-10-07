package com.example.autotest_backend.service;

import com.example.autotest_backend.model.Question;
import com.example.autotest_backend.model.QuestionStatus;
import com.example.autotest_backend.model.User;

import java.util.List;

public interface QuestionService {

    /*
     * Every operation below takes the requesting user because role alone is not
     * enough: the caller must also be related to the subject the question
     * belongs to. See the resource-level checks in QuestionServiceImpl.
     */

    Question createQuestion(Question question, User requester);

    List<Question> getAllQuestions(User requester);

    List<Question> getQuestionsByStatus(QuestionStatus status, User requester);

    Question approveQuestion(Long questionId, User requester);

    Question rejectQuestion(Long questionId, User requester);

    // subjectId is optional — null means "any subject the user belongs to"
    Question getNextQuestion(Long userId, Long subjectId);

    boolean answerQuestion(Long questionId, Long choiceId, User user);
}
