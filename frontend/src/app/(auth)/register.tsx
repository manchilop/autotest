import { useState } from "react";
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  Pressable,
  ActivityIndicator,
  ScrollView,
} from "react-native";
import { register as registerService } from "../../services/auth.service";
import { router } from "expo-router";

export default function RegisterScreen() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleRegister = async () => {
    setError("");

    if (!name.trim()) return setError("Name is required");
    if (!email.trim()) return setError("Email is required");
    if (!password.trim()) return setError("Password is required");
    if (password !== confirmPassword) return setError("Passwords do not match");
    if (password.length < 6) return setError("Password must be at least 6 characters");

    try {
      setLoading(true);
      await registerService({ name, email, password });
      router.replace("/(auth)/login");
    } catch (err) {
      setError("Registration failed. Try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>AutoTest</Text>
      <Text style={styles.subtitle}>Create your account</Text>

      <Text style={styles.label}>Name</Text>
      <TextInput
        placeholder="Your name"
        value={name}
        onChangeText={setName}
        style={styles.input}
        autoCapitalize="words"
      />

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
        placeholder="Min. 6 characters"
        value={password}
        onChangeText={setPassword}
        style={styles.input}
        secureTextEntry
      />

      <Text style={styles.label}>Confirm password</Text>
      <TextInput
        placeholder="Repeat your password"
        value={confirmPassword}
        onChangeText={setConfirmPassword}
        style={styles.input}
        secureTextEntry
      />

      {error ? <Text style={styles.error}>{error}</Text> : null}

      <Pressable
        style={[styles.button, loading && styles.buttonDisabled]}
        onPress={handleRegister}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Create account</Text>
        )}
      </Pressable>

      <Text style={styles.link} onPress={() => router.replace("/(auth)/login")}>
        Already have an account? Login
      </Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    justifyContent: "center",
    padding: 24,
    backgroundColor: "#F8FAFC",
    gap: 6,
  },
  title: {
    fontSize: 32,
    fontWeight: "700",
    textAlign: "center",
    color: "#111827",
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 15,
    color: "#6B7280",
    textAlign: "center",
    marginBottom: 20,
  },
  label: {
    fontSize: 12,
    fontWeight: "600",
    color: "#6B7280",
    textTransform: "uppercase",
    letterSpacing: 1,
    marginTop: 8,
    marginBottom: 4,
  },
  input: {
    backgroundColor: "#fff",
    borderWidth: 1,
    borderColor: "#E5E7EB",
    borderRadius: 10,
    padding: 12,
    fontSize: 15,
    color: "#111827",
  },
  error: {
    color: "#DC2626",
    textAlign: "center",
    fontSize: 13,
    marginTop: 4,
  },
  button: {
    backgroundColor: "#111827",
    borderRadius: 12,
    padding: 14,
    alignItems: "center",
    marginTop: 12,
  },
  buttonDisabled: { opacity: 0.6 },
  buttonText: { color: "#fff", fontSize: 15, fontWeight: "600" },
  link: {
    textAlign: "center",
    color: "#2563EB",
    marginTop: 12,
    fontSize: 14,
  },
});