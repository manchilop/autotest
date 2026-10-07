export interface Topic {
  id: number;
  name: string;
}

export interface Subject {
  id: number;
  name: string;
  inviteCode: string;
  topics: Topic[];
}

export interface CreateSubjectRequest {
  name: string;
}

export interface CreateTopicRequest {
  name: string;
}

export interface JoinSubjectRequest {
  inviteCode: string;
}
