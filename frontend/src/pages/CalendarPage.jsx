import { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { taskService } from '../api/services';
import TaskModal from '../components/TaskModal';

const STATUS_BG = { TODO: '#EDF1F4', IN_PROGRESS: '#E8EFF6', COMPLETED: '#E5F0EA', CANCELLED: '#F3E3E3' };
const STATUS_FG = { TODO: '#526579', IN_PROGRESS: '#416C9A', COMPLETED: '#38765B', CANCELLED: '#B54747' };

export default function CalendarPage() {
  const [cursor, setCursor] = useState(() => { const d = new Date(); return new Date(d.getFullYear(), d.getMonth(), 1); });
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

  function move(delta) { setCursor(new Date(cursor.getFullYear(), cursor.getMonth() + delta, 1)); }

  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="flex" style={{ justifyContent: 'space-between' }}>
        <div className="section-title" style={{ fontSize: 19 }}>
          {cursor.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}
        </div>
        <div className="flex">
          <button className="btn btn-outline btn-sm" onClick={() => move(-1)}><ChevronLeft size={15} /> Prev</button>
          <button className="btn btn-outline btn-sm" onClick={() => setCursor(new Date(new Date().getFullYear(), new Date().getMonth(), 1))}>Today</button>
          <button className="btn btn-outline btn-sm" onClick={() => move(1)}>Next <ChevronRight size={15} /></button>
        </div>
      </div>
      <div className="cal-grid mt">
        {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(d => <div key={d} className="cal-head">{d}</div>)}
        {cells.map(c => (
          <div key={c.key} className={'cal-cell' + (c.dim ? ' dim' : '') + (c.key === todayKey ? ' today' : '')}>
            <div className="d">{c.day}</div>
            {(byDate[c.key] || []).slice(0, 3).map(t => (
              <span key={t.id} className="cal-task" style={{ background: STATUS_BG[t.status], color: STATUS_FG[t.status] }} onClick={() => setEditing(t)} title={t.title}>
                {t.priority === 'URGENT' || t.priority === 'HIGH' ? '● ' : ''}{t.title}
              </span>
            ))}
            {(byDate[c.key] || []).length > 3 && <div className="small muted">+{(byDate[c.key]).length - 3} more</div>}
          </div>
        ))}
      </div>
      {editing && <TaskModal task={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); load(); }} />}
    </div>
  );
}
