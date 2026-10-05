import { User } from "./user";

export interface AuthState {
  token: string | null;
  // Absolute instant (ms since epoch) at which the token stops being accepted,
  // derived on login from the lifetime in seconds the server reports.
  expiresAt: number | null;
  user: User | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  expiresIn: number;
  user: User;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}