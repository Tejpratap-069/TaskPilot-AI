import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Compass } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setBusy(true); setError('');
    try { await register(name, email, password); navigate('/app'); }
    catch (err) { setError(err.message); setBusy(false); }
  }

  return (
    <div className="auth-wrap">
      <div className="card auth-card">
        <div className="flex" style={{ marginBottom: 18 }}>
          <div style={{ width: 34, height: 34, borderRadius: 8, background: 'var(--teal)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}><Compass size={19} /></div>
          <div>
            <div style={{ fontWeight: 800, fontSize: 17 }}>TaskPilot AI</div>
            <div className="small muted">Create your account</div>
          </div>
        </div>
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={submit}>
          <div className="field"><label>Full name</label>
            <input className="input" required value={name} onChange={e => setName(e.target.value)} placeholder="Your name" /></div>
          <div className="field"><label>Email</label>
            <input className="input" type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com" /></div>
          <div className="field"><label>Password</label>
            <input className="input" type="password" required minLength={6} value={password} onChange={e => setPassword(e.target.value)} placeholder="At least 6 characters" /></div>
          <button className="btn btn-primary" style={{ width: '100%' }} disabled={busy}>{busy ? 'Creating account...' : 'Create account'}</button>
        </form>
        <p className="small muted" style={{ textAlign: 'center', marginTop: 16 }}>
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
