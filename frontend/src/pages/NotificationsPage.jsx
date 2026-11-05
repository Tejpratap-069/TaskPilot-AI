import { useEffect, useState } from 'react';
import { Bell, AlertTriangle, CalendarClock, CheckCircle2, Info } from 'lucide-react';
import { notificationService } from '../api/services';
import { EmptyState, Spinner, timeAgo } from '../components/ui';

const ICONS = { OVERDUE: [AlertTriangle, 'var(--error)'], DUE_TODAY: [CalendarClock, 'var(--warning)'], DUE_SOON: [CalendarClock, 'var(--info)'], COMPLETED: [CheckCircle2, 'var(--success)'], INFO: [Info, 'var(--info)'] };

export default function NotificationsPage() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');

  function load() { notificationService.list().then(setItems).catch(e => setError(e.message)); }
  useEffect(load, []);

  async function markRead(n) {
    try { const upd = await notificationService.markRead(n.id); setItems(items.map(x => (x.id === n.id ? upd : x))); }
    catch (e) { setError(e.message); }
  }

  if (error) return <div className="alert alert-error">{error}</div>;
  if (items === null) return <div className="flex"><Spinner /> <span className="muted">Loading notifications...</span></div>;

  return (
    <div className="card card-p">
      <div className="section-title flex"><Bell size={18} /> Notifications</div>
      <p className="muted small mb">Generated from your real tasks - overdue items, tasks due today or tomorrow, and completions.</p>
      {items.length === 0 ? <EmptyState text="No notifications" sub="Deadline reminders will appear here as tasks approach their due dates." /> :
        items.map(n => {
          const [Icon, color] = ICONS[n.type] || ICONS.INFO;
          return (
            <div key={n.id} className="timeline-item" style={{ opacity: n.read ? 0.6 : 1 }}>
              <div className="timeline-dot" style={{ color }}><Icon size={15} /></div>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: n.read ? 500 : 700, fontSize: 14 }}>{n.title}</div>
                <div className="muted small">{n.message}</div>
              </div>
              <div className="flex">
                <span className="small muted">{timeAgo(n.createdAt)}</span>
                {!n.read && <button className="btn btn-outline btn-sm" onClick={() => markRead(n)}>Mark read</button>}
              </div>
            </div>
          );
        })}
    </div>
  );
}
