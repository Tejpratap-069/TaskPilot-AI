"""
TaskPilot AI - Python NLP service (supporting service only).

Rule-based natural-language task extraction. This is NLP / rule-based
processing (date parsing, segmentation, keyword classification, priority
scoring). It is NOT a trained machine-learning model and is labelled as such.

Zero third-party dependencies: runs on the Python standard library alone.

Run:
    python nlp_service.py            (listens on port 8000)
    PORT=8001 python nlp_service.py  (custom port)

Endpoints:
    GET  /health
    POST /parse       {"text": "...", "referenceDate": "2026-10-31"}
    POST /prioritize  {"tasks": [{"title": "...", "dueDate": "2026-11-02", "priority": "HIGH"}]}
"""

import json
import os
import re
from datetime import date, datetime, time, timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

# ---------------------------------------------------------------------------
# Vocabulary
# ---------------------------------------------------------------------------

MONTHS = {
    "january": 1, "jan": 1, "february": 2, "feb": 2, "march": 3, "mar": 3,
    "april": 4, "apr": 4, "may": 5, "june": 6, "jun": 6, "july": 7, "jul": 7,
    "august": 8, "aug": 8, "september": 9, "sep": 9, "sept": 9,
    "october": 10, "oct": 10, "november": 11, "nov": 11, "december": 12, "dec": 12,
}

WEEKDAYS = {
    "monday": 0, "mon": 0, "tuesday": 1, "tue": 1, "tues": 1,
    "wednesday": 2, "wed": 2, "thursday": 3, "thu": 3, "thur": 3, "thurs": 3,
    "friday": 4, "fri": 4, "saturday": 5, "sat": 5, "sunday": 6, "sun": 6,
}

URGENT_WORDS = ["urgent", "asap", "as soon as possible", "critical", "immediately",
                "emergency", "most urgent", "top priority", "highest priority"]
HIGH_WORDS = ["important", "high priority", "priority", "must", "deadline", "crucial"]
LOW_WORDS = ["low priority", "whenever", "no rush", "minor", "someday", "if possible"]

CATEGORY_KEYWORDS = {
    "STUDY": ["study", "revise", "revision", "learn", "exam", "lecture", "course",
              "homework", "assignment", "java", "python", "machine learning", " ml ",
              "dsa", "algorithm", "japanese", "maths", "math", "physics", "chemistry",
              "dbms", "operating system", " os ", "quiz", "notes", "chapter", "syllabus"],
    "PROJECT": ["project", "prototype", "hackathon", "demo", "presentation", "ppt",
                "report", "documentation", "deploy", "build", "develop"],
    "WORK": ["meeting", "email", "call", "interview", "office", "client", "boss",
             "team", "standup", "review meeting"],
    "HEALTH": ["gym", "workout", "exercise", "run", "jog", "yoga", "doctor",
               "medicine", "workout", "walk", "cycling", "swim"],
    "FINANCE": ["pay", "bill", "rent", "invoice", "bank", "fee", "fees", "budget",
                "subscription", "tax"],
    "PERSONAL": ["buy", "shop", "clean", "cook", "laundry", "birthday", "book tickets",
                 "travel", "family", "friend"],
}

LEADING_FILLERS = [
    r"^i have to\s+", r"^i need to\s+", r"^i must\s+", r"^i should\s+",
    r"^i want to\s+", r"^i will\s+", r"^please\s+", r"^remember to\s+",
    r"^don'?t forget to\s+", r"^also\s+", r"^and\s+",
]

# ---------------------------------------------------------------------------
# Date extraction
# ---------------------------------------------------------------------------

def _next_weekday(ref: date, weekday: int, strict_next: bool) -> date:
    delta = (weekday - ref.weekday()) % 7
    if delta == 0:
        delta = 7 if strict_next else 0
    return ref + timedelta(days=delta)


