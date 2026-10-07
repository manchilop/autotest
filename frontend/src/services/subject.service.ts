import api from "./api";
import {
  Subject,
  Topic,
  CreateSubjectRequest,
  CreateTopicRequest,
  JoinSubjectRequest,
} from "../types/subject";

// Teacher: create a subject (auto-enrolls the teacher)
export const createSubject = async (
  data: CreateSubjectRequest
): Promise<Subject> => {
  const response = await api.post<Subject>("/api/subjects", data);
  return response.data;
};

// Both roles: list subjects the current user belongs to
export const getMySubjects = async (): Promise<Subject[]> => {
  const response = await api.get<Subject[]>("/api/subjects");
  return response.data;
};

// Student: join a subject using an invite code
export const joinSubject = async (
  data: JoinSubjectRequest
): Promise<Subject> => {
  const response = await api.post<Subject>("/api/subjects/join", data);
  return response.data;
};

// Teacher: create a topic inside a subject
export const createTopic = async (
  subjectId: number,
  data: CreateTopicRequest
): Promise<Topic> => {
  const response = await api.post<Topic>(
    `/api/subjects/${subjectId}/topics`,
    data
  );
  return response.data;
};

// Both roles: list topics of a subject
export const getTopicsBySubject = async (
  subjectId: number
): Promise<Topic[]> => {
  const response = await api.get<Topic[]>(
    `/api/subjects/${subjectId}/topics`
  );
  return response.data;
};
