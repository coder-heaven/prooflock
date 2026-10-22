import http from "node:http";
import crypto from "node:crypto";

const port = Number(process.env.PORT || 8787);
const maxBody = 12 * 1024 * 1024;
const slotNames = ["habit_proof", "capture_quality", "activity_classification", "freshness_check", "tamper_check", "online_task_check", "screen_session_check", "consistency_check", "review_summary"];

const routes = Object.fromEntries(slotNames.flatMap((slot) => {
  const key = slot.toUpperCase().replaceAll("-", "_");
  return [
    [`${slot}:primary`, { url: process.env[`PROOFLOCK_AI_${key}_URL`] || "", model: process.env[`PROOFLOCK_AI_${key}_MODEL`] || "" }],
    [`${slot}:backup-1`, { url: process.env[`PROOFLOCK_AI_${key}_BACKUP_1_URL`] || "", model: process.env[`PROOFLOCK_AI_${key}_BACKUP_1_MODEL`] || "" }],
    [`${slot}:backup-2`, { url: process.env[`PROOFLOCK_AI_${key}_BACKUP_2_URL`] || "", model: process.env[`PROOFLOCK_AI_${key}_BACKUP_2_MODEL`] || "" }]
  ];
}));

function readJson(request) {
  return new Promise((resolve, reject) => {
    let size = 0; let body = "";
    request.on("data", (chunk) => { size += chunk.length; if (size > maxBody) reject(new Error("body_too_large")); else body += chunk; });
    request.on("end", () => { try { resolve(JSON.parse(body || "{}")); } catch { reject(new Error("invalid_json")); } });
    request.on("error", reject);
  });
}

async function callRoute(route, input) {
  if (!route.url || !route.model) throw new Error("route_unconfigured");
  const response = await fetch(route.url, { method: "POST", headers: { "content-type": "application/json", authorization: `Bearer ${process.env.PROOFLOCK_AI_TOKEN || ""}` }, body: JSON.stringify({ model: route.model, messages: [{ role: "user", content: [{ type: "text", text: `Check this ProofLock habit evidence. Habit: ${input.habitTitle}. Required proof: ${input.proofPrompt}. Return JSON with status pass, fail, or uncertain and a short reason.` }, { type: "image_url", image_url: { url: `data:image/jpeg;base64,${input.imageBase64}` } }] }], temperature: 0, response_format: { type: "json_object" } }) });
  if (!response.ok) throw new Error(`provider_${response.status}`);
  const payload = await response.json();
  const text = payload.choices?.[0]?.message?.content || "{}";
  const parsed = typeof text === "string" ? JSON.parse(text) : text;
  if (!["pass", "fail", "uncertain"].includes(parsed.status)) throw new Error("invalid_provider_result");
  return { status: parsed.status, reason: parsed.reason || "Provider returned a decision", route: route.model };
}

async function verify(input) {
  if (!input.habitTitle || !input.proofPrompt || !input.imageBase64) return { status: "fail", reason: "Missing habit or evidence", provider: "policy" };
  const attempts = ["primary", "backup-1", "backup-2"];
  const errors = [];
  for (const attempt of attempts) {
    try {
      const result = await callRoute(routes[`habit_proof:${attempt}`], input);
      return { ...result, provider: "prooflock-ai", attempts: attempts.slice(0, attempts.indexOf(attempt) + 1) };
    } catch (error) { errors.push(String(error.message)); }
  }
  return { status: "unavailable", reason: "All configured AI routes were unavailable", provider: "prooflock-ai", errors };
}

const server = http.createServer(async (request, response) => {
  response.setHeader("content-type", "application/json");
  response.setHeader("access-control-allow-origin", "*");
  if (request.method === "OPTIONS") { response.writeHead(204); return response.end(); }
  if (request.method === "GET" && request.url === "/health") { response.end(JSON.stringify({ ok: true, slots: slotNames.length, configuredRoutes: Object.values(routes).filter((route) => route.url && route.model).length })); return; }
  if (request.method === "POST" && request.url === "/v1/verify") {
    try { const result = await verify(await readJson(request)); response.end(JSON.stringify({ requestId: crypto.randomUUID(), ...result })); }
    catch (error) { response.writeHead(400); response.end(JSON.stringify({ status: "fail", reason: error.message })); }
    return;
  }
  response.writeHead(404); response.end(JSON.stringify({ error: "not_found" }));
});

server.listen(port, () => console.log(`ProofLock AI backend listening on ${port}`));
