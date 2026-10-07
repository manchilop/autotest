import { View, Text, StyleSheet, Pressable, ActivityIndicator, Modal, TextInput, Alert } from "react-native";
import { router, useFocusEffect } from "expo-router";
import { useCallback, useContext, useState } from "react";
import { AuthContext } from "../../../store/AuthContext";
import { getCompletedQuestions } from "../../../services/question.service";
import { getMySubjects, joinSubject } from "../../../services/subject.service";
import { Subject } from "../../../types/subject";

export default function Home() {
  const { authState } = useContext(AuthContext);
  const name = authState.user?.name || "Student";
  const [answeredCount, setAnsweredCount] = useState<number | null>(null);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loadingSubjects, setLoadingSubjects] = useState(true);

  const [joinModalVisible, setJoinModalVisible] = useState(false);
  const [inviteCode, setInviteCode] = useState("");
  const [joining, setJoining] = useState(false);
  const [joinError, setJoinError] = useState("");

  useFocusEffect(
    useCallback(() => {
      getCompletedQuestions()
        .then((data) => setAnsweredCount(data.length))
        .catch(console.error);
      fetchSubjects();
    }, [])
  );

  const fetchSubjects = async () => {
    try {
      setLoadingSubjects(true);
      const data = await getMySubjects();
      setSubjects(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingSubjects(false);
    }
  };

  const handleJoin = async () => {
    if (!inviteCode.trim()) return;
    try {
      setJoining(true);
      setJoinError("");
      await joinSubject({ inviteCode: inviteCode.trim().toUpperCase() });
      setInviteCode("");
      setJoinModalVisible(false);
      fetchSubjects();
      Alert.alert("Success", "You've joined the subject!");
    } catch (e: any) {
      setJoinError(e?.response?.data?.message || "Invalid or already joined code");
    } finally {
      setJoining(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.greeting}>Welcome back,</Text>
      <Text style={styles.name}>{name} 👋</Text>
      <Text style={styles.subtitle}>What do you want to do today?</Text>

      <View style={styles.statRow}>
        <View style={styles.stat}>
          <Text style={styles.statLabel}>Questions answered</Text>
          <Text style={[styles.statValue, styles.purple]}>
            {answeredCount ?? "—"}
          </Text>
        </View>
      </View>

      <View style={styles.sectionHeaderRow}>
        <Text style={styles.sectionTitle}>My subjects</Text>
        <Pressable style={styles.joinBtn} onPress={() => setJoinModalVisible(true)}>
          <Text style={styles.joinBtnText}>+ Join subject</Text>
        </Pressable>
      </View>

      {loadingSubjects ? (
        <ActivityIndicator style={{ marginBottom: 16 }} />
      ) : subjects.length === 0 ? (
        <View style={styles.emptySubjects}>
          <Text style={styles.emptySubjectsText}>
            You haven't joined any subject yet. Ask your teacher for an invite code.
          </Text>
        </View>
      ) : (
        <View style={styles.subjectsWrap}>
          {subjects.map((s) => (
            <View key={s.id} style={styles.subjectChip}>
              <Text style={styles.subjectChipText}>{s.name}</Text>
            </View>
          ))}
        </View>
      )}

      <Text style={styles.sectionTitle}>Quick actions</Text>

      <Pressable style={styles.actionBtn} onPress={() => router.push("/(student)/(tabs)/practice")}>
        <View style={[styles.actionIcon, styles.iconPurple]}>
          <Text style={styles.iconText}>🧠</Text>
        </View>
        <View style={styles.actionContent}>
          <Text style={styles.actionLabel}>Practice questions</Text>
          <Text style={styles.actionSub}>Train with questions from the platform</Text>
        </View>
        <Text style={styles.chevron}>›</Text>
      </Pressable>

      <Pressable style={styles.actionBtn} onPress={() => router.push("/(student)/(tabs)/create")}>
        <View style={[styles.actionIcon, styles.iconAmber]}>
          <Text style={styles.iconText}>✏️</Text>
        </View>
        <View style={styles.actionContent}>
          <Text style={styles.actionLabel}>Create question</Text>
          <Text style={styles.actionSub}>Contribute new questions to the system</Text>
        </View>
        <Text style={styles.chevron}>›</Text>
      </Pressable>

      <Pressable style={styles.actionBtn} onPress={() => router.push("/(student)/(tabs)/library")}>
        <View style={[styles.actionIcon, styles.iconTeal]}>
          <Text style={styles.iconText}>📚</Text>
        </View>
        <View style={styles.actionContent}>
          <Text style={styles.actionLabel}>My library</Text>
          <Text style={styles.actionSub}>Review your answered questions</Text>
        </View>
        <Text style={styles.chevron}>›</Text>
      </Pressable>

      <Modal
        visible={joinModalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setJoinModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>Join a subject</Text>
            <Text style={styles.modalSub}>Enter the invite code your teacher shared with you</Text>

            <TextInput
              placeholder="e.g. A3F9K2"
              value={inviteCode}
              onChangeText={setInviteCode}
              style={styles.modalInput}
              autoCapitalize="characters"
              maxLength={10}
            />

            {joinError ? <Text style={styles.modalError}>{joinError}</Text> : null}

            <View style={styles.modalActions}>
              <Pressable
                style={styles.modalCancel}
                onPress={() => {
                  setJoinModalVisible(false);
                  setJoinError("");
                  setInviteCode("");
                }}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </Pressable>
              <Pressable
                style={[styles.modalConfirm, joining && styles.modalConfirmDisabled]}
                onPress={handleJoin}
                disabled={joining}
              >
                {joining ? (
                  <ActivityIndicator color="#fff" size="small" />
                ) : (
                  <Text style={styles.modalConfirmText}>Join</Text>
                )}
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#F8FAFC", padding: 20 },
  greeting: { fontSize: 13, color: "#6B7280", marginTop: 20 },
  name: { fontSize: 22, fontWeight: "700", color: "#111827", marginBottom: 6 },
  subtitle: { fontSize: 14, color: "#6B7280", marginBottom: 24 },
  statRow: { flexDirection: "row", gap: 10, marginBottom: 24 },
  stat: { flex: 1, backgroundColor: "#F3F4F6", borderRadius: 10, padding: 14 },
  statLabel: { fontSize: 12, color: "#6B7280", marginBottom: 4 },
  statValue: { fontSize: 24, fontWeight: "700" },
  purple: { color: "#534AB7" },
  sectionHeaderRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 10 },
  sectionTitle: { fontSize: 12, fontWeight: "600", color: "#6B7280", textTransform: "uppercase", letterSpacing: 1 },
  joinBtn: { backgroundColor: "#EEEDFE", borderRadius: 20, paddingHorizontal: 12, paddingVertical: 6 },
  joinBtnText: { fontSize: 12, fontWeight: "700", color: "#534AB7" },
  emptySubjects: { backgroundColor: "#F3F4F6", borderRadius: 10, padding: 14, marginBottom: 20 },
  emptySubjectsText: { fontSize: 13, color: "#6B7280", lineHeight: 19 },
  subjectsWrap: { flexDirection: "row", flexWrap: "wrap", gap: 8, marginBottom: 20 },
  subjectChip: { backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 20, paddingHorizontal: 14, paddingVertical: 8 },
  subjectChipText: { fontSize: 13, fontWeight: "600", color: "#111827" },
  actionBtn: { backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 12, padding: 14, flexDirection: "row", alignItems: "center", marginBottom: 8 },
  actionIcon: { width: 36, height: 36, borderRadius: 8, alignItems: "center", justifyContent: "center", marginRight: 12 },
  iconPurple: { backgroundColor: "#EEEDFE" },
  iconAmber: { backgroundColor: "#FAEEDA" },
  iconTeal: { backgroundColor: "#E1F5EE" },
  iconText: { fontSize: 18 },
  actionContent: { flex: 1 },
  actionLabel: { fontSize: 15, fontWeight: "600", color: "#111827" },
  actionSub: { fontSize: 12, color: "#6B7280", marginTop: 2 },
  chevron: { fontSize: 20, color: "#9CA3AF" },
  modalOverlay: { flex: 1, backgroundColor: "rgba(0,0,0,0.4)", justifyContent: "center", padding: 24 },
  modalCard: { backgroundColor: "#fff", borderRadius: 16, padding: 20 },
  modalTitle: { fontSize: 18, fontWeight: "700", color: "#111827", marginBottom: 4 },
  modalSub: { fontSize: 13, color: "#6B7280", marginBottom: 16 },
  modalInput: { borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 10, padding: 12, fontSize: 16, letterSpacing: 2, textAlign: "center", marginBottom: 8 },
  modalError: { color: "#A32D2D", fontSize: 13, marginBottom: 8 },
  modalActions: { flexDirection: "row", gap: 8, marginTop: 8 },
  modalCancel: { flex: 1, padding: 12, borderRadius: 10, alignItems: "center", backgroundColor: "#F3F4F6" },
  modalCancelText: { color: "#6B7280", fontWeight: "600" },
  modalConfirm: { flex: 1, padding: 12, borderRadius: 10, alignItems: "center", backgroundColor: "#111827" },
  modalConfirmDisabled: { opacity: 0.6 },
  modalConfirmText: { color: "#fff", fontWeight: "600" },
});