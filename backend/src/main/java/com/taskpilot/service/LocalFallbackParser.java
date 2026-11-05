package com.taskpilot.service;

import com.taskpilot.dto.AiDtos.Suggestion;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local fallback task parser used when the Python NLP service is unreachable.
 * Implements the same core rules as the Python service (segmentation, relative
 * dates, weekday names, month dates, priority keywords) so AI Task Capture
 * keeps working offline. Labelled honestly as rule-based processing.
 */
@Component
public class LocalFallbackParser {

    private static final Map<String, Integer> MONTHS = new HashMap<>();
    private static final Map<String, Integer> WEEKDAYS = new HashMap<>();
    static {
        String[] m = {"january","february","march","april","may","june","july","august","september","october","november","december"};
        for (int i = 0; i < m.length; i++) { MONTHS.put(m[i], i + 1); MONTHS.put(m[i].substring(0, 3), i + 1); }
        MONTHS.put("sept", 9);
        String[] w = {"monday","tuesday","wednesday","thursday","friday","saturday","sunday"};
        for (int i = 0; i < w.length; i++) { WEEKDAYS.put(w[i], i + 1); WEEKDAYS.put(w[i].substring(0, 3), i + 1); }
    }

    public List<Suggestion> parse(String text, LocalDate ref) {
        List<Suggestion> out = new ArrayList<>();
        List<String> clauses = segment(text);
        List<String[]> boosts = new ArrayList<>();
        for (String clause : clauses) {
            String low = clause.toLowerCase();
            Matcher boost = Pattern.compile("^(.{2,40}?)\\s+is\\s+(the\\s+)?(most\\s+)?(urgent|important|critical)").matcher(low);
            if (boost.find() && !Pattern.compile("\\b(finish|complete|revise|submit|do|write|study|prepare|send|fix)\\b").matcher(low).find()) {
                boosts.add(new String[]{boost.group(1).trim(), "URGENT"});
                continue;
            }
            DateResult dr = extractDate(clause, ref);
            String cleaned = dr.cleaned;
            LocalTime time = null;
            Matcher tm = Pattern.compile("(?i)\\b(?:at|by)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b").matcher(cleaned);
            if (tm.find()) {
                int hour = Integer.parseInt(tm.group(1)) % 12 + ("pm".equalsIgnoreCase(tm.group(3)) ? 12 : 0);
                int min = tm.group(2) != null ? Integer.parseInt(tm.group(2)) : 0;
                time = LocalTime.of(hour, min);
                cleaned = (cleaned.substring(0, tm.start()) + " " + cleaned.substring(tm.end())).trim();
            }
            Integer minutes = null;
            Matcher dm = Pattern.compile("(?i)\\bfor\\s+(\\d+(?:\\.\\d+)?)\\s*hours?\\b").matcher(cleaned);
            if (dm.find()) { minutes = (int) (Double.parseDouble(dm.group(1)) * 60); cleaned = (cleaned.substring(0, dm.start()) + " " + cleaned.substring(dm.end())).trim(); }
            String priority = detectPriority(clause);
            String category = detectCategory(clause);
            String title = cleanTitle(cleaned);
            if (title.length() < 3) continue;
            List<String> warnings = new ArrayList<>();
            double confidence = 0.9;
            if (dr.date == null) { warnings.add("No deadline detected - please set a date before saving."); confidence = 0.55; }
            if (title.split("\\s+").length <= 2) confidence = Math.min(confidence, 0.7);
            out.add(new Suggestion(title, clause.trim(), dr.date, time, priority, category, minutes, null, confidence, warnings));
        }
        for (String[] b : boosts) {
            String keyword = b[0].substring(b[0].lastIndexOf(' ') + 1);
            for (int i = 0; i < out.size(); i++) {
                Suggestion s = out.get(i);
                if (s.title().toLowerCase().contains(keyword)) {
                    List<String> w = new ArrayList<>(s.warnings());
                    w.add("Priority raised because you marked it as the most urgent.");
                    out.set(i, new Suggestion(s.title(), s.description(), s.dueDate(), s.dueTime(), "URGENT",
                            s.category(), s.estimatedMinutes(), s.dependsOn(), s.confidence(), w));
                    break;
                }
            }
        }
        return out;
    }

    private record DateResult(LocalDate date, String cleaned) {}

