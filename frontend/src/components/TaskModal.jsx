import { useEffect, useRef, useState } from 'react';
import { X, Plus, Trash2, Check, Download, FileText, Upload } from 'lucide-react';
import { taskService, attachmentService } from '../api/services';
import { PRIORITIES, STATUSES, CATEGORIES, fmtDate } from './ui';

function fmtSize(bytes) {
  if (bytes == null) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}
const ATT_ACCEPT = 'image/*,.pdf,.txt,.md,.csv,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip';

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
  const [attachments, setAttachments] = useState([]);
  const [thumbs, setThumbs] = useState({}); // attachmentId -> object URL (images only)
  const [attError, setAttError] = useState('');
  const [uploading, setUploading] = useState(false);
  const objectUrls = useRef(new Set());
  const fileInput = useRef(null);

  // Revoke every object URL this modal created when it unmounts.
  useEffect(() => {
    const urls = objectUrls.current;
    return () => { urls.forEach(u => URL.revokeObjectURL(u)); urls.clear(); };
  }, []);

  async function loadThumb(att) {
    try {
      const blob = await attachmentService.content(att.id);
      const url = URL.createObjectURL(blob);
      objectUrls.current.add(url);
      setThumbs(prev => ({ ...prev, [att.id]: url }));
    } catch { /* thumbnail is best-effort; the file chip still renders */ }
  }

  async function loadAttachments(taskId) {
    try {
      const list = await attachmentService.list(taskId);
      setAttachments(list || []);
      (list || []).forEach(a => { if (a.contentType && a.contentType.startsWith('image/')) loadThumb(a); });
    } catch (e) { setAttError(e.message); }
  }

  useEffect(() => {
    if (task) {
      setForm({
        title: task.title || '', description: task.description || '', status: task.status || 'TODO',
        priority: task.priority || 'MEDIUM', dueDate: task.dueDate || '', dueTime: task.dueTime || '',
        category: task.category || '', estimatedMinutes: task.estimatedMinutes ?? '', dependsOn: task.dependsOn || '',
      });
      setSubtasks(task.subtasks || []);
      taskService.history(task.id).then(setHistory).catch(() => {});
      if (task.id) loadAttachments(task.id);
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

  async function uploadAttachment(e) {
    const file = e.target.files && e.target.files[0];
    e.target.value = ''; // allow re-picking the same file
    if (!file) return;
    setUploading(true); setAttError('');
    try {
      await attachmentService.upload(task.id, file);
      await loadAttachments(task.id);
    } catch (err) { setAttError(err.message); }
    setUploading(false);
  }
  async function downloadAttachment(att) {
    try {
      const blob = await attachmentService.content(att.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = att.fileName || 'attachment';
      document.body.appendChild(a); a.click(); a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 4000);
    } catch (err) { setAttError(err.message); }
  }
  async function deleteAttachment(att) {
    try {
      await attachmentService.remove(att.id);
      if (thumbs[att.id]) {
        URL.revokeObjectURL(thumbs[att.id]);
        objectUrls.current.delete(thumbs[att.id]);
        setThumbs(prev => { const next = { ...prev }; delete next[att.id]; return next; });
      }
      await loadAttachments(task.id);
    } catch (err) { setAttError(err.message); }
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
                    <span style={{ width: 18, height: 18, borderRadius: 5, border: '1.5px solid var(--blue-gray)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', background: s.completed ? 'var(--success)' : 'transparent', color: '#fff' }}>
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

              <div className="section-title mt">Attachments</div>
              {attError && <div className="alert alert-error">{attError}</div>}
              {attachments.length === 0 && <div className="muted small mb">No attachments yet. Max 2 MB per file.</div>}
              <div className="att-list">
                {attachments.map(a => (
                  <div key={a.id} className="att-item">
                    {a.contentType && a.contentType.startsWith('image/') && thumbs[a.id]
                      ? <img className="att-thumb" src={thumbs[a.id]} alt={a.fileName} />
                      : <span className="att-chip"><FileText size={17} /></span>}
                    <span className="att-meta">
                      <span className="att-name" title={a.fileName}>{a.fileName}</span>
                      <span className="small muted">{fmtSize(a.sizeBytes)}</span>
                    </span>
                    <button className="icon-btn" title="Download" onClick={() => downloadAttachment(a)}><Download size={16} /></button>
                    <button className="icon-btn" title="Delete attachment" onClick={() => deleteAttachment(a)}><Trash2 size={16} /></button>
                  </div>
                ))}
              </div>
              <input ref={fileInput} type="file" accept={ATT_ACCEPT} style={{ display: 'none' }} onChange={uploadAttachment} />
              <button className="btn btn-outline mt" onClick={() => fileInput.current && fileInput.current.click()} disabled={uploading}>
                {uploading ? <><span className="spinner" /> Uploading...</> : <><Upload size={15} /> Upload file</>}
              </button>

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
