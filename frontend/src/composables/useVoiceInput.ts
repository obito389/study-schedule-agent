import { onBeforeUnmount, ref, type Ref } from "vue";

interface Recognition {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  onstart: (() => void) | null;
  onend: (() => void) | null;
  onerror: ((event: { error: string }) => void) | null;
  onresult:
    | ((event: {
        results: ArrayLike<{ isFinal: boolean; 0: { transcript: string } }>;
      }) => void)
    | null;
  start(): void;
  stop(): void;
  abort(): void;
}
type SpeechWindow = Window & {
  SpeechRecognition?: new () => Recognition;
  webkitSpeechRecognition?: new () => Recognition;
};

export function useVoiceInput(text: Ref<string>, disabled: Ref<boolean>) {
  const listening = ref(false);
  const processing = ref(false);
  const message = ref("长按输入区说话，松开结束；也可直接打字。");
  let timer: ReturnType<typeof setTimeout> | undefined;
  let recognition: Recognition | undefined;
  let held = false;
  let origin = { x: 0, y: 0 };
  let disposed = false;

  function start() {
    if (disabled.value || recognition) return;
    const browser = window as SpeechWindow;
    const Constructor =
      browser.SpeechRecognition ?? browser.webkitSpeechRecognition;
    if (!Constructor) {
      message.value =
        "当前浏览器不支持语音输入，请使用 Chrome / Edge，或继续打字。";
      return;
    }
    const base = text.value;
    const session = new Constructor();
    recognition = session;
    session.lang = "zh-CN";
    session.continuous = true;
    session.interimResults = true;
    processing.value = true;
    message.value = "正在开启麦克风，请允许录音权限…";
    session.onstart = () => {
      if (!held || disposed) {
        session.stop();
        return;
      }
      listening.value = true;
      processing.value = false;
      message.value = "正在听，请说话… 松开结束。";
    };
    session.onresult = (event) => {
      if (disposed) return;
      const transcript = Array.from(
        event.results,
        (result) => result[0].transcript,
      ).join("");
      text.value = (base + (base && transcript ? " " : "") + transcript).slice(
        0,
        1500,
      );
    };
    session.onerror = (event) => {
      const errors: Record<string, string> = {
        "not-allowed": "麦克风权限被拒绝，请在浏览器设置中允许后重试。",
        "service-not-allowed":
          "浏览器语音服务不可用，请换用 Chrome / Edge，或继续打字。",
        "audio-capture": "未找到可用麦克风，请检查设备连接。",
        network: "语音识别服务连接失败，请检查网络，或继续打字。",
        "no-speech": "没有听到声音，请按住输入区后再说话。",
        aborted: "语音输入已取消，可以继续打字。",
      };
      message.value = errors[event.error] ?? "语音识别失败，请重试或继续打字。";
      listening.value = false;
      processing.value = false;
    };
    session.onend = () => {
      recognition = undefined;
      listening.value = false;
      processing.value = false;
      if (message.value.startsWith("正在"))
        message.value = "语音输入已结束，可修改文字后生成建议。";
    };
    try {
      session.start();
    } catch {
      recognition = undefined;
      processing.value = false;
      message.value = "无法启动语音识别，请重试或继续打字。";
    }
  }
  function release() {
    clearTimeout(timer);
    held = false;
    if (recognition && (listening.value || processing.value)) {
      listening.value = false;
      processing.value = true;
      message.value = "正在完成语音识别…";
      try {
        recognition.stop();
      } catch {
        recognition.abort();
      }
    }
  }
  function press(event: PointerEvent) {
    if (event.button !== 0 || disabled.value || recognition) return;
    origin = { x: event.clientX, y: event.clientY };
    held = true;
    clearTimeout(timer);
    timer = setTimeout(start, 550);
  }
  function move(event: PointerEvent) {
    if (
      !recognition &&
      Math.hypot(event.clientX - origin.x, event.clientY - origin.y) > 12
    ) {
      clearTimeout(timer);
      held = false;
    }
  }
  function keydown(event: KeyboardEvent) {
    if (event.repeat || ![" ", "Enter"].includes(event.key)) return;
    event.preventDefault();
    held = true;
    start();
  }
  function keyup(event: KeyboardEvent) {
    if ([" ", "Enter"].includes(event.key)) {
      event.preventDefault();
      release();
    }
  }
  window.addEventListener("pointerup", release);
  window.addEventListener("pointercancel", release);
  window.addEventListener("blur", release);
  onBeforeUnmount(() => {
    disposed = true;
    clearTimeout(timer);
    window.removeEventListener("pointerup", release);
    window.removeEventListener("pointercancel", release);
    window.removeEventListener("blur", release);
    if (recognition) {
      recognition.onresult = null;
      recognition.onend = null;
      recognition.onerror = null;
      recognition.onstart = null;
      recognition.abort();
    }
  });
  return {
    listening,
    processing,
    message,
    press,
    move,
    keydown,
    keyup,
    release,
  };
}
