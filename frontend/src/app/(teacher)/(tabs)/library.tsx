import { useState, useCallback } from "react";
import { useFocusEffect } from "expo-router";
import {
  View,
  Text,
  StyleSheet,
  Pressable,
  ScrollView,
  ActivityIndicator,
  Modal,
  TextInput,
  Alert,
} from "react-native";
import * as Clipboard from "expo-clipboard";
import { getQuestionsByStatus } from "../../../services/question.service";
import { getMySubjects, createSubject, createTopic } from "../../../services/subject.service";
import { QuestionResponse } from "../../../types/question";
import { Subject } from "../../../types/subject";

export default function Library() {
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [approved, setApproved] = useState<QuestionResponse[]>([]);
  const [rejected, setRejected] = useState<QuestionResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loadingSubjects, setLoadingSubjects] = useState(true);
  const [expandedSubjectId, setExpandedSubjectId] = useState<number | null>(null);
  const [activeSubjectId, setActiveSubjectId] = useState<number | null>(null); // null = all

  const [subjectModalVisible, setSubjectModalVisible] = useState(false);
  const [subjectName, setSubjectName] = useState("");
  const [creatingSubject, setCreatingSubject] = useState(false);

  const [topicModalSubjectId, setTopicModalSubjectId] = useState<number | null>(null);
  const [topicName, setTopicName] = useState("");
  const [creatingTopic, setCreatingTopic] = useState(false);

  const [error, setError] = useState("");

  useFocusEffect(
    useCallback(() => {
      fetchQuestions();
      fetchSubjects();
    }, [])
  );

  const fetchQuestions = async () => {
    try {
      const [approvedData, rejectedData] = await Promise.all([
        getQuestionsByStatus("APPROVED"),
        getQuestionsByStatus("REJECTED"),
      ]);
      setApproved(approvedData);
      setRejected(rejectedData);
    } catch (error) {
      console.log(error);
    } finally {
      setLoading(false);
    }
  };

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

  const handleCreateSubject = async () => {
    if (!subjectName.trim()) return;
    try {
      setCreatingSubject(true);
      setError("");
      await createSubject({ name: subjectName.trim() });
      setSubjectName("");
      setSubjectModalVisible(false);
      fetchSubjects();
    } catch (e: any) {
      setError(e?.response?.data?.message || "Failed to create subject");
    } finally {
      setCreatingSubject(false);
    }
  };

  const handleCreateTopic = async () => {
    if (!topicName.trim() || topicModalSubjectId === null) return;
    try {
      setCreatingTopic(true);
      setError("");
      await createTopic(topicModalSubjectId, { name: topicName.trim() });
      setTopicName("");
      setTopicModalSubjectId(null);
      fetchSubjects();
    } catch (e: any) {
      setError(e?.response?.data?.message || "Failed to create topic");
    } finally {
      setCreatingTopic(false);
    }
  };

  const copyInviteCode = async (code: string) => {
    await Clipboard.setStringAsync(code);
    Alert.alert("Copied", `Invite code "${code}" copied to clipboard`);
  };

  const matchesActiveSubject = (q: QuestionResponse) =>
    activeSubjectId === null || q.subjectId === activeSubjectId;

  const filteredApproved = approved.filter(matchesActiveSubject);
  const filteredRejected = rejected.filter(matchesActiveSubject);

  const renderQuestion = (question: QuestionResponse, dot: "green" | "red") => (
    <Pressable
      key={question.id}
      style={styles.card}
      onPress={() => setExpandedId(expandedId === question.id ? null : question.id)}
    >
      <View style={styles.cardHeader}>
        <View style={[styles.dot, dot === "green" ? styles.dotGreen : styles.dotRed]} />
        <View style={{ flex: 1 }}>
          {question.topicName && (
            <Text style={styles.topicLabel}>{question.subjectName} · {question.topicName}</Text>
          )}
          <Text style={styles.questionText}>{question.questionText}</Text>
        </View>
        <Text style={styles.chevron}>{expandedId === question.id ? "↓" : "›"}</Text>
      </View>

      {expandedId === question.id && (
        <View style={styles.choices}>
          {question.choices.map((choice) => (
            <View key={choice.id} style={styles.choiceRow}>
              {choice.correct && <Text style={styles.check}>✓</Text>}
              <Text style={[styles.choiceText, choice.correct && styles.correctChoice]}>
                {choice.choiceText}
              </Text>
            </View>
          ))}
        </View>
      )}
    </Pressable>
  );

  if (loading) return <ActivityIndicator style={{ flex: 1 }} size="large" />;

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.pageTitle}>Question library</Text>
      <Text style={styles.pageSub}>All reviewed questions</Text>

      {/* ── Subjects management ──────────────────────────────────── */}
      <View style={styles.sectionHeaderRow}>
        <Text style={styles.sectionTitle}>Subjects</Text>
        <Pressable style={styles.addBtn} onPress={() => setSubjectModalVisible(true)}>
          <Text style={styles.addBtnText}>+ New subject</Text>
        </Pressable>
      </View>

      {loadingSubjects ? (
        <ActivityIndicator style={{ marginBottom: 16 }} />
      ) : subjects.length === 0 ? (
        <Text style={styles.empty}>You haven't created any subject yet</Text>
      ) : (
        <View style={{ marginBottom: 16 }}>
          {/* "All" filter chip */}
          <View style={styles.filterRow}>
            <Pressable
              style={[styles.filterChip, activeSubjectId === null && styles.filterChipActive]}
              onPress={() => setActiveSubjectId(null)}
            >
              <Text style={[styles.filterChipText, activeSubjectId === null && styles.filterChipTextActive]}>
                All
              </Text>
            </Pressable>
            {subjects.map((s) => (
              <Pressable
                key={s.id}
                style={[styles.filterChip, activeSubjectId === s.id && styles.filterChipActive]}
                onPress={() => setActiveSubjectId(s.id)}
              >
                <Text style={[styles.filterChipText, activeSubjectId === s.id && styles.filterChipTextActive]}>
                  {s.name}
                </Text>
              </Pressable>
            ))}
          </View>

          {subjects.map((s) => {
            const expanded = expandedSubjectId === s.id;
            return (
              <View key={s.id} style={styles.subjectCard}>
                <Pressable
                  style={styles.subjectCardHeader}
                  onPress={() => setExpandedSubjectId(expanded ? null : s.id)}
                >
                  <View style={{ flex: 1 }}>
                    <Text style={styles.subjectName}>{s.name}</Text>
                    <Text style={styles.subjectMeta}>{s.topics.length} topic(s)</Text>
                  </View>
                  <Pressable style={styles.codeBtn} onPress={() => copyInviteCode(s.inviteCode)}>
                    <Text style={styles.codeBtnText}>{s.inviteCode} 📋</Text>
                  </Pressable>
                  <Text style={styles.chevron}>{expanded ? "↓" : "›"}</Text>
                </Pressable>

                {expanded && (
                  <View style={styles.topicsBox}>
                    {s.topics.length === 0 ? (
                      <Text style={styles.empty}>No topics yet</Text>
                    ) : (
                      s.topics.map((t) => (
                        <View key={t.id} style={styles.topicRow}>
                          <Text style={styles.topicRowText}>{t.name}</Text>
                        </View>
                      ))
                    )}
                    <Pressable
                      style={styles.addTopicBtn}
                      onPress={() => setTopicModalSubjectId(s.id)}
                    >
                      <Text style={styles.addTopicBtnText}>+ Add topic</Text>
                    </Pressable>
                  </View>
                )}
              </View>
            );
          })}
        </View>
      )}

      {/* ── Approved / Rejected questions ────────────────────────── */}
      <View style={styles.statRow}>
        <View style={styles.stat}>
          <Text style={styles.statLabel}>Approved</Text>
          <Text style={[styles.statValue, styles.green]}>{filteredApproved.length}</Text>
        </View>
        <View style={styles.stat}>
          <Text style={styles.statLabel}>Rejected</Text>
          <Text style={[styles.statValue, styles.red]}>{filteredRejected.length}</Text>
        </View>
      </View>

      <View style={styles.sectionHeader}>
        <Text style={styles.sectionTitle}>Approved</Text>
        <View style={styles.badgeGreen}>
          <Text style={styles.badgeGreenText}>{filteredApproved.length}</Text>
        </View>
      </View>
      {filteredApproved.length === 0
        ? <Text style={styles.empty}>No approved questions</Text>
        : filteredApproved.map((q) => renderQuestion(q, "green"))}

      <View style={[styles.sectionHeader, { marginTop: 24 }]}>
        <Text style={styles.sectionTitle}>Rejected</Text>
        <View style={styles.badgeRed}>
          <Text style={styles.badgeRedText}>{filteredRejected.length}</Text>
        </View>
      </View>
      {filteredRejected.length === 0
        ? <Text style={styles.empty}>No rejected questions</Text>
        : filteredRejected.map((q) => renderQuestion(q, "red"))}

      {/* ── Create Subject modal ─────────────────────────────────── */}
      <Modal
        visible={subjectModalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setSubjectModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>New subject</Text>
            <TextInput
              placeholder="e.g. Programación I"
              value={subjectName}
              onChangeText={setSubjectName}
              style={styles.modalInput}
            />
            {error ? <Text style={styles.modalError}>{error}</Text> : null}
            <View style={styles.modalActions}>
              <Pressable
                style={styles.modalCancel}
                onPress={() => { setSubjectModalVisible(false); setSubjectName(""); setError(""); }}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </Pressable>
              <Pressable
                style={[styles.modalConfirm, creatingSubject && styles.modalConfirmDisabled]}
                onPress={handleCreateSubject}
                disabled={creatingSubject}
              >
                {creatingSubject ? <ActivityIndicator color="#fff" size="small" /> : <Text style={styles.modalConfirmText}>Create</Text>}
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>

      {/* ── Create Topic modal ───────────────────────────────────── */}
      <Modal
        visible={topicModalSubjectId !== null}
        transparent
        animationType="fade"
        onRequestClose={() => setTopicModalSubjectId(null)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>New topic</Text>
            <TextInput
              placeholder="e.g. Estructuras de control"
              value={topicName}
              onChangeText={setTopicName}
              style={styles.modalInput}
            />
            {error ? <Text style={styles.modalError}>{error}</Text> : null}
            <View style={styles.modalActions}>
              <Pressable
                style={styles.modalCancel}
                onPress={() => { setTopicModalSubjectId(null); setTopicName(""); setError(""); }}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </Pressable>
              <Pressable
                style={[styles.modalConfirm, creatingTopic && styles.modalConfirmDisabled]}
                onPress={handleCreateTopic}
                disabled={creatingTopic}
              >
                {creatingTopic ? <ActivityIndicator color="#fff" size="small" /> : <Text style={styles.modalConfirmText}>Create</Text>}
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: "#F8FAFC" },
  pageTitle: { fontSize: 22, fontWeight: "700", color: "#111827", marginTop: 20, marginBottom: 2 },
  pageSub: { fontSize: 13, color: "#6B7280", marginBottom: 20 },
  sectionHeaderRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 10 },
  addBtn: { backgroundColor: "#EEEDFE", borderRadius: 20, paddingHorizontal: 12, paddingVertical: 6 },
  addBtnText: { fontSize: 12, fontWeight: "700", color: "#534AB7" },
  filterRow: { flexDirection: "row", flexWrap: "wrap", gap: 8, marginBottom: 12 },
  filterChip: { backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 20, paddingHorizontal: 12, paddingVertical: 6 },
  filterChipActive: { backgroundColor: "#111827", borderColor: "#111827" },
  filterChipText: { fontSize: 12, fontWeight: "600", color: "#6B7280" },
  filterChipTextActive: { color: "#fff" },
  subjectCard: { backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 12, marginBottom: 8, overflow: "hidden" },
  subjectCardHeader: { flexDirection: "row", alignItems: "center", gap: 10, padding: 14 },
  subjectName: { fontSize: 14, fontWeight: "700", color: "#111827" },
  subjectMeta: { fontSize: 12, color: "#6B7280", marginTop: 2 },
  codeBtn: { backgroundColor: "#F3F4F6", borderRadius: 8, paddingHorizontal: 8, paddingVertical: 4 },
  codeBtnText: { fontSize: 11, fontWeight: "700", color: "#374151" },
  topicsBox: { borderTopWidth: 1, borderTopColor: "#E5E7EB", padding: 12, gap: 6 },
  topicRow: { backgroundColor: "#F3F4F6", borderRadius: 8, padding: 10 },
  topicRowText: { fontSize: 13, color: "#111827", fontWeight: "600" },
  addTopicBtn: { alignSelf: "flex-start", marginTop: 4 },
  addTopicBtnText: { fontSize: 13, fontWeight: "700", color: "#534AB7" },
  statRow: { flexDirection: "row", gap: 10, marginBottom: 24 },
  stat: { flex: 1, backgroundColor: "#F3F4F6", borderRadius: 10, padding: 14 },
  statLabel: { fontSize: 12, color: "#6B7280", marginBottom: 4 },
  statValue: { fontSize: 24, fontWeight: "700" },
  green: { color: "#3B6D11" },
  red: { color: "#A32D2D" },
  sectionHeader: { flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 10 },
  sectionTitle: { fontSize: 12, fontWeight: "600", color: "#6B7280", textTransform: "uppercase", letterSpacing: 1 },
  badgeGreen: { backgroundColor: "#EAF3DE", borderRadius: 20, paddingHorizontal: 8, paddingVertical: 2 },
  badgeGreenText: { fontSize: 11, fontWeight: "600", color: "#3B6D11" },
  badgeRed: { backgroundColor: "#FCEBEB", borderRadius: 20, paddingHorizontal: 8, paddingVertical: 2 },
  badgeRedText: { fontSize: 11, fontWeight: "600", color: "#A32D2D" },
  card: { backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 12, marginBottom: 8, overflow: "hidden" },
  cardHeader: { flexDirection: "row", alignItems: "center", gap: 10, padding: 14 },
  dot: { width: 8, height: 8, borderRadius: 4 },
  dotGreen: { backgroundColor: "#639922" },
  dotRed: { backgroundColor: "#E24B4A" },
  topicLabel: { fontSize: 11, fontWeight: "700", color: "#9CA3AF", marginBottom: 2, textTransform: "uppercase", letterSpacing: 0.5 },
  questionText: { fontSize: 14, color: "#111827" },
  chevron: { fontSize: 18, color: "#9CA3AF" },
  choices: { borderTopWidth: 1, borderTopColor: "#E5E7EB", padding: 12, gap: 6 },
  choiceRow: { flexDirection: "row", alignItems: "center", gap: 8 },
  choiceText: { fontSize: 13, color: "#6B7280" },
  correctChoice: { color: "#3B6D11", fontWeight: "600" },
  check: { fontSize: 14, color: "#639922" },
  empty: { fontSize: 13, color: "#9CA3AF", paddingVertical: 12 },
  modalOverlay: { flex: 1, backgroundColor: "rgba(0,0,0,0.4)", justifyContent: "center", padding: 24 },
  modalCard: { backgroundColor: "#fff", borderRadius: 16, padding: 20 },
  modalTitle: { fontSize: 18, fontWeight: "700", color: "#111827", marginBottom: 16 },
  modalInput: { borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 10, padding: 12, fontSize: 15, marginBottom: 8 },
  modalError: { color: "#A32D2D", fontSize: 13, marginBottom: 8 },
  modalActions: { flexDirection: "row", gap: 8, marginTop: 8 },
  modalCancel: { flex: 1, padding: 12, borderRadius: 10, alignItems: "center", backgroundColor: "#F3F4F6" },
  modalCancelText: { color: "#6B7280", fontWeight: "600" },
  modalConfirm: { flex: 1, padding: 12, borderRadius: 10, alignItems: "center", backgroundColor: "#111827" },
  modalConfirmDisabled: { opacity: 0.6 },
  modalConfirmText: { color: "#fff", fontWeight: "600" },
});