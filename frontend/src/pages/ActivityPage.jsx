import { useEffect, useState } from 'react';
import { Plus, Pencil, CheckCircle2, RotateCcw, Trash2, ArrowRightCircle, CalendarClock, Star } from 'lucide-react';
import { activityService } from '../api/services';
import { EmptyState, Spinner, timeAgo } from '../components/ui';

const ICONS = {
  CREATED: [Plus, 'Task created'], UPDATED: [Pencil, 'Task updated'],
  STATUS_CHANGED: [ArrowRightCircle, 'Status changed'], PRIORITY_CHANGED: [Star, 'Priority changed'],
  DEADLINE_CHANGED: [CalendarClock, 'Deadline changed'], COMPLETED: [CheckCircle2, 'Task completed'],
  REOPENED: [RotateCcw, 'Task reopened'], DELETED: [Trash2, 'Task deleted'],
};

export default function ActivityPage() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    activityService.list(80).then(setItems).catch(e => setError(e.message));
  }, []);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (items === null) return <div className="flex"><Spinner /> <span className="muted">Loading activity...</span></div>;

  return (
    <div className="card card-p">
      <div className="section-title">Activity history</div>
      <p className="muted small mb">Every change to your tasks, newest first. Stored permanently with each task.</p>
      {items.length === 0 ? <EmptyState text="No activity yet" sub="Create or change a task and it will appear here." /> :
        items.map(h => {
          const [Icon, label] = ICONS[h.action] || [Pencil, h.action];
          return (
            <div key={h.id} className="timeline-item">
              <div className="timeline-dot"><Icon size={15} /></div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14 }}><b>{label}</b> · {h.taskTitle || 'Task'}
                  {h.fieldName && h.oldValue != null && <span className="muted"> — {h.fieldName}: {h.oldValue} → {h.newValue}</span>}
                </div>
                <div className="muted small">{h.detail || ''}</div>
              </div>
              <div className="small muted">{timeAgo(h.createdAt)}</div>
            </div>
          );
        })}
    </div>
  );
}
