import { View, Text, StyleSheet, Pressable } from "react-native";
import { useContext } from "react";
import { AuthContext } from "../../../store/AuthContext";
import { router } from "expo-router";

export default function Profile() {
  const { authState, logout } = useContext(AuthContext);
  const user = authState.user;
  const initial = user?.name?.charAt(0).toUpperCase() || "?";

  const handleLogout = async () => {
    await logout();
    router.replace("/(auth)/login");
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.pageTitle}>Profile</Text>
      </View>

      <View style={styles.avatar}>
        <Text style={styles.avatarText}>{initial}</Text>
      </View>

      <Text style={styles.name}>{user?.name}</Text>
      <View style={styles.roleBadge}>
        <Text style={styles.roleText}>{user?.role}</Text>
      </View>

      <Text style={styles.sectionTitle}>Account info</Text>
      <View style={styles.infoCard}>
        <View style={styles.infoRow}>
          <Text style={styles.infoLabel}>Name</Text>
          <Text style={styles.infoValue}>{user?.name}</Text>
        </View>
        <View style={[styles.infoRow, styles.infoRowBorder]}>
          <Text style={styles.infoLabel}>Email</Text>
          <Text style={styles.infoValue}>{user?.email}</Text>
        </View>
        <View style={[styles.infoRow, styles.infoRowBorder]}>
          <Text style={styles.infoLabel}>Role</Text>
          <Text style={styles.infoValue}>{user?.role}</Text>
        </View>
      </View>

      <Pressable style={styles.logoutButton} onPress={handleLogout}>
        <Text style={styles.logoutText}>Log out</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: "#F8FAFC", alignItems: "center" },
  header: { marginTop: 20, marginBottom: 24, alignSelf: "stretch" },
  pageTitle: { fontSize: 22, fontWeight: "700", color: "#111827" },
  avatar: { width: 72, height: 72, borderRadius: 36, backgroundColor: "#EEEDFE", alignItems: "center", justifyContent: "center", marginBottom: 16 },
  avatarText: { fontSize: 28, fontWeight: "700", color: "#534AB7" },
  name: { fontSize: 20, fontWeight: "700", color: "#111827", marginBottom: 6 },
  roleBadge: { backgroundColor: "#EEEDFE", borderRadius: 20, paddingHorizontal: 10, paddingVertical: 3, marginBottom: 24 },
  roleText: { fontSize: 12, color: "#534AB7", fontWeight: "600" },
  sectionTitle: { fontSize: 12, fontWeight: "600", color: "#6B7280", textTransform: "uppercase", letterSpacing: 1, marginBottom: 10, alignSelf: "stretch" },
  infoCard: { backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 12, overflow: "hidden", marginBottom: 24, alignSelf: "stretch" },
  infoRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", padding: 14 },
  infoRowBorder: { borderTopWidth: 1, borderTopColor: "#E5E7EB" },
  infoLabel: { fontSize: 13, color: "#6B7280" },
  infoValue: { fontSize: 14, color: "#111827", fontWeight: "500" },
  logoutButton: { backgroundColor: "#FCEBEB", borderRadius: 12, padding: 14, alignItems: "center", alignSelf: "stretch" },
  logoutText: { color: "#A32D2D", fontSize: 15, fontWeight: "600" },
});