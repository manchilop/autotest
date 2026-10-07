package com.example.autotest_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subject_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "subject_id"})
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
}