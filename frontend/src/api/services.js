import { api, getToken } from './client';

const BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Raw fetch for the calls api() cannot make: multipart uploads (api() forces a
// JSON Content-Type) and binary downloads. Same base URL and JWT as api().
async function authedFetch(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  let res;
  try {
    res = await fetch(BASE + path, { ...options, headers });
  } catch (e) {
    throw new Error('Cannot reach the TaskPilot server right now. Please try again in a moment.');
  }
  if (!res.ok) {
    let msg = `Request failed (${res.status})`;
    try { const d = await res.json(); if (d && d.message) msg = d.message; } catch { /* keep default */ }
    if (res.status === 413 || /too large|size|exceed/i.test(msg)) {
      msg = 'That file is too large — attachments are limited to 2 MB.';
    }
    throw new Error(msg);
  }
  return res;
}

export const authService = {
  register: (name, email, password) => api('/auth/register', { method: 'POST', body: JSON.stringify({ name, email, password }) }),
  login: (email, password) => api('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  me: () => api('/auth/me'),
};

export const taskService = {
  list: (params = {}) => {
    const q = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') q.set(k, v); });
    return api('/tasks?' + q.toString());
  },
  get: (id) => api('/tasks/' + id),
  create: (task) => api('/tasks', { method: 'POST', body: JSON.stringify(task) }),
  update: (id, task) => api('/tasks/' + id, { method: 'PUT', body: JSON.stringify(task) }),
  setStatus: (id, status) => api(`/tasks/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  setPriority: (id, priority) => api(`/tasks/${id}/priority`, { method: 'PATCH', body: JSON.stringify({ priority }) }),
  remove: (id) => api('/tasks/' + id, { method: 'DELETE' }),
  trash: () => api('/tasks/trash'),
  restore: (id) => api(`/tasks/${id}/restore`, { method: 'POST' }),
  removePermanent: (id) => api(`/tasks/${id}/permanent`, { method: 'DELETE' }),
  history: (id) => api(`/tasks/${id}/history`),
  addSubtask: (id, title) => api(`/tasks/${id}/subtasks`, { method: 'POST', body: JSON.stringify({ title }) }),
  toggleSubtask: (id, subId) => api(`/tasks/${id}/subtasks/${subId}/toggle`, { method: 'PATCH' }),
};

export const attachmentService = {
  list: (taskId) => api(`/tasks/${taskId}/attachments`),
  upload: async (taskId, file) => {
    const form = new FormData();
    form.append('file', file);
    const res = await authedFetch(`/tasks/${taskId}/attachments`, { method: 'POST', body: form });
    return res.json();
  },
  // Content requires the Authorization header, so callers must fetch a blob
  // and use URL.createObjectURL — a plain <img src> would be unauthenticated.
  content: async (attachmentId) => {
    const res = await authedFetch(`/attachments/${attachmentId}/content`);
    return res.blob();
  },
  remove: (attachmentId) => api(`/attachments/${attachmentId}`, { method: 'DELETE' }),
};

export const aiService = {
  parse: (text) => api('/ai/parse-tasks', { method: 'POST', body: JSON.stringify({ text }) }),
  prioritize: (tasks) => api('/ai/prioritize', { method: 'POST', body: JSON.stringify({ tasks }) }),
};

export const dashboardService = {
  summary: () => api('/dashboard/summary'),
  weekly: () => api('/dashboard/weekly'),
  upcoming: (days = 7) => api('/dashboard/upcoming?days=' + days),
  breakdown: () => api('/dashboard/breakdown'),
  focus: () => api('/dashboard/focus'),
  recentActivity: (limit = 8) => api('/dashboard/recent-activity?limit=' + limit),
  insights: () => api('/dashboard/insights'),
};

export const plannerService = {
  today: () => api('/planner/today'),
  upcoming: (days = 14) => api('/planner/upcoming?days=' + days),
};

export const activityService = {
  list: (limit = 60) => api('/activity?limit=' + limit),
};

export const notificationService = {
  list: () => api('/notifications'),
  unreadCount: () => api('/notifications/unread-count'),
  markRead: (id) => api(`/notifications/${id}/read`, { method: 'PATCH' }),
};

export const preferenceService = {
  get: () => api('/preferences'),
  update: (prefs) => api('/preferences', { method: 'PUT', body: JSON.stringify(prefs) }),
};
