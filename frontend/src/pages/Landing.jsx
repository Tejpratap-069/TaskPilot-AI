import { Link } from 'react-router-dom';
import { Compass, Sparkles, BarChart3, ShieldCheck, CalendarDays, ArrowRight, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Landing() {
  const { user } = useAuth();
  return (
    <div>
      <div className="landing-nav">
        <div className="flex" style={{ gap: 10 }}>
          <div style={{ width: 32, height: 32, borderRadius: 8, background: 'var(--teal)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}><Compass size={18} /></div>
          <b style={{ fontSize: 17 }}>TaskPilot AI</b>
        </div>
        <div style={{ flex: 1 }} />
        {user ? (
          <Link to="/app" className="btn btn-primary">Open dashboard <ArrowRight size={15} /></Link>
        ) : (
          <>
            <Link to="/login" className="btn btn-ghost">Sign in</Link>
            <Link to="/register" className="btn btn-primary">Get started</Link>
          </>
        )}
      </div>

      <div className="hero">
        <h1>Plan smarter. Focus better.<br />Get things done.</h1>
        <p>TaskPilot AI turns a plain sentence into an organised plan. Type what is on your mind —
          it extracts the tasks, deadlines and priorities, and you approve what gets saved.</p>
        <div className="flex" style={{ justifyContent: 'center' }}>
          <Link to={user ? '/app/capture' : '/register'} className="btn btn-teal" style={{ padding: '11px 22px', fontSize: 15 }}>
            <Sparkles size={17} /> Try AI Task Capture
          </Link>
          <Link to={user ? '/app' : '/login'} className="btn btn-outline" style={{ padding: '11px 22px', fontSize: 15 }}>View dashboard</Link>
        </div>
      </div>

      <div style={{ maxWidth: 880, margin: '10px auto 0', padding: '0 24px' }}>
        <div className="card card-p">
          <div className="small muted" style={{ fontWeight: 700, textTransform: 'uppercase', letterSpacing: .5 }}>You type</div>
          <p style={{ fontSize: 16, margin: '8px 0 14px' }}>
            "Finish my Java assignment by tomorrow, revise machine learning on Sunday,
            and submit my project next Friday. Java is the most urgent."
          </p>
          <div className="small muted" style={{ fontWeight: 700, textTransform: 'uppercase', letterSpacing: .5 }}>TaskPilot extracts</div>
          <div className="mt">
            {[['Finish Java assignment', 'Tomorrow', 'URGENT'], ['Revise machine learning', 'Sunday', 'MEDIUM'], ['Submit project', 'Next Friday', 'MEDIUM']].map(([t, d, p]) => (
              <div key={t} className="flex" style={{ padding: '7px 0', borderBottom: '1px solid var(--border)' }}>
                <CheckCircle2 size={16} style={{ color: 'var(--success)' }} />
                <span style={{ fontWeight: 600 }}>{t}</span>
                <span className="muted small">{d}</span>
                <span style={{ marginLeft: 'auto' }} className={`badge badge-${p}`}>{p}</span>
              </div>
            ))}
          </div>
          <p className="small muted mt">Nothing is saved until you review and approve each task.</p>
        </div>
      </div>

      <div className="feature-grid">
        {[
          [Sparkles, 'Natural-language capture', 'One instruction becomes multiple structured tasks with deadlines, priorities and categories - extracted by an explainable NLP engine.'],
          [BarChart3, 'A dashboard that is real', 'Every number - totals, overdue, due today, weekly completions - is calculated live from your saved tasks. No placeholders.'],
          [CalendarDays, 'Calendar, Kanban and My Day', 'See the same tasks by date, by status board, or as a focused daily plan. Everything stays in sync.'],
          [ShieldCheck, 'Private by design', 'Accounts are secured with hashed passwords and JWT sessions. Your tasks belong to you and only you.'],
        ].map(([Icon, title, body]) => (
          <div key={title} className="card card-p">
            <Icon size={22} style={{ color: 'var(--teal)' }} />
            <div style={{ fontWeight: 700, fontSize: 15.5, margin: '10px 0 6px' }}>{title}</div>
            <p className="muted" style={{ fontSize: 14 }}>{body}</p>
          </div>
        ))}
      </div>

      <div style={{ textAlign: 'center', padding: '26px 20px 46px' }}>
        <div className="section-title">How it works</div>
        <p className="muted" style={{ maxWidth: 560, margin: '6px auto 0' }}>
          1. Write your instruction in plain words. 2. Review the extracted tasks and fix anything.
          3. Approve to save. 4. Track progress on your dashboard, calendar and board.
        </p>
      </div>
      <footer style={{ borderTop: '1px solid var(--border)', padding: '18px 24px', textAlign: 'center' }} className="muted small">
        TaskPilot AI · Plan smarter. Focus better. Get things done.
      </footer>
    </div>
  );
}
