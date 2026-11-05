import { Inbox } from 'lucide-react';

export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
export const STATUSES = ['TODO', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
export const CATEGORIES = ['STUDY', 'PROJECT', 'WORK', 'HEALTH', 'FINANCE', 'PERSONAL', 'OTHER'];

export function PriorityBadge({ value }) {
  return <span className={`badge badge-${value}`}>{value}</span>;
}
export function StatusBadge({ value }) {
  return <span className={`badge badge-${value}`}>{value.replace('_', ' ')}</span>;
}
export function CategoryBadge({ value }) {
  if (!value) return null;
  return <span className="badge badge-cat">{value}</span>;
}
export function Spinner() { return <span className="spinner" />; }
export function EmptyState({ text, sub }) {
  return (
    <div className="empty">
      <div className="icon"><Inbox size={34} /></div>
      <div style={{ fontWeight: 600, color: 'var(--text)' }}>{text}</div>
      {sub && <div className="small">{sub}</div>}
    </div>
  );
}
export function fmtDate(d) {
  if (!d) return '—';
  const date = new Date(d + 'T00:00:00');
  return date.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}
export function isOverdue(task) {
  if (!task.dueDate || task.status === 'COMPLETED' || task.status === 'CANCELLED') return false;
  const today = new Date(); today.setHours(0, 0, 0, 0);
  return new Date(task.dueDate + 'T00:00:00') < today;
}
export function isDueToday(task) {
  if (!task.dueDate) return false;
  const today = new Date();
  const key = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0');
  return task.dueDate === key;
}
export function DueLabel({ task }) {
  if (!task.dueDate) return <span className="muted">No date</span>;
  if (isOverdue(task)) return <span className="overdue-text">{fmtDate(task.dueDate)} · overdue</span>;
  if (isDueToday(task)) return <span style={{ color: 'var(--warning)', fontWeight: 600 }}>{fmtDate(task.dueDate)} · today</span>;
  return <span>{fmtDate(task.dueDate)}</span>;
}
export function timeAgo(iso) {
  if (!iso) return '';
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return mins + 'm ago';
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return hrs + 'h ago';
  const days = Math.floor(hrs / 24);
  return days + 'd ago';
}
