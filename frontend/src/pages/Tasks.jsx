import { useEffect, useState } from 'react';
import { Plus, Search, Pencil, Trash2, CheckCircle2, RotateCcw } from 'lucide-react';
import { taskService } from '../api/services';
import { PriorityBadge, StatusBadge, DueLabel, EmptyState, Spinner, CategoryBadge, PRIORITIES, STATUSES } from '../components/ui';
import TaskModal from '../components/TaskModal';

export default function Tasks() {
  const [tasks, setTasks] = useState(null);
  const [total, setTotal] = useState(0);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [priority, setPriority] = useState('');
  const [sortBy, setSortBy] = useState('dueDate');
  const [error, setError] = useState('');
  const [modal, setModal] = useState(undefined); // undefined=closed, null=new, obj=edit

  function load() {
    taskService.list({ search, status, priority, sortBy, size: 200 })
      .then(page => { setTasks(page.content || []); setTotal(page.totalElements ?? (page.content || []).length); })
      .catch(e => setError(e.message));
  }
  useEffect(() => { const t = setTimeout(load, 200); return () => clearTimeout(t); }, [search, status, priority, sortBy]);

  async function quickStatus(task, next) {
    try { const upd = await taskService.setStatus(task.id, next); setTasks(tasks.map(t => (t.id === task.id ? upd : t))); }
    catch (e) { setError(e.message); }
  }
  async function remove(task) {
    if (!window.confirm('Delete "' + task.title + '"?')) return;
    try { await taskService.remove(task.id); load(); } catch (e) { setError(e.message); }
  }

  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="flex wrap">
        <div className="flex" style={{ flex: 1, minWidth: 220, position: 'relative' }}>
          <Search size={16} style={{ position: 'absolute', left: 10, color: 'var(--text-2)' }} />
          <input className="input" style={{ paddingLeft: 32 }} placeholder="Search tasks..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <select className="select" style={{ width: 170 }} value={status} onChange={e => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
        </select>
        <select className="select" style={{ width: 160 }} value={priority} onChange={e => setPriority(e.target.value)}>
          <option value="">All priorities</option>
          {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
        </select>
        <select className="select" style={{ width: 160 }} value={sortBy} onChange={e => setSortBy(e.target.value)}>
          <option value="dueDate">Sort: due date</option>
          <option value="createdAt">Sort: recently created</option>
        </select>
        <button className="btn btn-primary" onClick={() => setModal(null)}><Plus size={16} /> New task</button>
      </div>

      <div className="card mt">
        {tasks === null ? <div className="card-p flex"><Spinner /> <span className="muted">Loading tasks...</span></div>
        : tasks.length === 0 ? <EmptyState text="No tasks found" sub="Try a different filter, or create a new task." />
        : (
          <table className="table">
            <thead><tr><th>Task</th><th>Due</th><th>Priority</th><th>Status</th><th style={{ textAlign: 'right' }}>Actions</th></tr></thead>
            <tbody>
              {tasks.map(t => (
                <tr key={t.id}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{t.title}</div>
                    <div className="flex" style={{ gap: 6, marginTop: 2 }}>
                      <CategoryBadge value={t.category} />
                      {t.estimatedMinutes ? <span className="small muted">{t.estimatedMinutes} min</span> : null}
                      {t.subtasks?.length ? <span className="small muted">{t.subtasks.filter(s => s.completed).length}/{t.subtasks.length} subtasks</span> : null}
                    </div>
                  </td>
                  <td><DueLabel task={t} /></td>
                  <td><PriorityBadge value={t.priority} /></td>
                  <td>
                    <select className="select" style={{ width: 150, padding: '5px 8px', fontSize: 13 }} value={t.status}
                      onChange={e => quickStatus(t, e.target.value)}>
                      {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                    </select>
                  </td>
                  <td>
                    <div className="row-actions">
                      {t.status !== 'COMPLETED'
                        ? <button className="icon-btn" title="Mark complete" onClick={() => quickStatus(t, 'COMPLETED')}><CheckCircle2 size={17} /></button>
                        : <button className="icon-btn" title="Reopen" onClick={() => quickStatus(t, 'TODO')}><RotateCcw size={17} /></button>}
                      <button className="icon-btn" title="Edit" onClick={() => setModal(t)}><Pencil size={16} /></button>
                      <button className="icon-btn" title="Delete" onClick={() => remove(t)}><Trash2 size={16} /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      <div className="small muted mt">{total} task(s)</div>

      {modal !== undefined && (
        <TaskModal task={modal} onClose={() => setModal(undefined)} onSaved={() => { setModal(undefined); load(); }} />
      )}
    </div>
  );
}
