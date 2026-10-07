import { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  Pressable,
  Modal,
  FlatList,
} from "react-native";

export interface SelectOption<T> {
  label: string;
  value: T;
}

interface SelectProps<T> {
  options: SelectOption<T>[];
  value: T | null;
  onChange: (value: T) => void;
  placeholder?: string;
  title?: string;
  disabled?: boolean;
}

export function Select<T extends string | number>({
  options,
  value,
  onChange,
  placeholder = "Select an option",
  title = "Select",
  disabled = false,
}: SelectProps<T>) {
  const [open, setOpen] = useState(false);

  const selected = options.find((o) => o.value === value);

  const handlePick = (val: T) => {
    onChange(val);
    setOpen(false);
  };

  return (
    <>
      <Pressable
        style={[styles.field, disabled && styles.fieldDisabled]}
        onPress={() => !disabled && setOpen(true)}
      >
        <Text style={[styles.fieldText, !selected && styles.placeholder]} numberOfLines={1}>
          {selected ? selected.label : placeholder}
        </Text>
        <Text style={styles.chevron}>⌄</Text>
      </Pressable>

      <Modal
        visible={open}
        transparent
        animationType="slide"
        onRequestClose={() => setOpen(false)}
      >
        <Pressable style={styles.backdrop} onPress={() => setOpen(false)}>
          <Pressable style={styles.sheet} onPress={(e) => e.stopPropagation()}>
            <View style={styles.handle} />
            <Text style={styles.sheetTitle}>{title}</Text>

            <FlatList
              data={options}
              keyExtractor={(item) => String(item.value)}
              style={styles.list}
              renderItem={({ item }) => {
                const isSelected = item.value === value;
                return (
                  <Pressable
                    style={[styles.option, isSelected && styles.optionSelected]}
                    onPress={() => handlePick(item.value)}
                  >
                    <Text style={[styles.optionText, isSelected && styles.optionTextSelected]}>
                      {item.label}
                    </Text>
                    {isSelected && <Text style={styles.check}>✓</Text>}
                  </Pressable>
                );
              }}
            />
          </Pressable>
        </Pressable>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  field: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#fff",
    borderWidth: 1,
    borderColor: "#E5E7EB",
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 14,
    marginBottom: 8,
  },
  fieldDisabled: { opacity: 0.5 },
  fieldText: { flex: 1, fontSize: 15, color: "#111827" },
  placeholder: { color: "#9CA3AF" },
  chevron: { fontSize: 18, color: "#9CA3AF", marginLeft: 8 },
  backdrop: { flex: 1, backgroundColor: "rgba(0,0,0,0.4)", justifyContent: "flex-end" },
  sheet: {
    backgroundColor: "#fff",
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 28,
    maxHeight: "70%",
  },
  handle: { alignSelf: "center", width: 40, height: 4, borderRadius: 2, backgroundColor: "#E5E7EB", marginBottom: 12 },
  sheetTitle: { fontSize: 16, fontWeight: "700", color: "#111827", marginBottom: 8 },
  list: { flexGrow: 0 },
  option: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 14,
    paddingHorizontal: 12,
    borderRadius: 10,
  },
  optionSelected: { backgroundColor: "#EEEDFE" },
  optionText: { flex: 1, fontSize: 15, color: "#374151" },
  optionTextSelected: { color: "#534AB7", fontWeight: "700" },
  check: { fontSize: 16, color: "#534AB7", fontWeight: "700" },
});
