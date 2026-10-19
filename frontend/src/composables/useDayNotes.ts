import { onMounted, onBeforeUnmount, ref } from "vue";

const prefix = "study-planner:day-note:";
const datePattern = /^\d{4}-\d{2}-\d{2}$/;

export function useDayNotes() {
  const notes = ref<Record<string, string>>({});
  const error = ref("");
  function load() {
    try {
      const values: Record<string, string> = {};
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (!key?.startsWith(prefix)) continue;
        const day = key.slice(prefix.length);
        const text = localStorage.getItem(key)?.trim();
        if (datePattern.test(day) && text) values[day] = text;
      }
      notes.value = values;
      error.value = "";
    } catch {
      error.value = "无法读取此浏览器保存的重要事项，请允许站点存储后重试。";
    }
  }
  function save(day: string, text: string): boolean {
    if (!datePattern.test(day) || text.length > 1000) {
      error.value = "日期无效或内容超过 1000 字，请检查后重试。";
      return false;
    }
    try {
      const value = text.trim();
      if (value) localStorage.setItem(prefix + day, value);
      else localStorage.removeItem(prefix + day);
      const values = { ...notes.value };
      if (value) values[day] = value;
      else delete values[day];
      notes.value = values;
      error.value = "";
      return true;
    } catch {
      error.value =
        "保存失败，内容仍保留在输入框中。请允许站点存储或释放空间后重试。";
      return false;
    }
  }
  function onStorage(event: StorageEvent) {
    if (
      event.storageArea === localStorage &&
      (event.key === null || event.key.startsWith(prefix))
    )
      load();
  }
  onMounted(() => {
    load();
    window.addEventListener("storage", onStorage);
  });
  onBeforeUnmount(() => window.removeEventListener("storage", onStorage));
  return { notes, error, save };
}
