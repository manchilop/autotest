package com.example.autotest_backend.service;

import com.example.autotest_backend.model.Subject;
import com.example.autotest_backend.model.Topic;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.repository.SubjectMembershipRepository;
import com.example.autotest_backend.repository.SubjectRepository;
import com.example.autotest_backend.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TopicServiceImpl}.
 *
 * <p>Covers the two authorisation rules the service enforces: only teachers may
 * create topics, and only members of a subject may read or write its topics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TopicServiceImpl")
class TopicServiceImplTest {

    private static final Long SUBJECT_ID = 10L;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SubjectMembershipRepository membershipRepository;

    private TopicServiceImpl topicService;

    private User teacher;
    private User student;
    private Subject subject;

    @BeforeEach
    void setUp() {
        topicService = new TopicServiceImpl(topicRepository, subjectRepository, membershipRepository);

        teacher = User.builder()
                .id(1L).email("teacher@example.com").role(UserRole.TEACHER)
                .password("hashed").name("Teacher Tester").build();

        student = User.builder()
                .id(2L).email("student@example.com").role(UserRole.STUDENT)
                .password("hashed").name("Student Tester").build();

        subject = Subject.builder()
                .id(SUBJECT_ID).name("Mathematics").inviteCode("ABC123").owner(teacher).build();
    }

    @Nested
    @DisplayName("createTopic")
    class CreateTopic {

        @Test
        @DisplayName("saves the topic when a member teacher creates it")
        void savesTopicForMemberTeacher() {
            // Arrange
            when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
            when(membershipRepository.existsByUserAndSubject(teacher, subject)).thenReturn(true);
            when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Topic result = topicService.createTopic(SUBJECT_ID, "Algebra", teacher);

            // Assert
            assertThat(result.getName()).isEqualTo("Algebra");
            assertThat(result.getSubject()).isEqualTo(subject);

            ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
            verify(topicRepository).save(topicCaptor.capture());
            assertThat(topicCaptor.getValue().getSubject()).isEqualTo(subject);
        }

        @Test
        @DisplayName("rejects an unknown subject id with IllegalArgumentException")
        void rejectsUnknownSubject() {
            // Arrange
            when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> topicService.createTopic(SUBJECT_ID, "Algebra", teacher))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subject not found");

            verifyNoInteractions(topicRepository);
        }

        @Test
        @DisplayName("rejects a student even when they belong to the subject")
        void rejectsStudent() {
            // Arrange
            when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));

            // Act + Assert
            assertThatThrownBy(() -> topicService.createTopic(SUBJECT_ID, "Algebra", student))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only teachers can create topics");

            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a teacher who is not a member of the subject")
        void rejectsNonMemberTeacher() {
            // Arrange
            when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
            when(membershipRepository.existsByUserAndSubject(teacher, subject)).thenReturn(false);

            // Act + Assert
            assertThatThrownBy(() -> topicService.createTopic(SUBJECT_ID, "Algebra", teacher))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("You are not a member of this subject");

            verify(topicRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getTopicsForSubject")
    class GetTopicsForSubject {

        @Test
        @DisplayName("returns the topics of the subject for a member")
        void returnsTopicsForMember() {
            // Arrange
            Topic algebra = Topic.builder().id(1L).name("Algebra").subject(subject).build();
            when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
            when(membershipRepository.existsByUserAndSubject(student, subject)).thenReturn(true);
            when(topicRepository.findBySubjectId(SUBJECT_ID)).thenReturn(List.of(algebra));

            // Act
            List<Topic> result = topicService.getTopicsForSubject(SUBJECT_ID, student);

            // Assert
            assertThat(result).containsExactly(algebra);
        }

        @Test
        @DisplayName("rejects a user who is not a member of the subject")
        void rejectsNonMember() {
            // Arrange
            when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
            when(membershipRepository.existsByUserAndSubject(student, subject)).thenReturn(false);

            // Act + Assert
            assertThatThrownBy(() -> topicService.getTopicsForSubject(SUBJECT_ID, student))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("You are not a member of this subject");

            verify(topicRepository, never()).findBySubjectId(any());
        }

        @Test
        @DisplayName("rejects an unknown subject id with IllegalArgumentException")
        void rejectsUnknownSubject() {
            // Arrange
            when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> topicService.getTopicsForSubject(SUBJECT_ID, student))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subject not found");
        }
    }
}
