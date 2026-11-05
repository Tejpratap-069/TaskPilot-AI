import { useEffect, useState } from 'react';
import { X, Plus, Trash2, Check } from 'lucide-react';
import { taskService } from '../api/services';
import { PRIORITIES, STATUSES, CATEGORIES, fmtDate } from './ui';

export default function TaskModal({ task, onClose, onSaved }) {
  const editing = !!task;
  const [form, setForm] = useState({
    title: '', description: '', status: 'TODO', priority: 'MEDIUM',
    dueDate: '', dueTime: '', category: '', estimatedMinutes: '', dependsOn: '',
  });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [subtasks, setSubtasks] = useState([]);
  const [history, setHistory] = useState([]);
  const [newSub, setNewSub] = useState('');

  useEffect(() => {
    if (task) {
      setForm({
        title: task.title || '', description: task.description || '', status: task.status || 'TODO',
        priority: task.priority || 'MEDIUM', dueDate: task.dueDate || '', dueTime: task.dueTime || '',
        category: task.category || '', estimatedMinutes: task.estimatedMinutes ?? '', dependsOn: task.dependsOn || '',
      });
      setSubtasks(task.subtasks || []);
      taskService.history(task.id).then(setHistory).catch(() => {});
    }
  }, [task]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function save() {
    if (!form.title.trim()) { setError('Title is required.'); return; }
    setSaving(true); setError('');
    const payload = {
      title: form.title.trim(),
      description: form.description || null,
      status: form.status,
      priority: form.priority,
      dueDate: form.dueDate || null,
      dueTime: form.dueTime || null,
      category: form.category || null,
      estimatedMinutes: form.estimatedMinutes === '' ? null : Number(form.estimatedMinutes),
      dependsOn: form.dependsOn || null,
    };
    try {
      if (editing) await taskService.update(task.id, payload);
      else await taskService.create(payload);
      onSaved();
    } catch (e) { setError(e.message); setSaving(false); }
  }

  async function addSubtask() {
    if (!newSub.trim()) return;
    try {
      const s = await taskService.addSubtask(task.id, newSub.trim());
      setSubtasks([...subtasks, s]); setNewSub('');
    } catch (e) { setError(e.message); }
  }
  async function toggleSubtask(s) {
    try {
      const upd = await taskService.toggleSubtask(task.id, s.id);
      setSubtasks(subtasks.map(x => (x.id === s.id ? upd : x)));
    } catch (e) { setError(e.message); }
  }
  async function remove() {
    if (!window.confirm('Delete this task permanently?')) return;
    try { await taskService.remove(task.id); onSaved(); } catch (e) { setError(e.message); }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <header>
          <h3>{editing ? 'Task details' : 'New task'}</h3>
          <button className="icon-btn" onClick={onClose}><X size={18} /></button>
        </header>
        <div className="body">
          {error && <div className="alert alert-error">{error}</div>}
          <div className="field"><label>Title</label>
            <input className="input" value={form.title} onChange={set('title')} placeholder="e.g. Finish Java assignment" /></div>
          <div className="field"><label>Description</label>
            <textarea className="textarea" rows={2} value={form.description} onChange={set('description')} placeholder="Optional details" /></div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="field"><label>Status</label>
              <select className="select" value={form.status} onChange={set('status')}>
                {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
              </select></div>
            <div className="field"><label>Priority</label>
              <select className="select" value={form.priority} onChange={set('priority')}>
                {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
              </select></div>
            <div className="field"><label>Due date</label>
              <input className="input" type="date" value={form.dueDate} onChange={set('dueDate')} /></div>
            <div className="field"><label>Due time</label>
              <input className="input" type="time" value={form.dueTime} onChange={set('dueTime')} /></div>
            <div className="field"><label>Category</label>
              <input className="input" list="tp-cats" value={form.category} onChange={set('category')} placeholder="STUDY, WORK..." />
              <datalist id="tp-cats">{CATEGORIES.map(c => <option key={c} value={c} />)}</datalist></div>
            <div className="field"><label>Estimated minutes</label>
              <input className="input" type="number" min="0" value={form.estimatedMinutes} onChange={set('estimatedMinutes')} placeholder="e.g. 90" /></div>
          </div>
          <div className="field"><label>Depends on</label>
            <input className="input" value={form.dependsOn} onChange={set('dependsOn')} placeholder="Optional - e.g. after finishing the report draft" /></div>

          {editing && (
            <>
              <div className="section-title mt">Subtasks</div>
              {subtasks.length === 0 && <div className="muted small mb">No subtasks yet.</div>}
              {subtasks.map(s => (
                <div key={s.id} className="flex" style={{ padding: '5px 0' }}>
                  <button className="icon-btn" onClick={() => toggleSubtask(s)} title="Toggle">
                    <span style={{ width: 18, height: 18, borderRadius: 5, border: '1.5px solid var(--blue-gray)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', background: s.completed ? 'var(--success)' : '#fff', color: '#fff' }}>
                      {s.completed && <Check size={12} />}
                    </span>
                  </button>
                  <span style={{ textDecoration: s.completed ? 'line-through' : 'none', color: s.completed ? 'var(--text-2)' : 'var(--text)' }}>{s.title}</span>
                </div>
              ))}
              <div className="flex mt">
                <input className="input" value={newSub} onChange={e => setNewSub(e.target.value)} placeholder="Add a subtask" onKeyDown={e => e.key === 'Enter' && addSubtask()} />
                <button className="btn btn-outline" onClick={addSubtask}><Plus size={15} /> Add</button>
              </div>

              <div className="section-title mt">History</div>
              {history.length === 0 && <div className="muted small">No history yet.</div>}
              {history.slice(0, 8).map(h => (
                <div key={h.id} className="small" style={{ padding: '4px 0', borderBottom: '1px solid var(--border)' }}>
                  <b>{h.action.replace('_', ' ')}</b> <span className="muted">· {h.detail || ''} · {new Date(h.createdAt).toLocaleString()}</span>
                </div>
              ))}
            </>
          )}
        </div>
        <footer>
          {editing && <button className="btn btn-ghost" style={{ color: 'var(--error)', marginRight: 'auto' }} onClick={remove}><Trash2 size={15} /> Delete</button>}
          <button className="btn btn-outline" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Saving...' : editing ? 'Save changes' : 'Create task'}</button>
        </footer>
      </div>
    </div>
  );
}
