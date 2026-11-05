import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { preferenceService } from '../api/services';
import { Spinner } from '../components/ui';

export default function SettingsPage() {
  const { user } = useAuth();
  const [prefs, setPrefs] = useState(null);
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  useEffect(() => { preferenceService.get().then(setPrefs).catch(e => setError(e.message)); }, []);

  async function save() {
    setError(''); setMsg('');
    try { const saved = await preferenceService.update(prefs); setPrefs(saved); setMsg('Preferences saved.'); }
    catch (e) { setError(e.message); }
  }
  const set = (k) => (e) => setPrefs({ ...prefs, [k]: e.target.type === 'checkbox' ? e.target.checked : Number(e.target.value) });

  return (
    <div>
      <div className="card card-p">
        <div className="section-title">Profile</div>
        <div className="mt">
          <div className="field"><label>Name</label><input className="input" value={user?.name || ''} disabled /></div>
          <div className="field"><label>Email</label><input className="input" value={user?.email || ''} disabled /></div>
          <div className="field"><label>Role</label><input className="input" value={user?.role || 'USER'} disabled /></div>
        </div>
      </div>
      <div className="card card-p mt">
        <div className="section-title">Preferences</div>
        {error && <div className="alert alert-error mt">{error}</div>}
        {msg && <div className="alert alert-success mt">{msg}</div>}
        {!prefs ? <div className="flex mt"><Spinner /> <span className="muted">Loading...</span></div> : (
          <>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }} className="mt">
              <div className="field"><label>Pomodoro work (minutes)</label>
                <input className="input" type="number" min="1" max="180" value={prefs.pomodoroWorkMinutes} onChange={set('pomodoroWorkMinutes')} /></div>
              <div className="field"><label>Short break (minutes)</label>
                <input className="input" type="number" min="1" max="60" value={prefs.pomodoroShortBreakMinutes} onChange={set('pomodoroShortBreakMinutes')} /></div>
              <div className="field"><label>Long break (minutes)</label>
                <input className="input" type="number" min="1" max="90" value={prefs.pomodoroLongBreakMinutes} onChange={set('pomodoroLongBreakMinutes')} /></div>
              <div className="field"><label>Daily task goal</label>
                <input className="input" type="number" min="1" max="50" value={prefs.dailyGoalTasks} onChange={set('dailyGoalTasks')} /></div>
            </div>
            <label className="flex" style={{ gap: 8 }}>
              <input type="checkbox" checked={prefs.deadlineReminders} onChange={set('deadlineReminders')} />
              Enable deadline reminder notifications
            </label>
            <div className="mt"><button className="btn btn-primary" onClick={save}>Save preferences</button></div>
          </>
        )}
      </div>
    </div>
  );
}
