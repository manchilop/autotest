export type UserRole = "STUDENT" | "TEACHER";

export interface User {
  id: number;
  email: string;
  role: UserRole;
  name: string;
}