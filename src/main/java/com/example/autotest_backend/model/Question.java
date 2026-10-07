package com.example.autotest_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, length = 500)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QuestionStatus status = QuestionStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = true)
    private Topic topic;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private List<Choice> choices = new ArrayList<>();

    public List<Choice> getChoices() {
        return Collections.unmodifiableList(choices);
    }

    public void addChoice(Choice c) {
        choices.add(c);
        c.setQuestion(this);
    }

    public void removeChoice(Choice c) {
        choices.remove(c);
        c.setQuestion(null);
    }

}