def extract_date(text: str, ref: date):
    """Return (due_date or None, cleaned_text, warning or None)."""
    t = " " + text.lower() + " "
    found = None

    # ISO date 2026-11-05
    m = re.search(r"\b(20\d{2})-(\d{1,2})-(\d{1,2})\b", t)
    if m:
        try:
            found = date(int(m.group(1)), int(m.group(2)), int(m.group(3)))
        except ValueError:
            found = None
        t = t[:m.start()] + " " + t[m.end():]

    # dd/mm or dd/mm/yyyy
    if not found:
        m = re.search(r"\b(\d{1,2})/(\d{1,2})(?:/(20\d{2}))?\b", t)
        if m:
            day, month = int(m.group(1)), int(m.group(2))
            year = int(m.group(3)) if m.group(3) else ref.year
            try:
                cand = date(year, month, day)
                if not m.group(3) and cand < ref:
                    cand = date(year + 1, month, day)
                found = cand
            except ValueError:
                found = None
            t = t[:m.start()] + " " + t[m.end():]

    # "5 november" / "november 5" / "nov 5th"
    if not found:
        mon_alt = "|".join(MONTHS.keys())
        m = re.search(r"\b(\d{1,2})(?:st|nd|rd|th)?\s+(" + mon_alt + r")\b", t)
        if not m:
            m2 = re.search(r"\b(" + mon_alt + r")\s+(\d{1,2})(?:st|nd|rd|th)?\b", t)
            if m2:
                m = _SwapMatch(m2)
        if m:
            day = int(m.group(1)); month = MONTHS[m.group(2)]
            try:
                cand = date(ref.year, month, day)
                if cand < ref:
                    cand = date(ref.year + 1, month, day)
                found = cand
            except ValueError:
                found = None
            t = t[:m.start()] + " " + t[m.end():]

    # Relative phrases
    if not found:
        if re.search(r"\bday after tomorrow\b", t):
            found = ref + timedelta(days=2)
            t = re.sub(r"\bday after tomorrow\b", " ", t)
        elif re.search(r"\btomorrow\b", t):
            found = ref + timedelta(days=1)
            t = re.sub(r"\btomorrow\b", " ", t)
        elif re.search(r"\btonight\b", t):
            found = ref
            t = re.sub(r"\btonight\b", " ", t)
        elif re.search(r"\btoday\b", t):
            found = ref
            t = re.sub(r"\btoday\b", " ", t)

    if not found:
        m = re.search(r"\bin\s+(\d+)\s+days?\b", t)
        if m:
            found = ref + timedelta(days=int(m.group(1)))
            t = t[:m.start()] + " " + t[m.end():]

    if not found:
        m = re.search(r"\bnext week\b", t)
        if m:
            found = ref + timedelta(days=7)
            t = t[:m.start()] + " " + t[m.end():]

    # Weekday names: "on sunday", "before monday", "next friday", "by friday"
    if not found:
        wd_alt = "|".join(sorted(WEEKDAYS.keys(), key=len, reverse=True))
        m = re.search(r"\b(next|this|coming)?\s*(?:on\s+|by\s+|before\s+)?(" + wd_alt + r")\b", t)
        if m and m.group(2) in WEEKDAYS:
            strict = (m.group(1) == "next")
            cand = _next_weekday(ref, WEEKDAYS[m.group(2)], strict)
            if cand == ref and not strict:
                cand = ref + timedelta(days=7)  # "on Sunday" said on Sunday means next one
            found = cand
            t = t[:m.start()] + " " + t[m.end():]

    cleaned = re.sub(r"\s+", " ", t).strip(" ,.;")
    # Remove dangling temporal prepositions left behind ("by", "before", "on", "due")
    cleaned = re.sub(r"\b(by|before|due on|due|on|until)$", "", cleaned).strip(" ,.;")
    warning = None
    if not found:
        warning = "No deadline detected - please set a date before saving."
    return found, cleaned, warning


class _SwapMatch:
    """Adapt group order for the 'month day' pattern to (day, month)."""
    def __init__(self, m):
        self._m = m
    def group(self, i):
        if i == 1:
            return self._m.group(2)
        if i == 2:
            return self._m.group(1)
        return self._m.group(i)
    def start(self):
        return self._m.start()
    def end(self):
        return self._m.end()


def extract_time(text: str):
    """Extract 'at 5 pm' / 'by 6:30 pm' style time. Returns (time or None, cleaned)."""
    t = " " + text + " "
    m = re.search(r"\b(?:at|by)\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b", t, re.I)
    if m:
        hour = int(m.group(1)) % 12
        if m.group(3).lower() == "pm":
            hour += 12
        minute = int(m.group(2)) if m.group(2) else 0
        return time(hour, minute), (t[:m.start()] + " " + t[m.end():]).strip()
    m = re.search(r"\b(\d{1,2}):(\d{2})\b", t)
    if m:
        return time(int(m.group(1)), int(m.group(2))), (t[:m.start()] + " " + t[m.end():]).strip()
    return None, text


