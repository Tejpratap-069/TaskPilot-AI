import { useState } from 'react';
import { Sparkles, Check, Pencil, Trash2, CheckCheck, AlertTriangle, Info } from 'lucide-react';
import { aiService, taskService } from '../api/services';
import { PRIORITIES, CATEGORIES, fmtDate } from '../components/ui';

const EXAMPLE = 'I have to finish my Java assignment by tomorrow, revise machine learning on Sunday, complete my Japanese preparation before Monday, and submit my project next Friday. Java is the most urgent.';

export default function AiCapture() {
  const [text, setText] = useState('');
  const [analyzing, setAnalyzing] = useState(false);
  const [suggestions, setSuggestions] = useState(null);
  const [engine, setEngine] = useState('');
  const [fallbackUsed, setFallbackUsed] = useState(false);
  const [error, setError] = useState('');
  const [savedCount, setSavedCount] = useState(null);
  const [saving, setSaving] = useState(false);

  async function analyze() {
    if (!text.trim()) return;
    setAnalyzing(true); setError(''); setSavedCount(null);
    try {
      const res = await aiService.parse(text.trim());
      setSuggestions(res.tasks.map(t => ({ ...t, approved: true })));
      setEngine(res.engine);
      setFallbackUsed(res.fallbackUsed);
    } catch (e) { setError(e.message); }
    setAnalyzing(false);
  }

  function update(i, key, value) {
    setSuggestions(suggestions.map((s, idx) => (idx === i ? { ...s, [key]: value } : s)));
  }
  function remove(i) { setSuggestions(suggestions.filter((_, idx) => idx !== i)); }

  async function saveSelected() {
    const chosen = suggestions.filter(s => s.approved);
    if (chosen.length === 0) { setError('Select at least one task to save.'); return; }
    setSaving(true); setError('');
    try {
      for (const s of chosen) {
        await taskService.create({
          title: s.title, description: s.description || null, priority: s.priority || 'MEDIUM',
          dueDate: s.dueDate || null, dueTime: s.dueTime || null, category: s.category || null,
          estimatedMinutes: s.estimatedMinutes || null, dependsOn: s.dependsOn || null, status: 'TODO',
        });
      }
      setSavedCount(chosen.length);
      setSuggestions(suggestions.filter(s => !s.approved));
    } catch (e) { setError(e.message); }
    setSaving(false);
  }

  return (
    <div>
      <div className="card card-p">
        <div className="flex"><Sparkles size={20} style={{ color: 'var(--teal)' }} /><div className="section-title">AI Task Capture</div></div>
        <p className="muted mt" style={{ marginTop: 6 }}>
          Describe everything you need to do in plain words. TaskPilot extracts individual tasks,
          deadlines and priorities. You review and approve - nothing is saved without your confirmation.
        </p>
        <textarea className="textarea mt" rows={4} value={text} onChange={e => setText(e.target.value)}
          placeholder="e.g. Finish my DSA assignment tomorrow, revise ML on Sunday, and complete my project by November 5." />
        <div className="flex mt wrap">
          <button className="btn btn-teal" onClick={analyze} disabled={analyzing || !text.trim()}>
            {analyzing ? <><span className="spinner" /> Analyzing...</> : <><Sparkles size={16} /> Analyze instruction</>}
          </button>
          <button className="btn btn-ghost" onClick={() => setText(EXAMPLE)}>Use example</button>
          <span className="small muted">Engine: explainable NLP / rule-based extraction (Python service, with a local fallback).</span>
        </div>
      </div>

      {error && <div className="alert alert-error mt">{error}</div>}
      {savedCount !== null && <div className="alert alert-success mt">{savedCount} task(s) saved to your task list. Check My Tasks or the dashboard to see them.</div>}
      {fallbackUsed && suggestions && <div className="alert alert-warn mt">The Python NLP service is not reachable, so the built-in local fallback parser produced these results. Quality is equivalent for common phrasing.</div>}

      {suggestions && (
        <div className="mt">
          <div className="flex" style={{ justifyContent: 'space-between' }}>
            <div className="section-title">Extracted tasks ({suggestions.length})</div>
            <button className="btn btn-primary" onClick={saveSelected} disabled={saving || suggestions.every(s => !s.approved)}>
              <CheckCheck size={16} /> {saving ? 'Saving...' : 'Save approved tasks'}
            </button>
          </div>
          {suggestions.length === 0 && <div className="card card-p mt muted">All extracted tasks have been handled. Analyze another instruction whenever you are ready.</div>}
          {suggestions.map((s, i) => (
            <div key={i} className="card card-p mt" style={{ opacity: s.approved ? 1 : 0.55 }}>
              <div className="flex wrap">
                <label className="flex" style={{ gap: 7, fontWeight: 600 }}>
                  <input type="checkbox" checked={s.approved} onChange={e => update(i, 'approved', e.target.checked)} />
                  Save this task
                </label>
                <span className="small muted flex" style={{ gap: 6 }}>
                  Confidence <span className="conf-bar"><div style={{ width: Math.round((s.confidence || 0) * 100) + '%' }} /></span>
                  {Math.round((s.confidence || 0) * 100)}%
                </span>
                <span style={{ marginLeft: 'auto' }} className="flex">
                  <button className="icon-btn" onClick={() => remove(i)} title="Reject suggestion"><Trash2 size={16} /></button>
                </span>
              </div>
              <input className="input mt" value={s.title} onChange={e => update(i, 'title', e.target.value)} style={{ fontWeight: 600 }} />
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 10, marginTop: 10 }}>
                <div><label className="small" style={{ fontWeight: 600 }}>Due date</label>
                  <input className="input" type="date" value={s.dueDate || ''} onChange={e => update(i, 'dueDate', e.target.value)} /></div>
                <div><label className="small" style={{ fontWeight: 600 }}>Due time</label>
                  <input className="input" type="time" value={s.dueTime || ''} onChange={e => update(i, 'dueTime', e.target.value)} /></div>
                <div><label className="small" style={{ fontWeight: 600 }}>Priority</label>
                  <select className="select" value={s.priority} onChange={e => update(i, 'priority', e.target.value)}>
                    {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
                  </select></div>
                <div><label className="small" style={{ fontWeight: 600 }}>Category</label>
                  <select className="select" value={s.category || 'OTHER'} onChange={e => update(i, 'category', e.target.value)}>
                    {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                  </select></div>
                <div><label className="small" style={{ fontWeight: 600 }}>Est. minutes</label>
                  <input className="input" type="number" min="0" value={s.estimatedMinutes || ''} onChange={e => update(i, 'estimatedMinutes', e.target.value === '' ? null : Number(e.target.value))} /></div>
              </div>
              {s.dependsOn && <div className="small muted mt"><Info size={13} /> Depends on: {s.dependsOn}</div>}
              {(s.warnings || []).map((w, wi) => (
                <div key={wi} className="small mt" style={{ color: 'var(--warning)', display: 'flex', gap: 6, alignItems: 'center' }}>
                  <AlertTriangle size={13} /> {w}
                </div>
              ))}
              {s.dueDate && <div className="small muted mt">Detected deadline: {fmtDate(s.dueDate)}{s.dueTime ? ' at ' + s.dueTime : ''}</div>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
