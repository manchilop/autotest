import { useCallback, useContext, useState } from "react";
import { View, Text, StyleSheet, Pressable } from "react-native";
import { router, useFocusEffect } from "expo-router";
import { getQuestionsByStatus } from "../../../services/question.service";
import { getMySubjects } from "../../../services/subject.service";
import { AuthContext } from "../../../store/AuthContext";

export default function Home() {
  const { authState } = useContext(AuthContext);
  const [pendingCount, setPendingCount] = useState<number | null>(null);
  const [approvedCount, setApprovedCount] = useState<number | null>(null);
  const [subjectCount, setSubjectCount] = useState<number | null>(null);

  useFocusEffect(
    useCallback(() => {
      fetchCounts();
    }, [])
  );

  const fetchCounts = async () => {
    try {
      const [pending, approved, subjects] = await Promise.all([
        getQuestionsByStatus("PENDING"),
        getQuestionsByStatus("APPROVED"),
        getMySubjects(),
      ]);
      setPendingCount(pending.length);
      setApprovedCount(approved.length);
      setSubjectCount(subjects.length);
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.greeting}>Good morning,</Text>
      <Text style={styles.name}>{authState.user?.name ?? "Teacher"} 👋</Text>

      <View style={styles.statRow}>
        <View style={styles.stat}>
          <Text style={styles.statLabel}>Subjects</Text>
          <Text style={[styles.statValue, styles.purple]}>
            {subjectCount ?? "—"}
          </Text>
        </View>
        <View style={styles.stat}>
          <Text style={styles.statLabel}>Pending review</Text>
          <Text style={[styles.statValue, styles.amber]}>
            {pendingCount ?? "—"}
          </Text>
        </View>
        <View style={styles.stat}>
          <Text style={styles.statLabel}>Approved</Text>
          <Text style={[styles.statValue, styles.green]}>
            {approvedCount ?? "—"}
          </Text>
        </View>
      </View>

      {!!pendingCount && pendingCount > 0 && (
        <View style={styles.alert}>
          <Text style={styles.alertText}>
            You have{" "}
            <Text style={styles.alertBold}>{pendingCount} questions</Text>{" "}
            waiting for your review.
          </Text>
        </View>
      )}

      <Text style={styles.sectionTitle}>Quick actions</Text>

      <Pressable style={styles.actionBtn} onPress={() => router.push("/(teacher)/(tabs)/review")}>
        <View style={[styles.actionIcon, styles.iconAmber]}>
          <Text style={styles.iconText}>📋</Text>
        </View>
        <View style={styles.actionContent}>
          <Text style={styles.actionLabel}>
            Review questions{" "}
            {!!pendingCount && <Text style={styles.badge}> {pendingCount} </Text>}
          </Text>
          <Text style={styles.actionSub}>Approve or reject pending submissions</Text>
        </View>
        <Text style={styles.chevron}>›</Text>
      </Pressable>

      <Pressable style={styles.actionBtn} onPress={() => router.push("/(teacher)/(tabs)/library")}>
        <View style={[styles.actionIcon, styles.iconTeal]}>
          <Text style={styles.iconText}>📚</Text>
        </View>
        <View style={styles.actionContent}>
          <Text style={styles.actionLabel}>Question library</Text>
          <Text style={styles.actionSub}>Browse subjects, topics and reviewed questions</Text>
        </View>
        <Text style={styles.chevron}>›</Text>
      </Pressable>

      <Pressable style={styles.actionBtn} onPress={() => router.push("/(teacher)/(tabs)/profile")}>
        <View style={[styles.actionIcon, styles.iconPurple]}>
          <Text style={styles.iconText}>👤</Text>
        </View>
        <View style={styles.actionContent}>
          <Text style={styles.actionLabel}>Profile</Text>
          <Text style={styles.actionSub}>Manage your account settings</Text>
        </View>
        <Text style={styles.chevron}>›</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: "#F8FAFC" },
  greeting: { fontSize: 13, color: "#6B7280", marginTop: 20 },
  name: { fontSize: 22, fontWeight: "700", color: "#111827", marginBottom: 20 },
  statRow: { flexDirection: "row", gap: 8, marginBottom: 16 },
  stat: { flex: 1, backgroundColor: "#F3F4F6", borderRadius: 10, padding: 12 },
  statLabel: { fontSize: 11, color: "#6B7280", marginBottom: 4 },
  statValue: { fontSize: 22, fontWeight: "700" },
  purple: { color: "#534AB7" },
  amber: { color: "#BA7517" },
  green: { color: "#3B6D11" },
  alert: { backgroundColor: "#FAEEDA", borderRadius: 12, padding: 14, marginBottom: 16 },
  alertText: { fontSize: 14, color: "#633806", lineHeight: 20 },
  alertBold: { fontWeight: "700" },
  sectionTitle: { fontSize: 12, fontWeight: "600", color: "#6B7280", textTransform: "uppercase", letterSpacing: 1, marginBottom: 10 },
  actionBtn: { backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 12, padding: 14, flexDirection: "row", alignItems: "center", marginBottom: 8 },
  actionIcon: { width: 36, height: 36, borderRadius: 8, alignItems: "center", justifyContent: "center", marginRight: 12 },
  iconAmber: { backgroundColor: "#FAEEDA" },
  iconTeal: { backgroundColor: "#E1F5EE" },
  iconPurple: { backgroundColor: "#EEEDFE" },
  iconText: { fontSize: 18 },
  actionContent: { flex: 1 },
  actionLabel: { fontSize: 15, fontWeight: "600", color: "#111827" },
  actionSub: { fontSize: 12, color: "#6B7280", marginTop: 2 },
  badge: { backgroundColor: "#BA7517", color: "#fff", fontSize: 11, fontWeight: "700", borderRadius: 20, paddingHorizontal: 6 },
  chevron: { fontSize: 20, color: "#9CA3AF" },
});