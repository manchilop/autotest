import axios from "axios";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { router } from "expo-router";
import { Platform } from "react-native";

const api = axios.create({
  // Ajuste para que funcione en emuladores de Android y iOS/Web
  baseURL: Platform.OS === 'android' ? "http://10.0.2.2:8080" : "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});

/*
 * Clearing the stored keys is not enough when the server rejects a token: the
 * session also lives in AuthContext's React state, and a component that only
 * reads that state would still believe the user is signed in. Because an axios
 * interceptor cannot use a React context, AuthProvider registers its own logout
 * here and the interceptor delegates to it, so state and storage are cleared
 * together.
 */
type UnauthorizedHandler = () => Promise<void> | void;

let onUnauthorized: UnauthorizedHandler | null = null;

export const setUnauthorizedHandler = (handler: UnauthorizedHandler | null) => {
  onUnauthorized = handler;
};

/** Keys holding the session. Kept here so both modules clear exactly the same set. */
export const SESSION_KEYS = ["token", "user", "expiresAt"];

// 🔐 REQUEST INTERCEPTOR → attach JWT
api.interceptors.request.use(
  async (config) => {
    const token = await AsyncStorage.getItem("token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 🚨 RESPONSE INTERCEPTOR → handle auth errors globally
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;

    if (status === 401) {
      // Clear the in-memory session too, not just the stored one
      if (onUnauthorized) {
        await onUnauthorized();
      } else {
        await AsyncStorage.multiRemove(SESSION_KEYS);
      }

      // redirect to login
      router.replace("/(auth)/login");
    }

    return Promise.reject(error);
  }
);

export default api;
