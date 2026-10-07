import { useState, useContext } from "react";
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  Pressable,
  ActivityIndicator,
} from "react-native";
import { login as loginService } from "../../services/auth.service";
import { AuthContext } from "../../store/AuthContext";
import { router } from "expo-router";

export default function LoginScreen() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const { login } = useContext(AuthContext);

  const handleLogin = async () => {
    setError("");
    if (!email.trim()) return setError("Email is required");
    if (!password.trim()) return setError("Password is required");

    try {
      setLoading(true);
      const response = await loginService({ email, password });
      await login(response);

      if (response.user.role === "STUDENT") {
        router.replace("/(student)/(tabs)/home");
      } else {
        router.replace("/(teacher)/(tabs)/home");
      }
    } catch (err) {
      setError("Login failed. Check your credentials.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.inner}>
        <Text style={styles.title}>AutoTest</Text>
        <Text style={styles.subtitle}>Welcome back</Text>

        <Text style={styles.label}>Email</Text>
        <TextInput
          placeholder="your@email.com"
          value={email}
          onChangeText={setEmail}
          style={styles.input}
          autoCapitalize="none"
          keyboardType="email-address"
        />

        <Text style={styles.label}>Password</Text>
        <TextInput
          placeholder="Your password"
          value={password}
          onChangeText={setPassword}
          style={styles.input}
          secureTextEntry
        />

        {error ? <Text style={styles.error}>{error}</Text> : null}

        <Pressable
          style={[styles.button, loading && styles.buttonDisabled]}
          onPress={handleLogin}
          disabled={loading}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.buttonText}>Log in</Text>
          )}
        </Pressable>

        <Text style={styles.link} onPress={() => router.replace("/(auth)/register")}>
          Don't have an account? Register
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#F8FAFC", justifyContent: "center", alignItems: "center", padding: 20 },
  inner: { width: "100%", maxWidth: 420, gap: 6 },
  title: { fontSize: 32, fontWeight: "700", color: "#111827", textAlign: "center", marginBottom: 4 },
  subtitle: { fontSize: 15, color: "#6B7280", textAlign: "center", marginBottom: 20 },
  label: { fontSize: 12, fontWeight: "600", color: "#6B7280", textTransform: "uppercase", letterSpacing: 1, marginTop: 8, marginBottom: 4 },
  input: { backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 10, padding: 12, fontSize: 15, color: "#111827" },
  error: { color: "#DC2626", textAlign: "center", fontSize: 13, marginTop: 4 },
  button: { backgroundColor: "#111827", borderRadius: 12, padding: 14, alignItems: "center", marginTop: 12 },
  buttonDisabled: { opacity: 0.6 },
  buttonText: { color: "#fff", fontSize: 15, fontWeight: "600" },
  link: { textAlign: "center", color: "#2563EB", marginTop: 12, fontSize: 14 },
});