import api from "./api";
import { CreateQuestionRequest, QuestionResponse } from "../types/question";

export const createQuestion = async (
  data: CreateQuestionRequest
): Promise<QuestionResponse> => {
  const response = await api.post<QuestionResponse>(
    "/api/questions",
    data
  );

  return response.data;
};

export const getQuestionsByStatus = async (status: string) => {
  const response = await api.get(`/api/questions?status=${status}`);
  return response.data;
};

export const getNextQuestion = async (subjectId?: number) => {
  const query = subjectId ? `?subjectId=${subjectId}` : "";
  const response = await api.get(`/api/questions/next${query}`);
  return response.data;
};

export const answerQuestion = async (
  questionId: number,
  choiceId: number
) => {
  const response = await api.post(
    `/api/questions/${questionId}/answer`,
    { choiceId }
  );
  return response.data;
};

export const getCompletedQuestions = async () => {
  const response = await api.get("/api/user-questions/completed");
  return response.data;
};

// Teacher

export const approveQuestion = async (id: number) => {
  const response = await api.patch(
    `/api/questions/${id}/approve`
  );
  return response.data;
};

export const rejectQuestion = async (id: number) => {
  const response = await api.patch(
    `/api/questions/${id}/reject`
  );
  return response.data;
};
