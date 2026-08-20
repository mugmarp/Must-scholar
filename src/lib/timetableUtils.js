export const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

export const SESSION_STYLES = {
  THEORY: {
    label: "Theory",
    badge: "bg-blue-600 text-white",
    accent: "bg-blue-500",
    text: "text-blue-600",
  },
  PRACTICAL: {
    label: "Practical",
    badge: "bg-green-600 text-white",
    accent: "bg-green-500",
    text: "text-green-600",
  },
  CLINICAL: {
    label: "Clinical",
    badge: "bg-amber-600 text-white",
    accent: "bg-amber-500",
    text: "text-amber-600",
  },
  WARD: {
    label: "Ward",
    badge: "bg-amber-600 text-white",
    accent: "bg-amber-500",
    text: "text-amber-600",
  },
  DEFAULT: {
    label: "Class",
    badge: "bg-slate-500 text-white",
    accent: "bg-slate-400",
    text: "text-slate-500",
  },
};

export function inferSessionType(entry) {
  if (entry.session_type) return entry.session_type.toUpperCase();
  const room = (entry.room || "").toUpperCase();
  const title = (entry.course_title || "").toUpperCase();
  if (room.includes("LAB")) return "PRACTICAL";
  if (title.includes("CLINICAL") || title.includes("WARD")) return "CLINICAL";
  return "THEORY";
}

export function sessionStyle(entry) {
  const t = inferSessionType(entry);
  return SESSION_STYLES[t] || SESSION_STYLES.DEFAULT;
}

export function naturalKey(entry) {
  return [entry.program_group, entry.course_code, entry.day, entry.start_time].join("|");
}

function timeToMinutes(t) {
  if (!t) return 0;
  const [h, m] = t.split(":").map(Number);
  return h * 60 + (m || 0);
}

export function findNextUp(allEntries, now = new Date()) {
  const nowMinutes = now.getHours() * 60 + now.getMinutes();
  const jsDay = now.getDay();
  const todayIdx = (jsDay + 6) % 7;
  let best = null;
  for (let offset = 0; offset < DAYS.length; offset++) {
    const dayName = DAYS[(todayIdx + offset) % DAYS.length];
    const dayEntries = allEntries
      .filter((e) => e.day === dayName)
      .sort((a, b) => timeToMinutes(a.start_time) - timeToMinutes(b.start_time));
    for (const e of dayEntries) {
      const startMin = timeToMinutes(e.start_time);
      const minutesUntil = startMin + offset * 1440 - nowMinutes;
      if (minutesUntil > 0 && (!best || minutesUntil < best.minutesUntil)) {
        best = { entry: e, minutesUntil };
      }
    }
  }
  return best;
}

export function formatCountdown(minutes) {
  if (minutes <= 0) return "Starting now";
  const d = Math.floor(minutes / 1440);
  const h = Math.floor((minutes % 1440) / 60);
  const m = Math.floor(minutes % 60);
  if (d > 0) return `In ${d}d ${h}h`;
  if (h > 0) return `In ${h}h ${m}m`;
  return `In ${m}m`;
}

export function dedupeShared(entries) {
  const groups = new Map();
  entries.forEach((e) => {
    const key = [e.day, e.start_time, e.end_time, e.course_code, e.room || ""].join("|");
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(e);
  });
  const result = [];
  groups.forEach((dups) => {
    const base = { ...dups[0] };
    if (dups.length > 1) {
      const all = new Set();
      dups.forEach((d) => {
        all.add(d.program_group);
        (d.shared_with || []).forEach((g) => all.add(g));
      });
      base.shared_with = Array.from(all).sort();
    }
    result.push(base);
  });
  return result;
}

export function groupByTimeSlot(entries) {
  const map = {};
  entries.forEach((e) => {
    if (!map[e.start_time]) map[e.start_time] = [];
    map[e.start_time].push(e);
  });
  return Object.entries(map).sort(
    (a, b) => timeToMinutes(a[0]) - timeToMinutes(b[0])
  );
}

export function todayName(now = new Date()) {
  const jsDay = now.getDay();
  return DAYS[(jsDay + 6) % 7];
}