import { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { taskService } from '../api/services';
import TaskModal from '../components/TaskModal';

// Calendar events are pastel-tinted by task category (dark ink text),
// matching the Tasks list and Kanban board. Unmapped categories fall back
// to a dark chip with light text.
const CAT_BG = { STUDY: '#7cc4ff', PROJECT: '#b7a6ff', PERSONAL: '#ff9de2', WORK: '#ffd66b', HEALTH: '#8be0a4', FINANCE: '#63d68a' };
const CAT_INK = '#23222e';
const FALLBACK_BG = '#2a2a35';
const FALLBACK_FG = '#c9c8d6';

const HOUR_H = 48;               // px per hour in the week grid
const DAY_START = 6;             // first visible hour (06:00)
const HOURS = Array.from({ length: 24 - DAY_START }, (_, i) => DAY_START + i); // 06..23
const DAY_NAMES = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

function dateKey(d) {
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
}
function mondayOf(d) {
  const x = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  x.setDate(x.getDate() - ((x.getDay() + 6) % 7)); // Monday-first
  return x;
}
function timeToMinutes(t) {
  if (!t) return null;
  const p = String(t).split(':');
  return Number(p[0]) * 60 + Number(p[1] || 0);
}

export default function CalendarPage() {
  const [cursor, setCursor] = useState(() => { const d = new Date(); return new Date(d.getFullYear(), d.getMonth(), 1); });
  const [view, setView] = useState('month'); // 'month' | 'week' (month is the default)
  const [weekStart, setWeekStart] = useState(() => mondayOf(new Date()));
  const [tasks, setTasks] = useState([]);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');

  function load() {
    taskService.list({ size: 200, sortBy: 'dueDate' }).then(p => setTasks((p.content || []).filter(t => t.dueDate))).catch(e => setError(e.message));
  }
  useEffect(load, []);

  const cells = useMemo(() => {
    const year = cursor.getFullYear(), month = cursor.getMonth();
    const first = new Date(year, month, 1);
    const startOffset = (first.getDay() + 6) % 7; // Monday-first
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const daysInPrev = new Date(year, month, 0).getDate();
    const out = [];
    for (let i = 0; i < 42; i++) {
      const dayNum = i - startOffset + 1;
      let date, dim = false;
      if (dayNum < 1) { date = new Date(year, month - 1, daysInPrev + dayNum); dim = true; }
      else if (dayNum > daysInMonth) { date = new Date(year, month + 1, dayNum - daysInMonth); dim = true; }
      else date = new Date(year, month, dayNum);
      const key = date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');
      out.push({ date, key, dim, day: date.getDate() });
    }
    return out;
  }, [cursor]);

  const todayKey = (() => { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); })();
  const byDate = useMemo(() => {
    const m = {};
    tasks.forEach(t => { (m[t.dueDate] = m[t.dueDate] || []).push(t); });
    return m;
  }, [tasks]);

  function move(delta) {
    if (view === 'week') {
      const d = new Date(weekStart); d.setDate(d.getDate() + delta * 7); setWeekStart(d);
    } else {
      setCursor(new Date(cursor.getFullYear(), cursor.getMonth() + delta, 1));
    }
  }
  function goToday() {
    if (view === 'week') setWeekStart(mondayOf(new Date()));
    else setCursor(new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  }

  const weekDays = useMemo(() => Array.from({ length: 7 }, (_, i) => {
    const d = new Date(weekStart); d.setDate(d.getDate() + i); return d;
  }), [weekStart]);
  const weekLabel = useMemo(() => {
    const s = weekDays[0], e = weekDays[6];
    const mon = (d) => d.toLocaleDateString(undefined, { month: 'short' });
    return s.getMonth() === e.getMonth()
      ? `${s.getDate()} – ${e.getDate()} ${mon(s)} ${s.getFullYear()}`
      : `${s.getDate()} ${mon(s)} – ${e.getDate()} ${mon(e)} ${e.getFullYear()}`;
  }, [weekDays]);

  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="flex wrap" style={{ justifyContent: 'space-between' }}>
        <div className="section-title" style={{ fontSize: 19 }}>
          {view === 'week' ? weekLabel : cursor.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}
        </div>
        <div className="flex">
          <div className="flex" style={{ gap: 6 }}>
            <button className={'btn btn-sm ' + (view === 'month' ? 'btn-primary' : 'btn-outline')} onClick={() => setView('month')}>Month</button>
            <button className={'btn btn-sm ' + (view === 'week' ? 'btn-primary' : 'btn-outline')} onClick={() => setView('week')}>Week</button>
          </div>
          <button className="btn btn-outline btn-sm" onClick={() => move(-1)}><ChevronLeft size={15} /> Prev</button>
          <button className="btn btn-outline btn-sm" onClick={goToday}>Today</button>
          <button className="btn btn-outline btn-sm" onClick={() => move(1)}>Next <ChevronRight size={15} /></button>
        </div>
      </div>
      {view === 'week' && (
        <div className="card week-card mt">
          <div className="week-scroll">
            <div className="week-grid">
              <div className="week-head">
                <div />
                {weekDays.map((d, i) => (
                  <div key={d.toISOString()} className={'week-day-head' + (dateKey(d) === todayKey ? ' today' : '')}>
                    <span>{DAY_NAMES[i]}</span><b>{d.getDate()}</b>
                  </div>
                ))}
              </div>
              <div className="week-allday">
                <div className="week-gutter-label">All day</div>
                {weekDays.map(d => {
                  const key = dateKey(d);
                  const items = (byDate[key] || []).filter(t => !t.dueTime);
                  return (
                    <div key={key} className="week-allday-cell">
                      {items.map(t => (
                        <span key={t.id} className="week-chip" data-category={t.category || 'OTHER'} style={{ opacity: t.status === 'COMPLETED' ? 0.55 : 1 }} onClick={() => setEditing(t)} title={t.title}>
                          {t.priority === 'URGENT' || t.priority === 'HIGH' ? '● ' : ''}{t.title}
                        </span>
                      ))}
                    </div>
                  );
                })}
              </div>
              <div className="week-body">
                <div className="week-gutter">
                  {HOURS.map(h => <div key={h} className="week-hour-label">{String(h).padStart(2, '0')}:00</div>)}
                </div>
                {weekDays.map(d => {
                  const key = dateKey(d);
                  const items = (byDate[key] || []).filter(t => t.dueTime);
                  return (
                    <div key={key} className={'week-col' + (key === todayKey ? ' today' : '')}>
                      {items.map(t => {
                        const startMin = timeToMinutes(t.dueTime);
                        const top = Math.min(Math.max(0, (startMin - DAY_START * 60) / 60 * HOUR_H), HOURS.length * HOUR_H - 30);
                        const dur = Math.max(t.estimatedMinutes || 60, 30);
                        const height = Math.min(dur / 60 * HOUR_H, HOURS.length * HOUR_H - top);
                        return (
                          <div key={t.id} className="week-block" data-category={t.category || 'OTHER'}
                            style={{ top, height, opacity: t.status === 'COMPLETED' ? 0.55 : 1 }}
                            onClick={() => setEditing(t)} title={t.title}>
                            <span className="week-block-time">{String(t.dueTime).slice(0, 5)}</span>
                            <span className="week-block-title">{t.title}</span>
                          </div>
                        );
                      })}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}
      {view === 'month' && (
      <div className="cal-grid mt">
        {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(d => <div key={d} className="cal-head">{d}</div>)}
        {cells.map(c => (
          <div key={c.key} className={'cal-cell' + (c.dim ? ' dim' : '') + (c.key === todayKey ? ' today' : '')}>
            <div className="d">{c.day}</div>
            {(byDate[c.key] || []).slice(0, 3).map(t => (
              <span key={t.id} className="cal-task" style={{ background: CAT_BG[t.category] || FALLBACK_BG, color: CAT_BG[t.category] ? CAT_INK : FALLBACK_FG, opacity: t.status === 'COMPLETED' ? 0.55 : 1 }} onClick={() => setEditing(t)} title={t.title}>
                {t.priority === 'URGENT' || t.priority === 'HIGH' ? '● ' : ''}{t.title}
              </span>
            ))}
            {(byDate[c.key] || []).length > 3 && <div className="small muted">+{(byDate[c.key]).length - 3} more</div>}
          </div>
        ))}
      </div>
      )}
      {editing && <TaskModal task={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); load(); }} />}
    </div>
  );
}
