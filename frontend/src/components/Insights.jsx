import { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Flame, BarChart3 } from 'lucide-react';
import { dashboardService } from '../api/services';
import { EmptyState, fmtDate } from './ui';

// "Completed" segment colour per category, matching the pastel palette
// used on the Tasks list, Kanban board and Calendar.
const CAT_COLORS = { STUDY: '#7cc4ff', PROJECT: '#b7a6ff', PERSONAL: '#ff9de2', WORK: '#ffd66b', HEALTH: '#8be0a4', FINANCE: '#63d68a', OTHER: '#7c6cf6' };

// Insights section for the Dashboard. Every number comes from
// GET /api/dashboard/insights — nothing is hardcoded. If the call fails,
// the section degrades to a small note instead of showing fake data.
export default function Insights() {
  const [data, setData] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let alive = true;
    dashboardService.insights()
      .then(d => { if (alive) setData(d || {}); })
      .catch(() => { if (alive) setFailed(true); });
    return () => { alive = false; };
  }, []);

  if (failed) {
    return (
      <div className="card card-p mt">
        <div className="section-title flex"><BarChart3 size={17} style={{ color: 'var(--teal)' }} /> Insights</div>
        <div className="muted small">Insights unavailable right now — your other dashboard stats are unaffected.</div>
      </div>
    );
  }
  if (!data) return null; // loading: stay quiet rather than flashing placeholders

  const weeklyRaw = Array.isArray(data.completionsByWeek) ? data.completionsByWeek
    : (Array.isArray(data.weeklyCompletions) ? data.weeklyCompletions : []);
  const weekly = weeklyRaw
    .map(w => ({ label: w.weekStart ? fmtDate(w.weekStart).replace(/ \d{4}$/, '') : '', completed: Number(w.completed) || 0 }));
  const categoriesRaw = Array.isArray(data.byCategory) ? data.byCategory
    : (Array.isArray(data.categoryBreakdown) ? data.categoryBreakdown : []);
  const categories = categoriesRaw
    .map(c => ({ category: c.category || 'OTHER', open: Number(c.open) || 0, completed: Number(c.completed) || 0 }));
  const streak = Number(data.currentStreakDays) || 0;
  const maxCat = categories.reduce((m, c) => Math.max(m, c.open + c.completed), 0);

  return (
    <div className="card card-p mt">
      <div className="insights-head">
        <div>
          <div className="section-title flex"><BarChart3 size={17} style={{ color: 'var(--teal)' }} /> Insights</div>
          <div className="muted small">Completion trend and category mix, from your real task data</div>
        </div>
        <span className="streak-pill" title="Days in a row with at least one completed task">
          <Flame size={16} /> {streak}-day streak
        </span>
      </div>

      <div className="grid-2 mt">
        <div>
          <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 2 }}>Completions per week</div>
          <div className="muted small mb">Last {weekly.length || 8} weeks</div>
          {weekly.length === 0 ? <EmptyState text="No completion history yet" sub="Complete a task and your trend will appear here." /> : (
            <ResponsiveContainer width="100%" height={210}>
              <BarChart data={weekly} margin={{ top: 6, right: 6, left: -18, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#26262f" />
                <XAxis dataKey="label" tick={{ fontSize: 11.5, fill: '#9d9cae' }} interval="preserveStartEnd" />
                <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: '#9d9cae' }} />
                <Tooltip cursor={{ fill: 'rgba(255,255,255,0.04)' }} contentStyle={{ background: '#21212b', border: '1px solid #2e2e3a', borderRadius: 10, color: '#f2f1f7' }} labelStyle={{ color: '#f2f1f7' }} itemStyle={{ color: '#f2f1f7' }} />
                <Bar dataKey="completed" name="Completed" fill="#7c6cf6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 2 }}>Categories</div>
          <div className="muted small mb">Completed vs open tasks per category</div>
          {categories.length === 0 ? <EmptyState text="No category data yet" /> : (
            <div className="cat-bars">
              {categories.map(c => {
                const total = c.open + c.completed;
                const donePct = maxCat ? (c.completed / maxCat) * 100 : 0;
                const openPct = maxCat ? (c.open / maxCat) * 100 : 0;
                return (
                  <div key={c.category} className="cat-row">
                    <span style={{ fontWeight: 600 }}>{c.category}</span>
                    <span className="bar">
                      <span className="done-seg" style={{ width: donePct + '%', background: CAT_COLORS[c.category] || 'var(--teal)' }} />
                      <span className="open-seg" style={{ width: openPct + '%' }} />
                    </span>
                    <span className="muted">{c.completed}/{total}</span>
                  </div>
                );
              })}
              <div className="flex small muted" style={{ gap: 14 }}>
                <span className="flex" style={{ gap: 5 }}><span style={{ width: 9, height: 9, borderRadius: 3, background: 'var(--teal)', display: 'inline-block' }} />Completed</span>
                <span className="flex" style={{ gap: 5 }}><span style={{ width: 9, height: 9, borderRadius: 3, background: '#3a3a48', display: 'inline-block' }} />Open</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
