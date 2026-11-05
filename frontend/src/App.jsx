import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import Landing from './pages/Landing';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import AiCapture from './pages/AiCapture';
import Tasks from './pages/Tasks';
import MyDay from './pages/MyDay';
import CalendarPage from './pages/CalendarPage';
import KanbanPage from './pages/KanbanPage';
import ActivityPage from './pages/ActivityPage';
import NotificationsPage from './pages/NotificationsPage';
import SettingsPage from './pages/SettingsPage';
import PomodoroPage from './pages/PomodoroPage';

function Protected({ children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return children;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/app" element={<Protected><Layout /></Protected>}>
            <Route index element={<Dashboard />} />
            <Route path="tasks" element={<Tasks />} />
            <Route path="capture" element={<AiCapture />} />
            <Route path="my-day" element={<MyDay />} />
            <Route path="calendar" element={<CalendarPage />} />
            <Route path="board" element={<KanbanPage />} />
            <Route path="activity" element={<ActivityPage />} />
            <Route path="notifications" element={<NotificationsPage />} />
            <Route path="pomodoro" element={<PomodoroPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
