import { useState, useCallback, useMemo } from "react";
import { useFocusEffect } from "expo-router";
import {
  View,
  Text,
  StyleSheet,
  Pressable,
  ScrollView,
  ActivityIndicator,
} from "react-native";
import { getCompletedQuestions } from "../../../services/question.service";
import { QuestionResponse } from "../../../types/question";

const LETTERS = ["A", "B", "C", "D", "E"];

const NO_SUBJECT = "Sin asignatura";
const NO_TOPIC = "Sin tema";

interface TopicGroup {
  topicName: string;
  questions: QuestionResponse[];
}

interface SubjectGroup {
  subjectName: string;
  topics: TopicGroup[];
  count: number;
}

function groupQuestions(questions: QuestionResponse[]): SubjectGroup[] {
  const subjectMap = new Map<string, Map<string, QuestionResponse[]>>();

  for (const q of questions) {
    const subject = q.subjectName ?? NO_SUBJECT;
    const topic = q.topicName ?? NO_TOPIC;

    if (!subjectMap.has(subject)) subjectMap.set(subject, new Map());
    const topicMap = subjectMap.get(subject)!;
    if (!topicMap.has(topic)) topicMap.set(topic, []);
    topicMap.get(topic)!.push(q);
  }

  return Array.from(subjectMap.entries()).map(([subjectName, topicMap]) => {
    const topics = Array.from(topicMap.entries()).map(([topicName, qs]) => ({
      topicName,
      questions: qs,
    }));
    const count = topics.reduce((acc, t) => acc + t.questions.length, 0);
    return { subjectName, topics, count };
  });
}