def extract_duration(text: str):
    """Extract 'for 2 hours', '30 minutes', '1.5 hours'. Returns (minutes, cleaned)."""
    t = " " + text + " "
    m = re.search(r"\bfor\s+(\d+(?:\.\d+)?)\s*hours?\b", t, re.I)
    if m:
        return int(float(m.group(1)) * 60), (t[:m.start()] + " " + t[m.end():]).strip()
    m = re.search(r"\bfor\s+(\d+)\s*(?:minutes?|mins?)\b", t, re.I)
    if m:
        return int(m.group(1)), (t[:m.start()] + " " + t[m.end():]).strip()
    m = re.search(r"\b(\d+)\s*(?:minutes?|mins?)\b", t, re.I)
    if m:
        return int(m.group(1)), (t[:m.start()] + " " + t[m.end():]).strip()
    return None, text


def detect_priority(text: str):
    low = " " + text.lower() + " "
    for w in URGENT_WORDS:
        if w in low:
            return "URGENT"
    for w in LOW_WORDS:
        if w in low:
            return "LOW"
    for w in HIGH_WORDS:
        if w in low:
            return "HIGH"
    return "MEDIUM"


def detect_category(text: str):
    low = " " + text.lower() + " "
    for cat, words in CATEGORY_KEYWORDS.items():
        for w in words:
            if w in low:
                return cat
    return "OTHER"


def clean_title(text: str) -> str:
    t = text.strip(" ,.;")
    low = t.lower()
    for pat in LEADING_FILLERS:
        low_new = re.sub(pat, "", low)
        if low_new != low:
            t = t[len(t) - len(low_new):] if False else t  # fall through to regex on original
            t = re.sub(pat, "", t, flags=re.I)
            low = t.lower()
    t = re.sub(r"\s+", " ", t).strip(" ,.;")
    # Strip leftover priority phrases from the title
    t = re.sub(r"\b(it'?s|this is|that is)?\s*(very\s+)?(urgent|important|high priority|low priority)\b", "", t, flags=re.I)
    t = re.sub(r"\s+", " ", t).strip(" ,.;")
    t = re.sub(r"\s+(by|before|on|due|until|at)$", "", t, flags=re.I).strip(" ,.;")
    if t:
        t = t[0].upper() + t[1:]
    return t


def segment(text: str):
    """Split one instruction into task clauses (sentences first, then commas / 'and')."""
    parts = re.split(r"[;\n]+", text)
    clauses = []
    for p in parts:
        sentences = re.split(r"(?<=[.!?])\s+", p)
        for sent in sentences:
            sub = re.split(r",\s*(?:and\s+)?|\s+and\s+", sent)
            for s in sub:
                s = s.strip(" ,.")
                if len(s) >= 3:
                    clauses.append(s)
    return clauses


def parse_instruction(text: str, ref: date):
    clauses = segment(text)
    tasks = []
    boost_targets = []  # (keyword, priority) from clauses like "Java is the most urgent"

    for clause in clauses:
        low = clause.lower()
        # Comparative boost clause: "java is the most urgent" / "ml is most important"
        mb = re.match(r"^(.{2,40}?)\s+is\s+(the\s+)?(most\s+)?(urgent|important|critical|top priority)\b", low)
        if mb and not re.search(r"\b(finish|complete|revise|submit|do|write|study|prepare|send|fix)\b", low):
            boost_targets.append((mb.group(1).strip(), "URGENT"))
            continue

        due, cleaned, warning = extract_date(clause, ref)
        due_time, cleaned = extract_time(cleaned)
        minutes, cleaned = extract_duration(cleaned)
        depends = None
        md = re.search(r"\bafter\s+(?:finishing\s+|completing\s+)?(.{3,60})$", cleaned, re.I)
        if md:
            depends = md.group(1).strip(" ,.")
            cleaned = cleaned[:md.start()].strip(" ,.")
        priority = detect_priority(clause)
        category = detect_category(clause)
        title = clean_title(cleaned)
        if not title or len(title) < 3:
            continue
        confidence = 0.95
        if not due:
            confidence = 0.6
        if len(title.split()) <= 2:
            confidence = min(confidence, 0.75)
        task = {
            "title": title,
            "description": clause.strip(),
            "dueDate": due.isoformat() if due else None,
            "dueTime": due_time.strftime("%H:%M") if due_time else None,
            "priority": priority,
            "category": category,
            "estimatedMinutes": minutes,
            "dependsOn": depends,
            "confidence": round(confidence, 2),
            "warnings": [warning] if warning else [],
        }
        tasks.append(task)

    # Apply comparative boosts ("Java is the most urgent")
    for keyword, prio in boost_targets:
        for t in tasks:
            if keyword and keyword.split()[-1] in t["title"].lower():
                t["priority"] = prio
                t["warnings"].append("Priority raised because you marked it as the most urgent.")
                break

    return tasks


