package com.example.autotest_backend.config;

import com.example.autotest_backend.model.*;
import com.example.autotest_backend.repository.UserRepository;
import com.example.autotest_backend.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates the database with a default set of users, subjects, topics and questions
 * so a freshly created (empty) database is immediately usable for manual testing/demos.
 * Skips entirely if any user already exists, so it is safe to leave enabled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final UserService userService;
    private final SubjectService subjectService;
    private final TopicService topicService;
    private final QuestionService questionService;
    private final UserQuestionService userQuestionService;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains users, skipping default data seeding");
            return;
        }

        log.info("Seeding database with default data...");

        User teacher = userService.registerUser("teacher@autotest.dev", DEFAULT_PASSWORD, UserRole.TEACHER, "Alex Teacher");
        User student1 = userService.registerUser("student1@autotest.dev", DEFAULT_PASSWORD, UserRole.STUDENT, "Sam Student");
        User student2 = userService.registerUser("student2@autotest.dev", DEFAULT_PASSWORD, UserRole.STUDENT, "Jamie Student");

        Subject math = subjectService.createSubject("Mathematics", teacher);
        Subject science = subjectService.createSubject("Science", teacher);

        subjectService.joinByCode(math.getInviteCode(), student1);
        subjectService.joinByCode(math.getInviteCode(), student2);
        subjectService.joinByCode(science.getInviteCode(), student1);
        subjectService.joinByCode(science.getInviteCode(), student2);

        Topic algebra = topicService.createTopic(math.getId(), "Algebra", teacher);
        Topic geometry = topicService.createTopic(math.getId(), "Geometry", teacher);
        Topic physics = topicService.createTopic(science.getId(), "Physics", teacher);
        Topic chemistry = topicService.createTopic(science.getId(), "Chemistry", teacher);

        Question q1 = question("What is the value of x in 2x + 3 = 7?", algebra, QuestionStatus.APPROVED,
                choice("1", false), choice("2", true), choice("3", false), choice("4", false));
        Question q2 = question("Simplify: 3(x + 2)", algebra, QuestionStatus.APPROVED,
                choice("3x + 2", false), choice("3x + 6", true), choice("x + 6", false), choice("3x + 5", false));
        Question q3 = question("How many degrees do the interior angles of a triangle sum to?", geometry, QuestionStatus.APPROVED,
                choice("90", false), choice("180", true), choice("270", false), choice("360", false));
        Question q4 = question("What shape has 4 equal sides and 4 right angles?", geometry, QuestionStatus.PENDING,
                choice("Rectangle", false), choice("Square", true), choice("Rhombus", false), choice("Trapezoid", false));
        Question q5 = question("What is the SI unit of force?", physics, QuestionStatus.APPROVED,
                choice("Joule", false), choice("Newton", true), choice("Watt", false), choice("Pascal", false));
        Question q6 = question("What is the approximate speed of light, in km/s?", physics, QuestionStatus.APPROVED,
                choice("150,000", false), choice("300,000", true), choice("3,000", false), choice("30,000", false));
        Question q7 = question("What is the chemical formula for water?", chemistry, QuestionStatus.APPROVED,
                choice("O2", false), choice("H2O", true), choice("CO2", false), choice("NaCl", false));
        Question q8 = question("What is the atomic number of Hydrogen?", chemistry, QuestionStatus.PENDING,
                choice("0", false), choice("1", true), choice("2", false), choice("8", false));

        // The teacher is auto-enrolled in their own subjects, so they satisfy the
        // membership check that guards question creation.
        for (Question q : new Question[]{q1, q2, q3, q4, q5, q6, q7, q8}) {
            questionService.createQuestion(q, teacher);
        }

        userQuestionService.markAsCompleted(student1, q1);
        userQuestionService.markAsCompleted(student1, q3);

        log.info("Default data seeded: 1 teacher, 2 students, 2 subjects, 4 topics, 8 questions");
        log.info("Default login password for all seeded users: {}", DEFAULT_PASSWORD);
    }

    private static Question question(String text, Topic topic, QuestionStatus status, Choice... choices) {
        Question question = Question.builder()
                .questionText(text)
                .topic(topic)
                .status(status)
                .build();
        for (Choice c : choices) {
            question.addChoice(c);
        }
        return question;
    }

    private static Choice choice(String text, boolean correct) {
        return Choice.builder()
                .choiceText(text)
                .correct(correct)
                .build();
    }
}
