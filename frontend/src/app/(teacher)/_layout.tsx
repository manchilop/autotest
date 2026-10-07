import { Redirect, Stack, usePathname } from "expo-router";
import { useContext } from "react";
import { AuthContext } from "../../store/AuthContext";

export default function TeacherLayout() {
  const { authState, loading } = useContext(AuthContext);
  const pathname = usePathname();

  if (loading) return null;
  if (!authState.token || !authState.user) {
    return <Redirect href="/(auth)/login" />;
  }
  if (authState.user.role !== "TEACHER") {
    return <Redirect href={`/(student)/(tabs)${pathname}` as any} />;
  }

  return <Stack screenOptions={{ headerShown: false }} />;
}