    private DateResult extractDate(String text, LocalDate ref) {
        String t = " " + text + " ";
        String low = t.toLowerCase();
        // ISO
        Matcher m = Pattern.compile("\\b(20\\d{2})-(\\d{1,2})-(\\d{1,2})\\b").matcher(low);
        if (m.find()) {
            return new DateResult(LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))),
                    stripRange(t, m));
        }
        // month names
        String monAlt = String.join("|", MONTHS.keySet());
        m = Pattern.compile("\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+(" + monAlt + ")\\b").matcher(low);
        boolean swapped = false;
        if (!m.find()) {
            m = Pattern.compile("\\b(" + monAlt + ")\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b").matcher(low);
            swapped = true;
        }
        if (m.find()) {
            int day = Integer.parseInt(swapped ? m.group(2) : m.group(1));
            int month = MONTHS.get(swapped ? m.group(1) : m.group(2));
            LocalDate cand = LocalDate.of(ref.getYear(), month, day);
            if (cand.isBefore(ref)) cand = cand.plusYears(1);
            return new DateResult(cand, stripRange(t, m));
        }
        if (low.contains("day after tomorrow")) return new DateResult(ref.plusDays(2), t.replaceAll("(?i)day after tomorrow", " "));
        if (low.contains("tomorrow")) return new DateResult(ref.plusDays(1), t.replaceAll("(?i)tomorrow", " "));
        if (low.contains("tonight") || low.contains("today")) return new DateResult(ref, t.replaceAll("(?i)tonight|today", " "));
        m = Pattern.compile("\\bin\\s+(\\d+)\\s+days?\\b").matcher(low);
        if (m.find()) return new DateResult(ref.plusDays(Integer.parseInt(m.group(1))), stripRange(t, m));
        if (low.contains("next week")) return new DateResult(ref.plusDays(7), t.replaceAll("(?i)next week", " "));
        // weekday
        String wdAlt = String.join("|", WEEKDAYS.keySet());
        m = Pattern.compile("\\b(next|this|coming)?\\s*(?:on\\s+|by\\s+|before\\s+)?(" + wdAlt + ")\\b").matcher(low);
        if (m.find() && WEEKDAYS.containsKey(m.group(2))) {
            int target = WEEKDAYS.get(m.group(2));
            int delta = (target - ref.getDayOfWeek().getValue() + 7) % 7;
            if (delta == 0) delta = 7;
            return new DateResult(ref.plusDays(delta), stripRange(t, m));
        }
        String cleaned = t.trim().replaceAll("(?i)\\b(by|before|due on|due|on|until)$", "").trim();
        return new DateResult(null, cleaned);
    }

    private String stripRange(String t, Matcher m) {
        return (t.substring(0, m.start()) + " " + t.substring(m.end())).replaceAll("\\s+", " ").trim()
                .replaceAll("(?i)\\b(by|before|due on|due|on|until)$", "").trim();
    }

    private String detectPriority(String text) {
        String low = " " + text.toLowerCase() + " ";
        for (String w : new String[]{"urgent", "asap", "as soon as possible", "critical", "immediately", "most urgent", "top priority"})
            if (low.contains(w)) return "URGENT";
        for (String w : new String[]{"low priority", "whenever", "no rush", "minor", "someday"})
            if (low.contains(w)) return "LOW";
        for (String w : new String[]{"important", "high priority", "priority", "crucial"})
            if (low.contains(w)) return "HIGH";
        return "MEDIUM";
    }

    private String detectCategory(String text) {
        String low = " " + text.toLowerCase() + " ";
        Map<String, String[]> map = new LinkedHashMap<>();
        map.put("STUDY", new String[]{"study","revise","exam","homework","assignment","java","python","machine learning"," ml ","dsa","japanese","math","lecture","notes","quiz"});
        map.put("PROJECT", new String[]{"project","prototype","hackathon","demo","presentation","report","deploy","build","develop"});
        map.put("WORK", new String[]{"meeting","email","call","interview","office","client","team"});
        map.put("HEALTH", new String[]{"gym","workout","exercise","run","yoga","doctor","medicine","walk"});
        map.put("FINANCE", new String[]{"pay","bill","rent","invoice","bank","fee","budget","tax"});
        map.put("PERSONAL", new String[]{"buy","shop","clean","cook","laundry","birthday","travel","family"});
        for (var e : map.entrySet()) for (String w : e.getValue()) if (low.contains(w)) return e.getKey();
        return "OTHER";
    }

    private String cleanTitle(String text) {
        String t = text.trim().replaceAll("[,.;]+$", "");
        t = t.replaceAll("(?i)^(i have to|i need to|i must|i should|i want to|i will|please|remember to|also|and)\\s+", "");
        t = t.replaceAll("(?i)\\b(very\\s+)?(urgent|important|high priority|low priority)\\b", "");
        t = t.replaceAll("\\s+", " ").trim().replaceAll("[,.;]+$", "").replaceAll("(?i)\\s+(by|before|on|due|until|at)$", "").trim();
        if (!t.isEmpty()) t = Character.toUpperCase(t.charAt(0)) + t.substring(1);
        return t;
    }

    private List<String> segment(String text) {
        List<String> out = new ArrayList<>();
        for (String part : text.split("[;\\n]+")) {
            for (String sent : part.split("(?<=[.!?])\\s+")) {
                for (String s : sent.split(",\\s*(?:and\\s+)?|\\s+and\\s+")) {
                    String clean = s.trim().replaceAll("^[,.\"]+|[,.\"]+$", "");
                    if (clean.length() >= 3) out.add(clean);
                }
            }
        }
        return out;
    }

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
}
