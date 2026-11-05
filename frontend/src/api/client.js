const BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export function getToken() { return localStorage.getItem('tp_token'); }
export function setSession(token, user) {
  localStorage.setItem('tp_token', token);
  localStorage.setItem('tp_user', JSON.stringify(user));
}
export function getStoredUser() {
  try { return JSON.parse(localStorage.getItem('tp_user')); } catch { return null; }
}
export function clearSession() {
  localStorage.removeItem('tp_token');
  localStorage.removeItem('tp_user');
}

export async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  let res;
  try {
    res = await fetch(BASE + path, { ...options, headers });
  } catch (e) {
    throw new Error('Cannot reach the TaskPilot server. Is the backend running on ' + BASE + '?');
  }
  if (res.status === 401) {
    clearSession();
    if (!window.location.pathname.startsWith('/login')) window.location.href = '/login';
    throw new Error('Session expired. Please sign in again.');
  }
  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!res.ok) {
    const msg = (data && data.message) || `Request failed (${res.status})`;
    throw new Error(msg);
  }
  return data;
}
