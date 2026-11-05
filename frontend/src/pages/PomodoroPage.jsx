import { useEffect, useRef, useState } from 'react';
import { Play, Pause, RotateCcw } from 'lucide-react';
import { preferenceService } from '../api/services';

const MODES = { WORK: 'Focus', SHORT: 'Short break', LONG: 'Long break' };
const HISTORY_KEY = 'tp_pomodoro_history';

export default function PomodoroPage() {
  const [prefs, setPrefs] = useState({ pomodoroWorkMinutes: 25, pomodoroShortBreakMinutes: 5, pomodoroLongBreakMinutes: 15 });
  const [mode, setMode] = useState('WORK');
  const [secondsLeft, setSecondsLeft] = useState(25 * 60);
  const [running, setRunning] = useState(false);
  const [cycles, setCycles] = useState(0);
  const [history, setHistory] = useState(() => { try { return JSON.parse(localStorage.getItem(HISTORY_KEY)) || []; } catch { return []; } });
  const intervalRef = useRef(null);

  useEffect(() => { preferenceService.get().then(p => { setPrefs(p); setSecondsLeft(p.pomodoroWorkMinutes * 60); }).catch(() => {}); }, []);

  const duration = mode === 'WORK' ? prefs.pomodoroWorkMinutes * 60 : mode === 'SHORT' ? prefs.pomodoroShortBreakMinutes * 60 : prefs.pomodoroLongBreakMinutes * 60;

  useEffect(() => { setSecondsLeft(duration); setRunning(false); }, [mode, duration]);

  useEffect(() => {
    if (!running) { clearInterval(intervalRef.current); return; }
    intervalRef.current = setInterval(() => {
      setSecondsLeft(s => {
        if (s <= 1) {
          clearInterval(intervalRef.current);
          setRunning(false);
          if (mode === 'WORK') {
            const entry = { date: new Date().toISOString(), minutes: prefs.pomodoroWorkMinutes };
            const next = [entry, ...history].slice(0, 30);
            setHistory(next);
            localStorage.setItem(HISTORY_KEY, JSON.stringify(next));
            const c = cycles + 1;
            setCycles(c);
            setMode(c % 4 === 0 ? 'LONG' : 'SHORT');
          } else {
            setMode('WORK');
          }
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => clearInterval(intervalRef.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [running, mode]);

  const mm = String(Math.floor(secondsLeft / 60)).padStart(2, '0');
  const ss = String(secondsLeft % 60).padStart(2, '0');
  const progress = duration ? (1 - secondsLeft / duration) * 100 : 0;

  return (
    <div>
      <div className="card card-p" style={{ maxWidth: 520, margin: '0 auto', textAlign: 'center' }}>
        <div className="flex" style={{ justifyContent: 'center' }}>
          {Object.keys(MODES).map(m => (
            <button key={m} className={'btn btn-sm ' + (mode === m ? 'btn-primary' : 'btn-outline')} onClick={() => setMode(m)}>{MODES[m]}</button>
          ))}
        </div>
        <div className="timer-big mt" style={{ color: mode === 'WORK' ? 'var(--navy)' : 'var(--teal)' }}>{mm}:{ss}</div>
        <div className="progress" style={{ maxWidth: 340, margin: '0 auto' }}><div style={{ width: progress + '%' }} /></div>
        <div className="flex mt" style={{ justifyContent: 'center' }}>
          <button className="btn btn-teal" onClick={() => setRunning(!running)}>
            {running ? <><Pause size={16} /> Pause</> : <><Play size={16} /> Start</>}
          </button>
          <button className="btn btn-outline" onClick={() => { setRunning(false); setSecondsLeft(duration); }}>
            <RotateCcw size={16} /> Reset
          </button>
        </div>
        <p className="muted small mt">Durations come from your Settings ({prefs.pomodoroWorkMinutes} / {prefs.pomodoroShortBreakMinutes} / {prefs.pomodoroLongBreakMinutes} minutes). A long break follows every 4 focus sessions.</p>
      </div>
      <div className="card card-p mt" style={{ maxWidth: 520, margin: '16px auto 0' }}>
        <div className="section-title">Completed focus sessions</div>
        {history.length === 0 ? <p className="muted small">No completed sessions yet this device.</p> :
          history.slice(0, 8).map((h, i) => (
            <div key={i} className="flex" style={{ justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border)' }}>
              <span>{h.minutes} minute focus</span>
              <span className="muted small">{new Date(h.date).toLocaleString()}</span>
            </div>
          ))}
      </div>
    </div>
  );
}
