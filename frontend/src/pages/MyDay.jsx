import { useEffect, useState } from 'react';
import { Sun, AlertTriangle, CalendarClock, Sparkles } from 'lucide-react';
import { plannerService, dashboardService, taskService } from '../api/services';
import { PriorityBadge, DueLabel, EmptyState, Spinner, CategoryBadge } from '../components/ui';
import TaskModal from '../components/TaskModal';

function TaskRow({ t, onEdit, onToggle }) {
  return (
    <div className="flex" style={{ padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
      <input type="checkbox" checked={t.status === 'COMPLETED'} onChange={() => onToggle(t)} style={{ width: 17, height: 17 }} />
      <div style={{ flex: 1, cursor: 'pointer' }} onClick={() => onEdit(t)}>
        <div style={{ fontWeight: 600, textDecoration: t.status === 'COMPLETED' ? 'line-through' : 'none' }}>{t.title}</div>
        <div className="flex" style={{ gap: 6 }}><CategoryBadge value={t.category} /><span className="small"><DueLabel task={t} /></span></div>
      </div>
      <PriorityBadge value={t.priority} />
    </div>
  );
}

export default function MyDay() {
  const [today, setToday] = useState([]);
  const [overdue, setOverdue] = useState([]);
  const [upcoming, setUpcoming] = useState([]);
  const [focus, setFocus] = useState(null);
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null);

  function load() {
    plannerService.today().then(r => { setToday(r.today); setOverdue(r.overdue); }).catch(e => setError(e.message));
    plannerService.upcoming(14).then(setUpcoming).catch(() => {});
    dashboardService.focus().then(setFocus).catch(() => {});
    dashboardService.summary().then(setSummary).catch(() => {});
  }
  useEffect(load, []);

  async function toggle(t) {
    try { await taskService.setStatus(t.id, t.status === 'COMPLETED' ? 'TODO' : 'COMPLETED'); load(); }
    catch (e) { setError(e.message); }
  }

  const doneToday = summary ? summary.completedTasks : 0;
  const goalProgress = summary && summary.totalTasks ? Math.round((doneToday / Math.max(summary.totalTasks, 1)) * 100) : 0;

  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      {focus && (
        <div className="card card-p" style={{ borderLeft: '4px solid var(--teal)' }}>
          <div className="small" style={{ fontWeight: 700, color: 'var(--teal)', textTransform: 'uppercase', letterSpacing: .5 }}>
            <Sparkles size={13} /> Focus task · score {focus.score}
          </div>
          <div style={{ fontSize: 18, fontWeight: 700, margin: '5px 0' }}>{focus.task.title}</div>
          <div className="muted small">{focus.reasons.join(' · ')}</div>
        </div>
      )}
      <div className="grid-2 mt">
        <div className="card card-p">
          <div className="section-title flex"><Sun size={18} style={{ color: 'var(--warning)' }} /> Due today ({today.length})</div>
          {today.length === 0 ? <EmptyState text="Nothing due today" sub="Enjoy the breathing room, or pull something forward." />
            : today.map(t => <TaskRow key={t.id} t={t} onEdit={setEditing} onToggle={toggle} />)}
          <div className="section-title flex mt"><CalendarClock size={18} style={{ color: 'var(--info)' }} /> Upcoming (14 days)</div>
          {upcoming.filter(t => !today.some(x => x.id === t.id)).slice(0, 8).map(t => <TaskRow key={t.id} t={t} onEdit={setEditing} onToggle={toggle} />)}
        </div>
        <div>
          <div className="card card-p">
            <div className="section-title flex"><AlertTriangle size={18} style={{ color: 'var(--error)' }} /> Overdue ({overdue.length})</div>
            {overdue.length === 0 ? <p className="muted small">No overdue tasks. Clean slate.</p>
              : overdue.map(t => <TaskRow key={t.id} t={t} onEdit={setEditing} onToggle={toggle} />)}
          </div>
          <div className="card card-p mt">
            <div className="section-title">Daily progress</div>
            <div className="muted small mb">{doneToday} task(s) completed overall · keep the streak going</div>
            <div className="progress"><div style={{ width: goalProgress + '%' }} /></div>
            <div className="small muted mt">{goalProgress}% of all tasks completed</div>
          </div>
        </div>
      </div>
      {editing && <TaskModal task={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); load(); }} />}
    </div>
  );
}