export default function Library() {
  const [questions, setQuestions] = useState<QuestionResponse[]>([]);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [collapsedSubjects, setCollapsedSubjects] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState(false);

  useFocusEffect(
    useCallback(() => {
      fetchCompleted();
    }, [])
  );

  const fetchCompleted = async () => {
    try {
      setLoading(true);
      const data = await getCompletedQuestions();
      setQuestions(data);
    } finally {
      setLoading(false);
    }
  };

  const groups = useMemo(() => groupQuestions(questions), [questions]);

  const toggle = (id: number) => setExpandedId(expandedId === id ? null : id);
  const toggleSubject = (name: string) =>
    setCollapsedSubjects((prev) => ({ ...prev, [name]: !prev[name] }));

  const renderQuestion = (item: QuestionResponse) => {
    const expanded = expandedId === item.id;
    return (
      <Pressable key={item.id} style={styles.card} onPress={() => toggle(item.id)}>
        <View style={styles.cardHeader}>
          <Text style={styles.questionText}>{item.questionText}</Text>
          <Text style={styles.chevron}>{expanded ? "↓" : "›"}</Text>
        </View>

        {expanded && (
          <View style={styles.choices}>
            {item.choices.map((c, index) => (
              <View key={c.id} style={[styles.choiceRow, c.correct && styles.choiceRowCorrect]}>
                <View style={[styles.letter, c.correct && styles.letterCorrect]}>
                  <Text style={[styles.letterText, c.correct && styles.letterTextCorrect]}>
                    {LETTERS[index]}
                  </Text>
                </View>
                <Text style={[styles.choiceText, c.correct && styles.choiceTextCorrect]}>
                  {c.choiceText}
                </Text>
              </View>
            ))}
          </View>
        )}
      </Pressable>
    );
  };

  if (loading) return <ActivityIndicator style={{ flex: 1 }} size="large" />;

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.pageTitle}>My Library</Text>
        <Text style={styles.pageSub}>Questions you've already answered</Text>
      </View>

      {questions.length > 0 && (
        <View style={styles.countPill}>
          <Text style={styles.countText}>📚 {questions.length} questions</Text>
        </View>
      )}

      {questions.length === 0 ? (
        <View style={styles.empty}>
          <Text style={styles.emptyTitle}>Nothing here yet</Text>
          <Text style={styles.emptySub}>Answer questions in Practice to fill your library</Text>
        </View>
      ) : (
        <ScrollView showsVerticalScrollIndicator={false}>
          {groups.map((subject) => {
            const collapsed = collapsedSubjects[subject.subjectName];
            return (
              <View key={subject.subjectName} style={styles.subjectSection}>
                <Pressable
                  style={styles.subjectHeader}
                  onPress={() => toggleSubject(subject.subjectName)}
                >
                  <Text style={styles.subjectTitle}>{subject.subjectName}</Text>
                  <View style={styles.subjectCountBadge}>
                    <Text style={styles.subjectCountText}>{subject.count}</Text>
                  </View>
                  <Text style={styles.subjectChevron}>{collapsed ? "›" : "↓"}</Text>
                </Pressable>

                {!collapsed &&
                  subject.topics.map((topic) => (
                    <View key={topic.topicName} style={styles.topicSection}>
                      <Text style={styles.topicTitle}>{topic.topicName}</Text>
                      {topic.questions.map(renderQuestion)}
                    </View>
                  ))}
              </View>
            );
          })}
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#F8FAFC", padding: 20 },
  header: { marginTop: 20, marginBottom: 16 },
  pageTitle: { fontSize: 22, fontWeight: "700", color: "#111827" },
  pageSub: { fontSize: 13, color: "#6B7280", marginTop: 2 },
  countPill: { alignSelf: "flex-start", backgroundColor: "#EEEDFE", borderRadius: 20, paddingHorizontal: 12, paddingVertical: 4, marginBottom: 16 },
  countText: { fontSize: 13, color: "#534AB7", fontWeight: "600" },
  subjectSection: { marginBottom: 20 },
  subjectHeader: { flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 10 },
  subjectTitle: { fontSize: 16, fontWeight: "700", color: "#111827" },
  subjectCountBadge: { backgroundColor: "#EEEDFE", borderRadius: 20, paddingHorizontal: 8, paddingVertical: 1 },
  subjectCountText: { fontSize: 11, fontWeight: "700", color: "#534AB7" },
  subjectChevron: { marginLeft: "auto", fontSize: 16, color: "#9CA3AF" },
  topicSection: { marginBottom: 12 },
  topicTitle: { fontSize: 12, fontWeight: "700", color: "#0F6E56", textTransform: "uppercase", letterSpacing: 0.5, marginBottom: 6, marginLeft: 2 },
  card: { backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 12, marginBottom: 8, overflow: "hidden" },
  cardHeader: { flexDirection: "row", alignItems: "center", padding: 14, gap: 10 },
  questionText: { flex: 1, fontSize: 14, fontWeight: "600", color: "#111827", lineHeight: 20 },
  chevron: { fontSize: 18, color: "#9CA3AF" },
  choices: { borderTopWidth: 1, borderTopColor: "#E5E7EB", padding: 10, gap: 6 },
  choiceRow: { flexDirection: "row", alignItems: "center", gap: 8, backgroundColor: "#F3F4F6", padding: 8, borderRadius: 6 },
  choiceRowCorrect: { backgroundColor: "#EAF3DE" },
  letter: { width: 22, height: 22, borderRadius: 11, backgroundColor: "#fff", borderWidth: 1, borderColor: "#E5E7EB", alignItems: "center", justifyContent: "center" },
  letterCorrect: { backgroundColor: "#3B6D11", borderColor: "#3B6D11" },
  letterText: { fontSize: 11, fontWeight: "600", color: "#6B7280" },
  letterTextCorrect: { color: "#fff" },
  choiceText: { fontSize: 13, color: "#374151", flex: 1 },
  choiceTextCorrect: { color: "#3B6D11", fontWeight: "600" },
  empty: { flex: 1, justifyContent: "center", alignItems: "center" },
  emptyTitle: { fontSize: 18, fontWeight: "700", color: "#111827" },
  emptySub: { fontSize: 13, color: "#6B7280", marginTop: 6, textAlign: "center" },
});
