import { useEffect, useState } from "react";
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  Pressable,
  Alert,
  ActivityIndicator,
  ScrollView,
} from "react-native";
import { Select } from "../../../components/Select";
import { createQuestion } from "../../../services/question.service";
import { getMySubjects, getTopicsBySubject } from "../../../services/subject.service";
import { Choice, CreateQuestionRequest } from "../../../types/question";
import { Subject, Topic } from "../../../types/subject";

const LETTERS = ["A", "B", "C", "D"];

export default function Create() {
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);
  const [choices, setChoices] = useState<Choice[]>([
    { choiceText: "", correct: false },
    { choiceText: "", correct: false },
    { choiceText: "", correct: false },
    { choiceText: "", correct: false },
  ]);

  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loadingSubjects, setLoadingSubjects] = useState(true);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | null>(null);

  const [topics, setTopics] = useState<Topic[]>([]);
  const [loadingTopics, setLoadingTopics] = useState(false);
  const [selectedTopicId, setSelectedTopicId] = useState<number | null>(null);

  useEffect(() => {
    fetchSubjects();
  }, []);

  useEffect(() => {
    if (selectedSubjectId === null) {
      setTopics([]);
      setSelectedTopicId(null);
      return;
    }
    fetchTopics(selectedSubjectId);
  }, [selectedSubjectId]);

  const fetchSubjects = async () => {
    try {
      setLoadingSubjects(true);
      const data = await getMySubjects();
      setSubjects(data);
      if (data.length > 0) setSelectedSubjectId(data[0].id);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingSubjects(false);
    }
  };

  const fetchTopics = async (subjectId: number) => {
    try {
      setLoadingTopics(true);
      setSelectedTopicId(null);
      const data = await getTopicsBySubject(subjectId);
      setTopics(data);
      if (data.length > 0) setSelectedTopicId(data[0].id);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingTopics(false);
    }
  };

  const setChoiceText = (index: number, text: string) => {
    const updated = [...choices];
    updated[index] = { ...updated[index], choiceText: text };
    setChoices(updated);
  };

  const setCorrect = (index: number) => {
    setChoices(choices.map((c, i) => ({ ...c, correct: i === index })));
  };

  const resetForm = () => {
    setQuestion("");
    setChoices([
      { choiceText: "", correct: false },
      { choiceText: "", correct: false },
      { choiceText: "", correct: false },
      { choiceText: "", correct: false },
    ]);
  };

  const handleSubmit = async () => {
    if (subjects.length === 0) return Alert.alert("Error", "Join a subject before creating questions");
    if (!selectedSubjectId) return Alert.alert("Error", "Select a subject");
    if (!selectedTopicId) return Alert.alert("Error", "Select a topic");
    if (!question.trim()) return Alert.alert("Error", "Question is required");
    if (choices.some((c) => !c.choiceText.trim())) return Alert.alert("Error", "All options are required");
    if (!choices.some((c) => c.correct)) return Alert.alert("Error", "Select the correct answer");

    try {
      setLoading(true);
      const payload: CreateQuestionRequest = {
        questionText: question,
        topicId: selectedTopicId,
        choices,
      };
      await createQuestion(payload);
      Alert.alert("Success", "Question submitted for review!");
      resetForm();
    } catch (error) {
      Alert.alert("Error", "Failed to create question");
    } finally {
      setLoading(false);
    }
  };

  if (loadingSubjects) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (subjects.length === 0) {
    return (
      <View style={styles.centered}>
        <Text style={styles.emptyTitle}>No subjects yet</Text>
        <Text style={styles.emptySubtitle}>Join a subject from Home before creating questions</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <Text style={styles.pageTitle}>Create question</Text>
        <Text style={styles.pageSub}>Contribute to the question bank</Text>
      </View>

      <Text style={styles.label}>Subject</Text>
      <Select
        title="Select subject"
        placeholder="Choose a subject"
        value={selectedSubjectId}
        onChange={(value) => setSelectedSubjectId(value)}
        options={subjects.map((s) => ({ label: s.name, value: s.id }))}
      />

      <Text style={styles.label}>Topic</Text>
      {loadingTopics ? (
        <ActivityIndicator style={{ marginBottom: 12 }} />
      ) : topics.length === 0 ? (
        <Text style={styles.noTopics}>This subject has no topics yet. Ask your teacher to add one.</Text>
      ) : (
        <Select
          title="Select topic"
          placeholder="Choose a topic"
          value={selectedTopicId}
          onChange={(value) => setSelectedTopicId(value)}
          options={topics.map((t) => ({ label: t.name, value: t.id }))}
        />
      )}

      <Text style={styles.label}>Question</Text>
      <TextInput
        placeholder="Write your question here..."
        value={question}
        onChangeText={setQuestion}
        style={styles.input}
        multiline
      />

      <Text style={styles.label}>Answer options</Text>

      {choices.map((choice, index) => (
        <View key={index} style={styles.choiceRow}>
          <Pressable
            style={[styles.letterBadge, choice.correct && styles.letterBadgeCorrect]}
            onPress={() => setCorrect(index)}
          >
            <Text style={[styles.letterText, choice.correct && styles.letterTextCorrect]}>
              {LETTERS[index]}
            </Text>
          </Pressable>

          <TextInput
            placeholder={`Option ${LETTERS[index]}`}
            value={choice.choiceText}
            onChangeText={(text) => setChoiceText(index, text)}
            style={[styles.choiceInput, choice.correct && styles.choiceInputCorrect]}
          />
        </View>
      ))}

      <Text style={styles.help}>Tap a letter to mark the correct answer</Text>

      <Pressable
        style={[styles.button, (loading || topics.length === 0) && styles.buttonDisabled]}
        onPress={handleSubmit}
        disabled={loading || topics.length === 0}
      >
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Submit question →</Text>
        )}
      </Pressable>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#F8FAFC" },
  content: { padding: 20, gap: 4 },
  centered: { flex: 1, justifyContent: "center", alignItems: "center", padding: 20 },
  header: { marginTop: 20, marginBottom: 24 },
  pageTitle: { fontSize: 22, fontWeight: "700", color: "#111827" },
  pageSub: { fontSize: 13, color: "#6B7280", marginTop: 2 },
  label: { fontSize: 12, fontWeight: "600", color: "#6B7280", textTransform: "uppercase", letterSpacing: 1, marginBottom: 8, marginTop: 12 },
  noTopics: { fontSize: 13, color: "#9CA3AF", marginBottom: 12 },
  input: { backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 12, padding: 14, fontSize: 15, color: "#111827", minHeight: 80, textAlignVertical: "top", marginBottom: 8 },
  choiceRow: { flexDirection: "row", alignItems: "center", gap: 10, marginBottom: 8 },
  letterBadge: { width: 32, height: 32, borderRadius: 16, backgroundColor: "#F3F4F6", borderWidth: 1, borderColor: "#E5E7EB", alignItems: "center", justifyContent: "center" },
  letterBadgeCorrect: { backgroundColor: "#3B6D11", borderColor: "#3B6D11" },
  letterText: { fontSize: 13, fontWeight: "600", color: "#6B7280" },
  letterTextCorrect: { color: "#fff" },
  choiceInput: { flex: 1, backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 8, padding: 12, fontSize: 14, color: "#111827" },
  choiceInputCorrect: { borderColor: "#3B6D11", backgroundColor: "#EAF3DE" },
  help: { fontSize: 12, color: "#9CA3AF", marginTop: 4, marginBottom: 20 },
  button: { backgroundColor: "#111827", borderRadius: 12, padding: 14, alignItems: "center" },
  buttonDisabled: { opacity: 0.6 },
  buttonText: { color: "#fff", fontSize: 15, fontWeight: "600" },
  emptyTitle: { fontSize: 20, fontWeight: "700", color: "#111827" },
  emptySubtitle: { fontSize: 14, color: "#6B7280", marginTop: 6, textAlign: "center" },
});