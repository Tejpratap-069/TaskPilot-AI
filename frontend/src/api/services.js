import { api } from './client';

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
  history: (id) => api(`/tasks/${id}/history`),
  addSubtask: (id, title) => api(`/tasks/${id}/subtasks`, { method: 'POST', body: JSON.stringify({ title }) }),
  toggleSubtask: (id, subId) => api(`/tasks/${id}/subtasks/${subId}/toggle`, { method: 'PATCH' }),
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
