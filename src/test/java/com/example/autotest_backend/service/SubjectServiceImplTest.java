package com.example.autotest_backend.service;

import com.example.autotest_backend.model.Subject;
import com.example.autotest_backend.model.SubjectMembership;
import com.example.autotest_backend.model.User;
import com.example.autotest_backend.model.UserRole;
import com.example.autotest_backend.repository.SubjectMembershipRepository;
import com.example.autotest_backend.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SubjectServiceImpl}.
 *
 * <p>Both repositories are mocked, so these tests touch no database and start no
 * Spring context. Only the business rules living in the service are exercised.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubjectServiceImpl")
class SubjectServiceImplTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SubjectMembershipRepository membershipRepository;

    private SubjectServiceImpl subjectService;

    private User teacher;
    private User student;

    @BeforeEach
    void setUp() {
        // Wired by hand instead of @InjectMocks to keep the dependencies explicit.
        subjectService = new SubjectServiceImpl(subjectRepository, membershipRepository);

        teacher = User.builder()
                .id(1L)
                .email("teacher@example.com")
                .role(UserRole.TEACHER)
                .password("hashed")
                .name("Teacher Tester")
                .build();

        student = User.builder()
                .id(2L)
                .email("student@example.com")
                .role(UserRole.STUDENT)
                .password("hashed")
                .name("Student Tester")
                .build();
    }

    @Nested
    @DisplayName("createSubject")
    class CreateSubject {

        @Test
        @DisplayName("creates the subject and auto-enrols the owning teacher")
        void createsSubjectAndEnrolsTeacher() {
            // Arrange
            when(subjectRepository.findByInviteCode(anyString())).thenReturn(Optional.empty());
            when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> {
                Subject toSave = invocation.getArgument(0);
                return Subject.builder()
                        .id(100L)
                        .name(toSave.getName())
                        .inviteCode(toSave.getInviteCode())
                        .owner(toSave.getOwner())
                        .build();
            });

            // Act
            Subject result = subjectService.createSubject("Mathematics", teacher);

            // Assert: the subject was persisted with the expected data
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getName()).isEqualTo("Mathematics");
            assertThat(result.getOwner()).isEqualTo(teacher);
            assertThat(result.getInviteCode()).hasSize(6);

            // Assert: the teacher was enrolled into the subject returned by save()
            ArgumentCaptor<SubjectMembership> membershipCaptor =
                    ArgumentCaptor.forClass(SubjectMembership.class);
            verify(membershipRepository).save(membershipCaptor.capture());
            assertThat(membershipCaptor.getValue().getUser()).isEqualTo(teacher);
            assertThat(membershipCaptor.getValue().getSubject()).isEqualTo(result);
        }

        @Test
        @DisplayName("rejects a non-teacher with IllegalStateException")
        void rejectsNonTeacher() {
            // Act + Assert
            assertThatThrownBy(() -> subjectService.createSubject("Mathematics", student))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only teachers can create subjects");

            // Nothing may be persisted when the role check fails
            verifyNoInteractions(subjectRepository);
            verifyNoInteractions(membershipRepository);
        }

        @Test
        @DisplayName("generates a new invite code when the first one collides")
        void retriesInviteCodeOnCollision() {
            // Arrange: first generated code is already taken, second one is free
            Subject existing = Subject.builder()
                    .id(5L).name("Other").inviteCode("DUPCOD").owner(teacher).build();
            when(subjectRepository.findByInviteCode(anyString()))
                    .thenReturn(Optional.of(existing))
                    .thenReturn(Optional.empty());
            when(subjectRepository.save(any(Subject.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            subjectService.createSubject("Physics", teacher);

            // Assert: the code was checked twice before being accepted as unique
            verify(subjectRepository, times(2)).findByInviteCode(anyString());
        }
    }

    @Nested
    @DisplayName("joinByCode")
    class JoinByCode {

        @Test
        @DisplayName("enrols the user when the code is valid and they are not a member yet")
        void enrolsUserWithValidCode() {
            // Arrange
            Subject subject = Subject.builder()
                    .id(10L).name("History").inviteCode("ABC123").owner(teacher).build();
            when(subjectRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(subject));
            when(membershipRepository.existsByUserAndSubject(student, subject)).thenReturn(false);

            // Act
            Subject result = subjectService.joinByCode("ABC123", student);

            // Assert
            assertThat(result).isEqualTo(subject);
            verify(membershipRepository).save(argThat(membership ->
                    membership.getUser().equals(student) && membership.getSubject().equals(subject)));
        }

        @Test
        @DisplayName("rejects an unknown invite code with IllegalArgumentException")
        void rejectsUnknownInviteCode() {
            // Arrange
            when(subjectRepository.findByInviteCode("BADCODE")).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> subjectService.joinByCode("BADCODE", student))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid invite code");

            verifyNoInteractions(membershipRepository);
        }

        @Test
        @DisplayName("rejects a user who already belongs to the subject")
        void rejectsAlreadyEnrolledUser() {
            // Arrange
            Subject subject = Subject.builder()
                    .id(10L).name("History").inviteCode("ABC123").owner(teacher).build();
            when(subjectRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(subject));
            when(membershipRepository.existsByUserAndSubject(student, subject)).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> subjectService.joinByCode("ABC123", student))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already a member");

            // A duplicate membership must never reach the database
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("translates a concurrent duplicate insert into IllegalStateException")
        void translatesDataIntegrityViolation() {
            // Arrange: two concurrent requests both pass the exists check,
            // but the unique constraint rejects the second insert.
            Subject subject = Subject.builder()
                    .id(10L).name("History").inviteCode("ABC123").owner(teacher).build();
            when(subjectRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(subject));
            when(membershipRepository.existsByUserAndSubject(student, subject)).thenReturn(false);
            when(membershipRepository.save(any(SubjectMembership.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));

            // Act + Assert
            assertThatThrownBy(() -> subjectService.joinByCode("ABC123", student))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already a member");
        }
    }

    @Nested
    @DisplayName("getSubjectsForUser")
    class GetSubjectsForUser {

        @Test
        @DisplayName("returns the subjects reported by the membership repository")
        void returnsSubjectsOfUser() {
            // Arrange
            Subject subject = Subject.builder()
                    .id(10L).name("History").inviteCode("ABC123").owner(teacher).build();
            when(membershipRepository.findSubjectsByUserId(teacher.getId()))
                    .thenReturn(List.of(subject));

            // Act
            List<Subject> result = subjectService.getSubjectsForUser(teacher);

            // Assert
            assertThat(result).containsExactly(subject);
            verify(membershipRepository).findSubjectsByUserId(teacher.getId());
        }
    }
}
