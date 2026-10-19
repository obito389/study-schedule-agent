// Deterministic local provider for browser tests only. Production never uses it.
import { createServer } from "node:http";
createServer(async (req, res) => {
  if (req.method === "GET") {
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end('{"status":"test-provider"}');
    return;
  }
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  const payload = JSON.parse(Buffer.concat(chunks).toString("utf8"));
  const context = JSON.parse(payload.messages[1].content);
  if (context.instruction.includes("失败")) {
    res.writeHead(503);
    res.end("test outage");
    return;
  }
  const start = new Date(
    Math.ceil((Date.parse(context.snapshot.now) + 86400000) / 1800000) *
      1800000,
  );
  const occupied = [...context.snapshot.tasks.flatMap(t=>t.scheduledBlocks), ...context.snapshot.courses.flatMap(c=>c.meetingBlocks), ...context.snapshot.lockedBlocks]; while(occupied.some(b=>Date.parse(b.start)<start.getTime()+3600000 && Date.parse(b.end)>start.getTime())) start.setTime(start.getTime()+3600000); const end = new Date(start.getTime() + 3600000);
  const response = context.instruction.includes("缺少")
    ? {
        type: null,
        targets: [],
        changes: {},
        reasonFacts: [],
        ambiguities: ["请补充预计耗时和截止时间"],
      }
    : {
        type: "CREATE_TASK",
        targets: [],
        changes: {
          title: "AI浏览器验收任务",
          deadline: end.toISOString(),
          durationMinutes: 60,
          priority: 4,
          start: start.toISOString(),
          end: end.toISOString(),
        },
        reasonFacts: ["测试提供方解析了自然语言中的时段和耗时"],
        ambiguities: [],
      };
  res.writeHead(200, { "Content-Type": "application/json" });
  res.end(
    JSON.stringify({
      choices: [
        {
          finish_reason: "stop",
          message: { content: JSON.stringify(response) },
        },
      ],
    }),
  );
}).listen(8090, "127.0.0.1");