def prioritize(tasks, ref: date):
    """Rank tasks by deadline proximity + priority weight. Explainable scoring."""
    weights = {"URGENT": 40, "HIGH": 30, "MEDIUM": 20, "LOW": 10}
    scored = []
    for t in tasks:
        score = weights.get(str(t.get("priority", "MEDIUM")).upper(), 20)
        reasons = [f"Priority {t.get('priority', 'MEDIUM')} (+{score})"]
        due_raw = t.get("dueDate")
        if due_raw:
            try:
                due = datetime.fromisoformat(str(due_raw)).date() if "T" in str(due_raw) else date.fromisoformat(str(due_raw))
                days = (due - ref).days
                if days < 0:
                    score += 50; reasons.append(f"Overdue by {-days} day(s) (+50)")
                elif days == 0:
                    score += 45; reasons.append("Due today (+45)")
                elif days == 1:
                    score += 35; reasons.append("Due tomorrow (+35)")
                elif days <= 3:
                    score += 25; reasons.append(f"Due in {days} days (+25)")
                elif days <= 7:
                    score += 15; reasons.append(f"Due in {days} days (+15)")
                else:
                    score += 5; reasons.append(f"Due in {days} days (+5)")
            except ValueError:
                reasons.append("Unparseable due date (+0)")
        else:
            reasons.append("No due date (+0)")
        scored.append({**t, "score": score, "reasons": reasons})
    scored.sort(key=lambda x: -x["score"])
    return scored


# ---------------------------------------------------------------------------
# HTTP layer
# ---------------------------------------------------------------------------

class Handler(BaseHTTPRequestHandler):
    # HTTP/1.1 with explicit Content-Length keeps strict clients
    # (e.g. Java's HttpURLConnection) happy; HTTP/1.0 default can be
    # rejected by them as an invalid response.
    protocol_version = "HTTP/1.1"

    def _send(self, code, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read_json(self):
        length = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(length) if length else b"{}"
        try:
            return json.loads(raw.decode("utf-8"))
        except (ValueError, UnicodeDecodeError):
            return None

    def do_GET(self):
        if self.path == "/health":
            self._send(200, {"status": "ok", "engine": "rule-based-nlp"})
        else:
            self._send(404, {"error": "not found"})

    def do_POST(self):
        data = self._read_json()
        if data is None:
            self._send(400, {"error": "invalid JSON body"})
            return
        ref = date.today()
        if data.get("referenceDate"):
            try:
                ref = date.fromisoformat(str(data["referenceDate"]))
            except ValueError:
                self._send(400, {"error": "referenceDate must be YYYY-MM-DD"})
                return
        if self.path == "/parse":
            text = str(data.get("text", "")).strip()
            if not text:
                self._send(400, {"error": "text is required"})
                return
            if len(text) > 5000:
                self._send(400, {"error": "text too long (max 5000 chars)"})
                return
            tasks = parse_instruction(text, ref)
            if not tasks:
                self._send(422, {"error": "No tasks could be extracted from the instruction.",
                                 "tasks": [], "engine": "rule-based-nlp"})
                return
            self._send(200, {"tasks": tasks, "engine": "rule-based-nlp",
                             "referenceDate": ref.isoformat()})
        elif self.path == "/prioritize":
            tasks = data.get("tasks")
            if not isinstance(tasks, list):
                self._send(400, {"error": "tasks must be a list"})
                return
            self._send(200, {"tasks": prioritize(tasks, ref), "engine": "rule-based-nlp"})
        else:
            self._send(404, {"error": "not found"})

    def log_message(self, fmt, *args):  # keep console clean
        pass


if __name__ == "__main__":
    port = int(os.environ.get("PORT", "8000"))
    server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    print(f"TaskPilot NLP service (rule-based) listening on port {port}")
    server.serve_forever()
