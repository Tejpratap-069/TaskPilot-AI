import { useEffect, useState } from 'react';
import { taskService } from '../api/services';
import { PriorityBadge, DueLabel, EmptyState, Spinner, STATUSES } from '../components/ui';
import TaskModal from '../components/TaskModal';

export default function KanbanPage() {
  const [tasks, setTasks] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null);

  function load() {
    taskService.list({ size: 200, sortBy: 'dueDate' }).then(p => setTasks(p.content || [])).catch(e => setError(e.message));
  }
  useEffect(load, []);

  async function changeStatus(task, next) {
    try { const upd = await taskService.setStatus(task.id, next); setTasks(tasks.map(t => (t.id === task.id ? upd : t))); }
    catch (e) { setError(e.message); }
  }

  if (tasks === null) return <div className="flex"><Spinner /> <span className="muted">Loading board...</span></div>;

  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="kanban">
        {STATUSES.map(status => {
          const col = tasks.filter(t => t.status === status);
          return (
            <div key={status} className="kanban-col">
              <h4>{status.replace('_', ' ')} <span>{col.length}</span></h4>
              {col.length === 0 && <div className="small muted" style={{ textAlign: 'center', padding: '14px 0' }}>No tasks</div>}
              {col.map(t => (
                <div key={t.id} className="kanban-card">
                  <div className="t" style={{ cursor: 'pointer' }} onClick={() => setEditing(t)}>{t.title}</div>
                  <div className="flex" style={{ justifyContent: 'space-between' }}>
                    <span className="small"><DueLabel task={t} /></span>
                    <PriorityBadge value={t.priority} />
                  </div>
                  <select value={t.status} onChange={e => changeStatus(t, e.target.value)}>
                    {STATUSES.map(s => <option key={s} value={s}>Move to {s.replace('_', ' ')}</option>)}
                  </select>
                </div>
              ))}
            </div>
          );
        })}
      </div>
      {editing && <TaskModal task={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); load(); }} />}
    </div>
  );
}
