package com.example.autotest_backend.mapper;

import com.example.autotest_backend.dto.question.*;
import com.example.autotest_backend.model.Choice;
import com.example.autotest_backend.model.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    /* =========================
       CREATE (DTO -> ENTITY)
       ========================= */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "choices", ignore = true)
    Question toEntity(CreateQuestionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "question", ignore = true)
    Choice toEntity(CreateChoiceRequest request);

    List<Choice> toChoiceEntities(List<CreateChoiceRequest> requests);

    /* =========================
       READ (ENTITY -> DTO)
       ========================= */

    @Mapping(target = "topicId", source = "topic.id")
    @Mapping(target = "topicName", source = "topic.name")
    @Mapping(target = "subjectId", source = "topic.subject.id")
    @Mapping(target = "subjectName", source = "topic.subject.name")
    QuestionResponse toResponse(Question question);

    @Mapping(target = "correct", source = "correct")
    ChoiceResponse toResponse(Choice choice);

    List<ChoiceResponse> toChoiceResponses(List<Choice> choices);

    PracticeQuestionResponse toPracticeResponse(Question question);

    PracticeChoiceResponse toPracticeChoiceResponse(Choice choice);
}
