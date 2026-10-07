import api from "./api";
import { LoginRequest, LoginResponse, RegisterRequest } from "../types/auth";

// 🔐 LOGIN
export const login = async (
  data: LoginRequest
): Promise<LoginResponse> => {
  const response = await api.post<LoginResponse>(
    "/auth/login",
    data
  );

  return response.data;
};

// 📝 REGISTER
export const register = async (
  data: RegisterRequest
): Promise<string> => {
  const response = await api.post<string>(
    "/auth/register",
    data
  );

  return response.data;
};