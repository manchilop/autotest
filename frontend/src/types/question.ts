export interface Choice {
  choiceText: string;
  correct: boolean;
}

export interface Question {
  id: number;
  questionText: string;
  choices: Choice[];
}

export interface CreateQuestionRequest {
  questionText: string;
  topicId?: number;
  choices: Choice[];
}

export interface ChoiceResponse {
  id: number;
  choiceText: string;
  correct: boolean;
}

export interface QuestionResponse {
  id: number;
  questionText: string;
  choices: ChoiceResponse[];
  topicId: number | null;
  topicName: string | null;
  subjectId: number | null;
  subjectName: string | null;
}

export type QuestionStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface PracticeQuestionResponse {
  id: number;
  questionText: string;
  choices: PracticeChoiceResponse[];
  status: QuestionStatus;
}

export interface PracticeChoiceResponse {
  id: number;
  choiceText: string;
}
