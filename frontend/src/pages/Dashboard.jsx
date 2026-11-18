import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { CheckCircle2, Clock, AlertTriangle, CalendarClock, Flame, ListTodo, TrendingUp, Sparkles, Plus } from 'lucide-react';
import { dashboardService } from '../api/services';
import { useAuth } from '../context/AuthContext';
import { PriorityBadge, StatusBadge, DueLabel, EmptyState, Spinner, timeAgo, CategoryBadge } from '../components/ui';
import TaskModal from '../components/TaskModal';
import Insights from '../components/Insights';

const PIE_COLORS = { TODO: '#8a87a0', IN_PROGRESS: '#7cc4ff', COMPLETED: '#4cc38a', CANCELLED: '#ff7a7a' };

export default function Dashboard() {
  const { user } = useAuth();
  const [summary, setSummary] = useState(null);
  const [weekly, setWeekly] = useState([]);
  const [upcoming, setUpcoming] = useState([]);
  const [breakdown, setBreakdown] = useState(null);
  const [focus, setFocus] = useState(null);
  const [activity, setActivity] = useState([]);
  const [error, setError] = useState('');
  const [showNew, setShowNew] = useState(false);
  const [editing, setEditing] = useState(null);

  function load() {
    Promise.all([
      dashboardService.summary(), dashboardService.weekly(), dashboardService.upcoming(7),
      dashboardService.breakdown(), dashboardService.focus(), dashboardService.recentActivity(7),
    ]).then(([s, w, u, b, f, a]) => {
      setSummary(s); setWeekly(w); setUpcoming(u); setBreakdown(b); setFocus(f); setActivity(a);
    }).catch(e => setError(e.message));
  }
  useEffect(load, []);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!summary) return <div className="flex"><Spinner /> <span className="muted">Loading your dashboard...</span></div>;

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  const pieData = breakdown ? Object.entries(breakdown.byStatus).map(([k, v]) => ({ name: k.replace('_', ' '), value: v, key: k })).filter(d => d.value > 0) : [];
  const stats = [
    [ListTodo, 'Total tasks', summary.totalTasks, '#7c6cf6'],
    [Clock, 'Pending', summary.pendingTasks, '#7cc4ff'],
    [TrendingUp, 'In progress', summary.inProgressTasks, '#b7a6ff'],
    [CheckCircle2, 'Completed', summary.completedTasks, '#4cc38a'],
    [AlertTriangle, 'Overdue', summary.overdueTasks, '#ff7a7a'],
    [CalendarClock, 'Due today', summary.dueTodayTasks, '#f0ad4e'],
    [Flame, 'High priority', summary.highPriorityTasks, '#ff9de2'],
  ];

  return (
    <div>
      <div className="flex wrap" style={{ justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: 24, fontWeight: 800 }}>{greeting}, {user?.name?.split(' ')[0]}</h2>
          <p className="muted">Here is what your day looks like.</p>
        </div>
        <div className="flex">
          <Link to="/app/capture" className="btn btn-teal"><Sparkles size={16} /> AI Capture</Link>
          <button className="btn btn-primary" onClick={() => setShowNew(true)}><Plus size={16} /> New task</button>
        </div>
      </div>

      <div className="stat-grid mt">
        {stats.map(([Icon, label, value, color]) => (
          <div key={label} className="card stat-card">
            <div className="label"><span className="stat-chip" style={{ background: color + '26', color }}><Icon size={15} /></span> {label}</div>
            <div className="value">{value}</div>
          </div>
        ))}
      </div>

      <div className="card card-p mt">
        <div className="flex" style={{ justifyContent: 'space-between' }}>
          <div>
            <div className="section-title">Overall completion</div>
            <div className="muted small">{summary.completedTasks} of {summary.totalTasks} tasks completed · {summary.completedThisWeek} completed this week</div>
          </div>
          <div style={{ fontSize: 22, fontWeight: 800, color: 'var(--teal)' }}>{summary.completionRate}%</div>
        </div>
        <div className="progress mt"><div style={{ width: summary.completionRate + '%' }} /></div>
      </div>

      {focus && (
        <div className="card card-p mt" style={{ borderLeft: '4px solid var(--teal)' }}>
          <div className="flex" style={{ justifyContent: 'space-between' }}>
            <div>
              <div className="small" style={{ fontWeight: 700, color: 'var(--teal)', textTransform: 'uppercase', letterSpacing: .5 }}>Today's focus · score {focus.score}</div>
              <div style={{ fontSize: 17, fontWeight: 700, margin: '4px 0' }}>{focus.task.title}</div>
              <div className="muted small">{focus.reasons.join(' · ')}</div>
            </div>
            <div className="flex">
              <PriorityBadge value={focus.task.priority} />
              <button className="btn btn-outline btn-sm" onClick={() => setEditing(focus.task)}>Open</button>
            </div>
          </div>
        </div>
      )}

      <div className="grid-2 mt">
        <div className="card card-p">
          <div className="section-title">Weekly productivity</div>
          <div className="muted small mb">Tasks completed and created per day, last 7 days</div>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={weekly} margin={{ top: 6, right: 6, left: -18, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#26262f" />
              <XAxis dataKey="dayLabel" tick={{ fontSize: 12, fill: '#9d9cae' }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: '#9d9cae' }} />
              <Tooltip cursor={{ fill: 'rgba(255,255,255,0.04)' }} contentStyle={{ background: '#21212b', border: '1px solid #2e2e3a', borderRadius: 10, color: '#f2f1f7' }} labelStyle={{ color: '#f2f1f7' }} itemStyle={{ color: '#f2f1f7' }} />
              <Bar dataKey="completed" name="Completed" fill="#7c6cf6" radius={[4, 4, 0, 0]} />
              <Bar dataKey="created" name="Created" fill="#7cc4ff" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
        <div className="card card-p">
          <div className="section-title">Tasks by status</div>
          <div className="muted small mb">Live distribution from your task list</div>
          {pieData.length === 0 ? <EmptyState text="No tasks yet" sub="Create your first task to see analytics." /> : (
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie data={pieData} dataKey="value" nameKey="name" innerRadius={55} outerRadius={85} paddingAngle={2}>
                  {pieData.map(d => <Cell key={d.key} fill={PIE_COLORS[d.key]} />)}
                </Pie>
                <Tooltip contentStyle={{ background: '#21212b', border: '1px solid #2e2e3a', borderRadius: 10, color: '#f2f1f7' }} itemStyle={{ color: '#f2f1f7' }} />
              </PieChart>
            </ResponsiveContainer>
          )}
          <div className="flex wrap" style={{ justifyContent: 'center' }}>
            {pieData.map(d => <span key={d.key} className="small muted flex" style={{ gap: 5 }}><span style={{ width: 9, height: 9, borderRadius: 3, background: PIE_COLORS[d.key], display: 'inline-block' }} />{d.name}: {d.value}</span>)}
          </div>
        </div>
      </div>

      <Insights />

      <div className="grid-2 mt">
        <div className="card">
          <div className="card-p" style={{ paddingBottom: 8 }}><div className="section-title">Upcoming deadlines</div></div>
          {upcoming.length === 0 ? <EmptyState text="Nothing due in the next 7 days" /> : (
            <table className="table"><tbody>
              {upcoming.slice(0, 6).map(t => (
                <tr key={t.id} onClick={() => setEditing(t)} style={{ cursor: 'pointer' }}>
                  <td><div style={{ fontWeight: 600 }}>{t.title}</div><CategoryBadge value={t.category} /></td>
                  <td><DueLabel task={t} /></td>
                  <td style={{ textAlign: 'right' }}><PriorityBadge value={t.priority} /></td>
                </tr>
              ))}
            </tbody></table>
          )}
        </div>
        <div className="card card-p">
          <div className="section-title">Recent activity</div>
          {activity.length === 0 ? <EmptyState text="No activity yet" /> : activity.map(h => (
            <div key={h.id} className="timeline-item">
              <div>
                <div style={{ fontSize: 13.5 }}><b>{h.action.replace(/_/g, ' ')}</b> · {h.taskTitle}</div>
                <div className="muted small">{h.detail || ''} {h.detail ? '·' : ''} {timeAgo(h.createdAt)}</div>
              </div>
            </div>
          ))}
          <Link to="/app/activity" className="small">View full activity →</Link>
        </div>
      </div>

      {(showNew || editing) && (
        <TaskModal task={editing} onClose={() => { setShowNew(false); setEditing(null); }}
          onSaved={() => { setShowNew(false); setEditing(null); load(); }} />
      )}
    </div>
  );
}
