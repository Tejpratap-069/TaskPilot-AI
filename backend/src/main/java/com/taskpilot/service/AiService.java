package com.taskpilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.dto.AiDtos.*;
import com.taskpilot.entity.Task;
import com.taskpilot.enums.Priority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Task extraction. Engine order:
 * <ol>
 *   <li>Gemini LLM — only when {@code GEMINI_API_KEY} is set and the Python
 *       service URL was not explicitly configured ({@code AI_SERVICE_URL}
 *       unset). Any Gemini error or timeout falls through to the next engine.</li>
 *   <li>The Python NLP service (rule-based). Python performs NLP only; all
 *       application data stays in Spring Boot + the database.</li>
 *   <li>The Java LocalFallbackParser, an equivalent rule-based extractor.</li>
 * </ol>
 * Extraction only ever returns suggestions — nothing is saved until the user
 * reviews and approves tasks in the AI Task Capture screen.
 */
@Service
public class AiService {
    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");
    private static final Set<String> CATEGORIES =
            Set.of("STUDY", "WORK", "PERSONAL", "HEALTH", "FINANCE", "PROJECT", "OTHER");
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";

    private final RestClient http;
    private final RestClient geminiHttp;
    private final ObjectMapper mapper = new ObjectMapper();
    private final LocalFallbackParser fallback;
    private final String geminiApiKey;
    private final String geminiModel;
    private final boolean pythonConfigured;

    public AiService(@Value("${app.ai.url}") String aiUrl,
                     @Value("${app.ai.timeout-ms}") int timeoutMs,
                     @Value("${app.ai.gemini-api-key:}") String geminiApiKey,
                     @Value("${app.ai.gemini-model:gemini-2.5-flash}") String geminiModel,
                     @Value("${app.ai.gemini-timeout-ms:20000}") int geminiTimeoutMs,
                     @Value("${app.ai.python-configured:}") String pythonServiceUrlEnv,
                     LocalFallbackParser fallback) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.http = RestClient.builder().baseUrl(aiUrl).requestFactory(factory).build();

        SimpleClientHttpRequestFactory geminiFactory = new SimpleClientHttpRequestFactory();
        geminiFactory.setConnectTimeout(Math.min(5_000, geminiTimeoutMs));
        geminiFactory.setReadTimeout(geminiTimeoutMs);
        this.geminiHttp = RestClient.builder().baseUrl(GEMINI_BASE_URL).requestFactory(geminiFactory).build();

