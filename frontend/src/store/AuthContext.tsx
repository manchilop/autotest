import React, { createContext, useEffect, useState, ReactNode} from "react";

import AsyncStorage from "@react-native-async-storage/async-storage";
import { AuthState, LoginResponse } from "../types/auth";
import { setUnauthorizedHandler, SESSION_KEYS } from "../services/api";

interface AuthContextType {
  authState: AuthState;
  loading: boolean;
  login: (data: LoginResponse) => Promise<void>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType>(
  {} as AuthContextType
);

interface Props {
  children: ReactNode;
}

export const AuthProvider = ({ children }: Props) => {
  const [authState, setAuthState] = useState<AuthState>({
    token: null,
    user: null,
    expiresAt: null,
  });

  const [loading, setLoading] = useState(true);

  // 🔄 Load session on app start
  useEffect(() => {
    loadStorage();
  }, []);

  // A 401 means the server no longer accepts the token: clear the session here
  // as well, so no component keeps reading a stale one from React state.
  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, []);

  const loadStorage = async () => {
    try {
      const token = await AsyncStorage.getItem("token");
      const user = await AsyncStorage.getItem("user");
      const expiresAt = await AsyncStorage.getItem("expiresAt");

      if (!token || !user || !expiresAt) {
        return;
      }

      // The stored instant is when the token stops being accepted. Restoring an
      // already expired session would show the interface only for every request
      // to fail with 401, so it is discarded up front.
      if (Number(expiresAt) <= Date.now()) {
        await AsyncStorage.multiRemove(SESSION_KEYS);
        return;
      }

      setAuthState({
        token,
        user: JSON.parse(user),
        expiresAt: Number(expiresAt),
      });
    } catch (err) {
      console.log("Error loading auth state:", err);
    } finally {
      setLoading(false);
    }
  };

  // 🔐 LOGIN
  const login = async (data: LoginResponse) => {
    // The server reports the token's lifetime in seconds; what is useful to the
    // client is the resulting instant, which is what can actually be compared.
    const expiresAt = Date.now() + data.expiresIn * 1000;

    setAuthState({
      token: data.token,
      user: data.user,
      expiresAt,
    });

    await AsyncStorage.setItem("token", data.token);
    await AsyncStorage.setItem("user", JSON.stringify(data.user));
    await AsyncStorage.setItem("expiresAt", String(expiresAt));
  };

  // 🚪 LOGOUT
  const logout = async () => {
    setAuthState({
      token: null,
      user: null,
      expiresAt: null,
    });

    await AsyncStorage.multiRemove(SESSION_KEYS);
  };

  return (
    <AuthContext.Provider
      value={{
        authState,
        loading,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
