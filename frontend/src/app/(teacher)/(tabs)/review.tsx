import { useCallback, useState } from "react";
import { useFocusEffect } from "expo-router";
import {
  View,
  Text,
  StyleSheet,
  Pressable,
  ActivityIndicator,
} from "react-native";
import {
  getQuestionsByStatus,
  approveQuestion,
  rejectQuestion,
} from "../../../services/question.service";
import { QuestionResponse } from "../../../types/question";

export default function Review() {
  const [questions, setQuestions] = useState<QuestionResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useFocusEffect(
    useCallback(() => {
      loadQuestions();
    }, [])
  );

  const loadQuestions = async () => {
    try {
      const data = await getQuestionsByStatus("PENDING");
      setQuestions(data);
    } catch (error) {
      console.log(error);
    } finally {
      setLoading(false);
    }
  };

  const currentQuestion = questions[0];

  const removeCurrentQuestion = () => setQuestions((prev) => prev.slice(1));

  const handleApprove = async () => {
    if (!currentQuestion) return;
    try {
      await approveQuestion(currentQuestion.id);
      removeCurrentQuestion();
    } catch (error) {
      console.log(error);
    }
  };

  const handleReject = async () => {
    if (!currentQuestion) return;
    try {
      await rejectQuestion(currentQuestion.id);
      removeCurrentQuestion();
    } catch (error) {
      console.log(error);
    }
  };

  const handleSkip = () => {
    if (!currentQuestion) return;
    setQuestions((prev) => [...prev.slice(1), prev[0]]);
  };

  if (loading) return <ActivityIndicator style={{ flex: 1 }} size="large" />;

  if (!currentQuestion) {
    return (
      <View style={styles.center}>
        <Text style={styles.emptyTitle}>All caught up! 🎉</Text>
        <Text style={styles.emptySubtitle}>No pending questions to review.</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.pageTitle}>Review questions</Text>
      <Text style={styles.pageSub}>Approve or reject student submissions</Text>

      <View style={styles.counter}>
        <Text style={styles.counterText}>🕐 {questions.length} pending</Text>
      </View>

      <View style={styles.card}>
        {(currentQuestion.subjectName || currentQuestion.topicName) && (
          <View style={styles.tagsRow}>
            {currentQuestion.subjectName && (
              <View style={styles.subjectTag}>
                <Text style={styles.subjectTagText}>{currentQuestion.subjectName}</Text>
              </View>
            )}
            {currentQuestion.topicName && (
              <View style={styles.topicTag}>
                <Text style={styles.topicTagText}>{currentQuestion.topicName}</Text>
              </View>
            )}
          </View>
        )}
        <Text style={styles.question}>{currentQuestion.questionText}</Text>
        <View style={styles.choices}>
          {currentQuestion.choices.map((choice) => (
            <View
              key={choice.id}
              style={[styles.choice, choice.correct && styles.choiceCorrect]}
            >
              {choice.correct && <Text style={styles.check}>✓</Text>}
              <Text style={[styles.choiceText, choice.correct && styles.choiceTextCorrect]}>
                {choice.choiceText}
              </Text>
            </View>
          ))}
        </View>
      </View>

      <View style={styles.actions}>
        <Pressable style={[styles.button, styles.rejectButton]} onPress={handleReject}>
          <Text style={styles.rejectText}>✕ Reject</Text>
        </Pressable>
        <Pressable style={[styles.button, styles.skipButton]} onPress={handleSkip}>
          <Text style={styles.skipText}>→ Skip</Text>
        </Pressable>
        <Pressable style={[styles.button, styles.approveButton]} onPress={handleApprove}>
          <Text style={styles.approveText}>✓ Approve</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#F8FAFC", padding: 20 },
  center: { flex: 1, justifyContent: "center", alignItems: "center" },
  pageTitle: { fontSize: 22, fontWeight: "700", color: "#111827", marginTop: 20, marginBottom: 2 },
  pageSub: { fontSize: 13, color: "#6B7280", marginBottom: 16 },
  counter: { alignSelf: "flex-start", backgroundColor: "#FAEEDA", borderRadius: 20, paddingHorizontal: 12, paddingVertical: 4, marginBottom: 20 },
  counterText: { fontSize: 13, color: "#633806", fontWeight: "600" },
  card: { backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 12, padding: 20 },
  tagsRow: { flexDirection: "row", gap: 6, marginBottom: 12 },
  subjectTag: { backgroundColor: "#EEEDFE", borderRadius: 10, paddingHorizontal: 8, paddingVertical: 3 },
  subjectTagText: { fontSize: 11, fontWeight: "700", color: "#534AB7" },
  topicTag: { backgroundColor: "#E1F5EE", borderRadius: 10, paddingHorizontal: 8, paddingVertical: 3 },
  topicTagText: { fontSize: 11, fontWeight: "700", color: "#0F6E56" },
  question: { fontSize: 18, fontWeight: "600", color: "#111827", marginBottom: 20, lineHeight: 26 },
  choices: { gap: 8 },
  choice: { backgroundColor: "#F3F4F6", borderRadius: 8, padding: 12, flexDirection: "row", alignItems: "center", gap: 10 },
  choiceCorrect: { backgroundColor: "#EAF3DE" },
  choiceText: { fontSize: 14, color: "#374151", flex: 1 },
  choiceTextCorrect: { color: "#3B6D11", fontWeight: "600" },
  check: { fontSize: 14, color: "#639922" },
  actions: { flexDirection: "row", gap: 8, marginTop: 20 },
  button: { flex: 1, padding: 14, borderRadius: 10, alignItems: "center" },
  rejectButton: { backgroundColor: "#DC2626" },
  rejectText: { color: "#fff", fontWeight: "600", fontSize: 15 },
  skipButton: { backgroundColor: "#F3F4F6" },
  skipText: { color: "#6B7280", fontWeight: "600", fontSize: 15 },
  approveButton: { backgroundColor: "#16A34A" },
  approveText: { color: "#fff", fontWeight: "600", fontSize: 15 },
  emptyTitle: { fontSize: 22, fontWeight: "700", color: "#111827" },
  emptySubtitle: { marginTop: 8, color: "#6B7280", fontSize: 14 },
});