        this.geminiApiKey = geminiApiKey == null ? "" : geminiApiKey.trim();
        this.geminiModel = geminiModel;
        this.pythonConfigured = pythonServiceUrlEnv != null && !pythonServiceUrlEnv.isBlank();
        this.fallback = fallback;
    }

    public ParseResponse parse(String text) {
        LocalDate today = LocalDate.now();
        if (!geminiApiKey.isEmpty() && !pythonConfigured) {
            ParseResponse gemini = parseWithGemini(text, today);
            if (gemini != null) return gemini;
            // Any Gemini failure falls through to the Python service / local fallback.
        }
        try {
            String body = http.post().uri("/parse")
                    .header("Content-Type", "application/json")
                    .body(mapper.writeValueAsString(Map.of("text", text, "referenceDate", today.toString())))
                    .retrieve().body(String.class);
            JsonNode root = mapper.readTree(body);
            List<Suggestion> tasks = new ArrayList<>();
            for (JsonNode n : root.path("tasks")) {
                tasks.add(new Suggestion(
                        n.path("title").asText(),
                        n.path("description").asText(null),
                        parseDate(n.path("dueDate")),
                        parseTime(n.path("dueTime")),
                        n.path("priority").asText("MEDIUM"),
                        n.path("category").asText("OTHER"),
                        n.hasNonNull("estimatedMinutes") ? n.get("estimatedMinutes").asInt() : null,
                        n.hasNonNull("dependsOn") ? n.get("dependsOn").asText() : null,
                        n.path("confidence").asDouble(0.8),
                        toStringList(n.path("warnings"))));
            }
            if (!tasks.isEmpty()) {
                return new ParseResponse(tasks, root.path("engine").asText("rule-based-nlp"), false);
            }
            return new ParseResponse(fallback.parse(text, today), "local-fallback", true);
        } catch (Exception e) {
            return new ParseResponse(fallback.parse(text, today), "local-fallback", true);
        }
    }

    /**
     * Calls the Gemini generateContent API and maps its JSON answer onto the
     * existing suggestion shape. Returns null on ANY problem (network, HTTP
     * error, timeout, malformed JSON, empty result) so the caller falls back
     * to the rule-based chain. The API key is sent as a header and is never
     * logged.
     */
    private ParseResponse parseWithGemini(String text, LocalDate today) {
        try {
            var payload = mapper.createObjectNode();
            var parts = payload.putArray("contents").addObject().putArray("parts");
            parts.addObject().put("text", geminiPrompt(text, today));
            var generationConfig = payload.putObject("generationConfig");
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.put("temperature", 0.2);

            String body = geminiHttp.post()
                    .uri("/v1beta/models/{model}:generateContent", geminiModel)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiApiKey)
                    .body(mapper.writeValueAsString(payload))
                    .retrieve().body(String.class);

            JsonNode root = mapper.readTree(body);
            String jsonText = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text").asText(null);
            if (jsonText == null || jsonText.isBlank()) return null;

            JsonNode parsed = mapper.readTree(stripCodeFences(jsonText));
            List<Suggestion> tasks = mapGeminiSuggestions(parsed.path("tasks"));
            if (tasks.isEmpty()) return null;
            return new ParseResponse(tasks, "gemini", false);
        } catch (Exception e) {
            return null;
        }
    }

    private String geminiPrompt(String text, LocalDate today) {
        return """
                You extract tasks from a user's free-text instruction for a task manager.
                Today is %s (%s). Resolve relative dates ("tomorrow", "next Friday", "in 3 days") against today.
                Respond with ONLY a JSON object, no markdown, no explanation, in exactly this shape:
                {"tasks":[{"title":"string","description":"string or null","dueDate":"YYYY-MM-DD or null","dueTime":"HH:mm or null","priority":"LOW|MEDIUM|HIGH|URGENT","category":"STUDY|WORK|PERSONAL|HEALTH|FINANCE|PROJECT|OTHER","estimatedMinutes":integer or null,"confidence":number between 0 and 1}]}
                Rules: one task per distinct action; keep titles short and imperative; if no date is stated use null; if no time is stated use null; default priority is MEDIUM; default category is OTHER.
                User instruction:
                %s""".formatted(today, today.getDayOfWeek(), text);
    }

    /** Defensive mapping: invalid items are skipped or repaired, never thrown on. */
    private List<Suggestion> mapGeminiSuggestions(JsonNode tasksNode) {
        List<Suggestion> out = new ArrayList<>();
        if (tasksNode == null || !tasksNode.isArray()) return out;
        for (JsonNode n : tasksNode) {
            String title = n.path("title").asText("").trim();
            if (title.isEmpty()) continue;
            String priority = n.path("priority").asText("MEDIUM").trim().toUpperCase();
            if (!PRIORITIES.contains(priority)) priority = "MEDIUM";
            String category = n.path("category").asText("OTHER").trim().toUpperCase();
            if (!CATEGORIES.contains(category)) category = "OTHER";
            double confidence = n.path("confidence").asDouble(0.7);
            confidence = Math.max(0.0, Math.min(1.0, confidence));
            out.add(new Suggestion(
                    title,
                    n.path("description").asText(null),
                    safeDate(n.path("dueDate")),
                    safeTime(n.path("dueTime")),
                    priority,
                    category,
                    n.hasNonNull("estimatedMinutes") ? n.get("estimatedMinutes").asInt() : null,
                    n.hasNonNull("dependsOn") ? n.get("dependsOn").asText() : null,
                    confidence,
                    toStringList(n.path("warnings"))));
        }
        return out;
    }

    private LocalDate safeDate(JsonNode n) {
        try {
            return parseDate(n);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime safeTime(JsonNode n) {
        try {
            return parseTime(n);
        } catch (Exception e) {
            return null;
        }
    }

    private String stripCodeFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline > 0) t = t.substring(firstNewline + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }

    public PrioritizeResponse prioritize(List<PrioritizeItem> items) {
        LocalDate today = LocalDate.now();
        try {
            List<Map<String, Object>> payload = new ArrayList<>();
            for (PrioritizeItem i : items) {
                Map<String, Object> m = new HashMap<>();
                m.put("title", i.title());
                m.put("dueDate", i.dueDate() != null ? i.dueDate().toString() : null);
                m.put("priority", i.priority());
                payload.add(m);
            }
            String body = http.post().uri("/prioritize")
                    .header("Content-Type", "application/json")
                    .body(mapper.writeValueAsString(Map.of("tasks", payload, "referenceDate", today.toString())))
                    .retrieve().body(String.class);
            JsonNode root = mapper.readTree(body);
            List<ScoredItem> out = new ArrayList<>();
            for (JsonNode n : root.path("tasks")) {
                out.add(new ScoredItem(n.path("title").asText(), parseDate(n.path("dueDate")),
                        n.path("priority").asText("MEDIUM"), n.path("score").asInt(),
                        toStringList(n.path("reasons"))));
            }
            if (!out.isEmpty()) return new PrioritizeResponse(out, "rule-based-nlp");
            throw new IllegalStateException("empty python response");
        } catch (Exception e) {
            List<ScoredItem> out = new ArrayList<>();
            for (PrioritizeItem i : items) {
                Priority p = safePriority(i.priority());
                Task probe = Task.builder().title(i.title()).priority(p).dueDate(i.dueDate()).build();
                ScoringUtil.Score s = ScoringUtil.score(probe, today);
                out.add(new ScoredItem(i.title(), i.dueDate(), p.name(), s.value(), s.reasons()));
            }
            out.sort(Comparator.comparingInt(ScoredItem::score).reversed());
            return new PrioritizeResponse(out, "local-fallback");
        }
    }

    private Priority safePriority(String p) {
        try { return Priority.valueOf(p); } catch (Exception e) { return Priority.MEDIUM; }
    }

    private LocalDate parseDate(JsonNode n) {
        return (n == null || n.isNull() || n.asText().isBlank()) ? null : LocalDate.parse(n.asText());
    }

    private LocalTime parseTime(JsonNode n) {
        if (n == null || n.isNull() || n.asText().isBlank()) return null;
        String v = n.asText();
        return v.length() == 5 ? LocalTime.parse(v) : LocalTime.parse(v.substring(0, 5));
    }

    private List<String> toStringList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) node.forEach(x -> out.add(x.asText()));
        return out;
    }
}
