import React, { useEffect, useMemo, useRef, useState } from "react";
import { base44 } from "@/api/base44Client";
import ProgrammeSelector from "@/components/timetable/ProgrammeSelector";
import NextUpCard from "@/components/timetable/NextUpCard";
import DaySelector from "@/components/timetable/DaySelector";
import LectureCard from "@/components/timetable/LectureCard";
import EventCard from "@/components/timetable/EventCard";
import LectureDetailSheet from "@/components/timetable/LectureDetailSheet";
import EventSheet from "@/components/timetable/EventSheet";
import { DAYS, findNextUp, todayName, dedupeShared } from "@/lib/timetableUtils";
import { useToast } from "@/components/ui/use-toast";
import { Plus } from "lucide-react";

export default function Home() {
  const [entries, setEntries] = useState([]);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [programme, setProgramme] = useState(
    () => localStorage.getItem("must_programme") || "MBR I"
  );
  const [selectedDay, setSelectedDay] = useState(() => todayName());
  const [selectedEntry, setSelectedEntry] = useState(null);
  const [editingEvent, setEditingEvent] = useState(null);
  const [now, setNow] = useState(new Date());
  const [syncing, setSyncing] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    localStorage.setItem("must_programme", programme);
  }, [programme]);

  const loadAll = async () => {
    const [list, evs] = await Promise.all([
      base44.entities.TimetableEntry.list("-created_date", 2000),
      base44.entities.CustomEvent.list("-created_date", 500),
    ]);
    setEntries(list);
    setEvents(evs);
  };

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const [list, evs] = await Promise.all([
          base44.entities.TimetableEntry.list("-created_date", 2000),
          base44.entities.CustomEvent.list("-created_date", 500),
        ]);
        if (active) {
          setEntries(list);
          setEvents(evs);
        }
      } catch {
        /* ignore */
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 30000);
    return () => clearInterval(t);
  }, []);

  const programmes = useMemo(() => {
    const set = new Set();
    entries.forEach((e) => set.add(e.program_group));
    return Array.from(set).sort();
  }, [entries]);

  const myEntries = useMemo(() => {
    const mine = entries.filter(
      (e) =>
        e.program_group === programme ||
        (e.shared_with || []).includes(programme)
    );
    return dedupeShared(mine);
  }, [entries, programme]);

  const dayItems = useMemo(() => {
    const lectures = myEntries
      .filter((e) => e.day === selectedDay)
      .map((e) => ({ ...e, _kind: "lecture" }));
    const dayEvents = events
      .filter((e) => e.day === selectedDay)
      .map((e) => ({ ...e, _kind: "event" }));
    return [...lectures, ...dayEvents].sort((a, b) =>
      (a.start_time || "").localeCompare(b.start_time || "")
    );
  }, [myEntries, events, selectedDay]);

  const nextUp = useMemo(() => findNextUp(myEntries, now), [myEntries, now]);

  const handleSync = async () => {
    setSyncing(true);
    try {
      const res = await base44.functions.invoke("refreshTimetable", {});
      const when = new Date().toLocaleString("en-GB", {
        dateStyle: "medium",
        timeStyle: "short",
      });
      localStorage.setItem("must_last_synced", when);
      await loadAll();
      toast({ title: `Timetable updated • ${res.data.imported} classes` });
    } catch (e) {
      toast({ title: "Sync failed", description: e.message, variant: "destructive" });
    } finally {
      setSyncing(false);
    }
  };

  const touchStartX = useRef(null);
  const onTouchStart = (e) => {
    touchStartX.current = e.touches[0].clientX;
  };
  const onTouchEnd = (e) => {
    if (touchStartX.current == null) return;
    const dx = e.changedTouches[0].clientX - touchStartX.current;
    if (Math.abs(dx) > 50) {
      const idx = DAYS.indexOf(selectedDay);
      if (dx < 0 && idx < DAYS.length - 1) setSelectedDay(DAYS[idx + 1]);
      else if (dx > 0 && idx > 0) setSelectedDay(DAYS[idx - 1]);
    }
    touchStartX.current = null;
  };

  const hour = now.getHours();
  const greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="w-8 h-8 border-4 border-muted border-t-primary rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 space-y-5">
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-primary/15 flex items-center justify-center text-primary font-bold">
            M
          </div>
          <div>
            <p className="text-sm font-semibold">{greeting}</p>
            <p className="text-xs text-muted-foreground">{programme}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setEditingEvent({ day: selectedDay })}
            className="flex items-center justify-center w-9 h-9 rounded-full bg-secondary text-secondary-foreground"
          >
            <Plus className="w-5 h-5" />
          </button>
          <button
            onClick={handleSync}
            disabled={syncing}
            className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-full bg-green-100 text-green-700"
          >
            <span className="w-1.5 h-1.5 rounded-full bg-green-500" />
            {syncing ? "Syncing" : "Synced"}
          </button>
        </div>
      </div>

      <h1 className="text-2xl font-bold">Your Schedule Today</h1>

      <ProgrammeSelector programmes={programmes} value={programme} onChange={setProgramme} />

      {nextUp && nextUp.entry && (
        <NextUpCard nextUp={nextUp} onSelect={setSelectedEntry} />
      )}

      <DaySelector
        selected={selectedDay}
        onSelect={setSelectedDay}
        onQuickAdd={(day) => setEditingEvent({ day })}
        now={now}
      />

      <div onTouchStart={onTouchStart} onTouchEnd={onTouchEnd}>
        {dayItems.length === 0 ? (
          <div className="py-16 text-center text-sm text-muted-foreground">
            No classes or events for this day.
          </div>
        ) : (
          <div className="space-y-3">
            {dayItems.map((item) =>
              item._kind === "event" ? (
                <EventCard key={`ev-${item.id}`} event={item} onSelect={setEditingEvent} />
              ) : (
                <LectureCard key={`lc-${item.id}`} entry={item} onSelect={setSelectedEntry} />
              )
            )}
          </div>
        )}
      </div>

      <LectureDetailSheet
        entry={selectedEntry}
        onClose={() => setSelectedEntry(null)}
      />
      <EventSheet
        event={editingEvent}
        onClose={() => setEditingEvent(null)}
        onSaved={loadAll}
      />
    </div>
  );
}