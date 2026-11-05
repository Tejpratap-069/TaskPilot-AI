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
 * Talks to the Python NLP service. Python performs NLP only; all application
 * data stays in Spring Boot + MySQL. If the Python service is unreachable,
 * the Java LocalFallbackParser provides equivalent rule-based extraction.
 * Priority suggestions are never saved automatically - the user reviews and
 * approves every suggested task in the AI Task Capture screen.
 */
@Service
public class AiService {
    private final RestClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final LocalFallbackParser fallback;

    public AiService(@Value("${app.ai.url}") String aiUrl,
                     @Value("${app.ai.timeout-ms}") int timeoutMs,
                     LocalFallbackParser fallback) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.http = RestClient.builder().baseUrl(aiUrl).requestFactory(factory).build();
        this.fallback = fallback;
    }

    public ParseResponse parse(String text) {
        LocalDate today = LocalDate.now();
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
