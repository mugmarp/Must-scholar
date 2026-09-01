import { useEffect, useRef } from "react";
import { base44 } from "@/api/base44Client";
import { useToast } from "@/components/ui/use-toast";
import { fireReminder } from "@/lib/alarms";
import { naturalKey } from "@/lib/timetableUtils";

const DAY_MON_IDX = {
  Monday: 0, Tuesday: 1, Wednesday: 2, Thursday: 3,
  Friday: 4, Saturday: 5, Sunday: 6,
};

function nextOccurrence(dayName, timeStr) {
  const now = new Date();
  const target = new Date(now);
  const targetMonIdx = DAY_MON_IDX[dayName];
  if (targetMonIdx == null) return null;
  const nowMonIdx = (now.getDay() + 6) % 7;
  let diff = (targetMonIdx - nowMonIdx + 7) % 7;
  target.setDate(now.getDate() + diff);
  const [h, m] = (timeStr || "0:0").split(":").map(Number);
  target.setHours(h || 0, m || 0, 0, 0);
  if (target.getTime() <= now.getTime()) target.setDate(target.getDate() + 7);
  return target;
}

export function useAlarmScheduler() {
  const { toast } = useToast();
  const fired = useRef(new Set());

  useEffect(() => {
    let active = true;
    const tick = async () => {
      const now = Date.now();
      const programme = localStorage.getItem("must_programme") || "MBR I";
      const targets = [];
      try {
        const [entries, notes, events, assignments] = await Promise.all([
          base44.entities.TimetableEntry.list("-created_date", 2000),
          base44.entities.LectureNote.list("-updated_date", 500),
          base44.entities.CustomEvent.list("-created_date", 500),
          base44.entities.Assignment.list("-created_date", 500),
        ]);
        const myEntries = entries.filter(
          (e) => e.program_group === programme || (e.shared_with || []).includes(programme)
        );
        const noteByKey = new Map();
        notes.forEach((n) => noteByKey.set(n.natural_key, n));
        myEntries.forEach((e) => {
          const n = noteByKey.get(naturalKey(e));
          if (n && n.alarm_minutes != null) {
            const at = nextOccurrence(e.day, e.start_time);
            if (at) targets.push({
              key: `class:${e.id}`,
              title: e.course_title || e.course_code,
              body: `${e.course_code} • ${e.room || "TBA"} • ${e.start_time}`,
              at, lead: n.alarm_minutes,
            });
          }
        });
        events.forEach((ev) => {
          if (ev.alarm_minutes != null) {
            const at = nextOccurrence(ev.day, ev.start_time);
            if (at) {
              const key = ev.repeat_weekly === false ? `event:${ev.id}` : `event:${ev.id}:${at.toISOString()}`;
              targets.push({
                key,
                title: ev.title,
                body: `${ev.location || "Personal event"} • ${ev.start_time}`,
                at, lead: ev.alarm_minutes,
              });
            }
          }
        });
        assignments.forEach((a) => {
          if (a.reminder_minutes != null && a.due_date && !a.completed) {
            const at = new Date(a.due_date);
            targets.push({
              key: `assign:${a.id}`,
              title: a.title,
              body: `Assignment due ${at.toLocaleString("en-GB", { dateStyle: "medium", timeStyle: "short" })}`,
              at, lead: a.reminder_minutes,
            });
          }
        });
      } catch {
        /* ignore */
      }
      if (!active) return;
      targets.forEach((t) => {
        const fireAt = t.at.getTime() - t.lead * 60000;
        const fkey = `${t.key}:${t.at.toISOString()}`;
        if (fireAt <= now && now < t.at.getTime() && !fired.current.has(fkey)) {
          fired.current.add(fkey);
          fireReminder(`⏰ ${t.title}`, t.body);
          toast({ title: `⏰ ${t.title}`, description: t.body });
        }
      });
    };
    tick();
    const id = setInterval(tick, 30000);
    return () => {
      active = false;
      clearInterval(id);
    };
  }, [toast]);
}