import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import {
  LayoutDashboard, CheckSquare, Sparkles, Sun, CalendarDays, Columns3,
  History, Bell, Settings as SettingsIcon, Timer, LogOut, Compass, Menu, X,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { notificationService } from '../api/services';

const TITLES = {
  '/app': 'Overview', '/app/tasks': 'My Tasks', '/app/capture': 'AI Task Capture',
  '/app/my-day': 'My Day', '/app/calendar': 'Calendar', '/app/board': 'Kanban Board',
  '/app/activity': 'Activity', '/app/notifications': 'Notifications',
  '/app/pomodoro': 'Pomodoro Timer', '/app/settings': 'Settings',
};

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [unread, setUnread] = useState(0);
  const [navOpen, setNavOpen] = useState(false);

  useEffect(() => {
    let alive = true;
    const load = () => notificationService.unreadCount().then(r => { if (alive) setUnread(r.unread || 0); }).catch(() => {});
    load();
    const id = setInterval(load, 30000);
    return () => { alive = false; clearInterval(id); };
  }, [location.pathname]);

  const links = [
    { to: '/app', icon: LayoutDashboard, label: 'Overview', end: true },
    { to: '/app/tasks', icon: CheckSquare, label: 'My Tasks' },
    { to: '/app/capture', icon: Sparkles, label: 'AI Task Capture' },
    { to: '/app/my-day', icon: Sun, label: 'My Day' },
    { to: '/app/calendar', icon: CalendarDays, label: 'Calendar' },
    { to: '/app/board', icon: Columns3, label: 'Kanban' },
    { to: '/app/activity', icon: History, label: 'Activity' },
    { to: '/app/notifications', icon: Bell, label: 'Notifications', count: unread },
    { to: '/app/pomodoro', icon: Timer, label: 'Pomodoro' },
    { to: '/app/settings', icon: SettingsIcon, label: 'Settings' },
  ];

  return (
    <div className="app-shell">
      {navOpen && <div className="nav-overlay" onClick={() => setNavOpen(false)} aria-hidden="true" />}
      <aside className={'sidebar' + (navOpen ? ' open' : '')}>
        <div className="brand">
          <div className="logo-mark"><Compass size={18} /></div>
          <div>
            <b>TaskPilot AI</b>
            <div style={{ fontSize: 11, color: '#83829a' }}>Plan smarter. Focus better.</div>
          </div>
          <button className="icon-btn nav-close" aria-label="Close menu" onClick={() => setNavOpen(false)}>
            <X size={20} />
          </button>
        </div>
        <nav>
          {links.map(l => (
            <NavLink key={l.to} to={l.to} end={l.end} onClick={() => setNavOpen(false)} className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}>
              <l.icon size={17} /> {l.label}
              {l.count > 0 && <span className="count">{l.count}</span>}
            </NavLink>
          ))}
        </nav>
        <div className="user-box">
          <div className="user-profile">
            <div className="avatar">{(user?.name || '?').trim().charAt(0).toUpperCase()}</div>
            <div className="user-meta">
              <div className="name">{user?.name}</div>
              <div className="email">{user?.email}</div>
            </div>
          </div>
          <button className="btn btn-ghost btn-sm" style={{ color: '#b9b8c9', marginTop: 8, paddingLeft: 0 }}
            onClick={() => { setNavOpen(false); logout(); navigate('/login'); }}>
            <LogOut size={15} /> Sign out
          </button>
        </div>
      </aside>
      <div className="main">
        <div className="topbar">
          <button className="icon-btn menu-btn" aria-label="Open menu" onClick={() => setNavOpen(true)}>
            <Menu size={22} />
          </button>
          <div className="page-title">{TITLES[location.pathname] || 'TaskPilot AI'}</div>
          <div className="spacer" />
          <span className="muted small topbar-date">{new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}</span>
        </div>
        <div className="content">
          <div key={location.pathname} className="page-enter"><Outlet /></div>
        </div>
      </div>
    </div>
  );
}
