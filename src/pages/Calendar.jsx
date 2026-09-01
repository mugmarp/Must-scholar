import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { base44 } from "@/api/base44Client";
import {
  ArrowLeft, ChevronLeft, ChevronRight, BookOpen, CalendarPlus, ClipboardList,
} from "lucide-react";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import LectureDetailSheet from "@/components/timetable/LectureDetailSheet";
import EventSheet from "@/components/timetable/EventSheet";
import AssignmentSheet from "@/components/tasks/AssignmentSheet";
import { DAYS, todayName, dedupeShared } from "@/lib/timetableUtils";

const MONTHS = ["January","February","March","April","May","June","July","August","September","October","November","December"];

function toMin(t) {
  if (!t) return null;
  const [h, m] = t.split(":").map(Number);
  return h * 60 + (m || 0);
}

export default function Calendar() {
  const navigate = useNavigate();
  const [cursor, setCursor] = useState(() => {
    const d = new Date();
    d.setDate(1);
    return d;
  });
  const [entries, setEntries] = useState([]);
  const [events, setEvents] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedLecture, setSelectedLecture] = useState(null);
  const [editingEvent, setEditingEvent] = useState(null);
  const [editingAssignment, setEditingAssignment] = useState(null);

  const loadAll = async () => {
    const [e, ev, a] = await Promise.all([
      base44.entities.TimetableEntry.list("-created_date", 2000),
      base44.entities.CustomEvent.list("-created_date", 500),
      base44.entities.Assignment.list("-created_date", 500),
    ]);
    setEntries(e);
    setEvents(ev);
    setAssignments(a);
  };

  useEffect(() => {
    (async () => {
      try {
        await loadAll();
      } catch {
        /* ignore */
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const programme = localStorage.getItem("must_programme") || "MBR I";
  const myEntries = useMemo(
    () =>
      dedupeShared(
        entries.filter(
          (e) => e.program_group === programme || (e.shared_with || []).includes(programme)
        )
      ),
    [entries, programme]
  );

  const classesByDay = useMemo(() => {
    const m = {};
    DAYS.forEach((d) => (m[d] = []));
    myEntries.forEach((e) => (m[e.day] = m[e.day] || []).push(e));
    return m;
  }, [myEntries]);

  const eventsByDay = useMemo(() => {
    const m = {};
    DAYS.forEach((d) => (m[d] = []));
    events.forEach((e) => (m[e.day] = m[e.day] || []).push(e));
    return m;
  }, [events]);

  const deadlinesByDate = useMemo(() => {
    const m = {};
    assignments.forEach((a) => {
      if (a.due_date && !a.completed) {
        const key = new Date(a.due_date).toDateString();
        (m[key] = m[key] || []).push(a);
      }
    });
    return m;
  }, [assignments]);

  const grid = useMemo(() => {
    const year = cursor.getFullYear();
    const month = cursor.getMonth();
    const firstDayJs = new Date(year, month, 1).getDay();
    const startOffset = (firstDayJs + 6) % 7;
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const cells = [];
    for (let i = 0; i < startOffset; i++) cells.push(null);
    for (let d = 1; d <= daysInMonth; d++) cells.push(new Date(year, month, d));
    return cells;
  }, [cursor]);

  const todayStr = new Date().toDateString();

  const selectedItems = useMemo(() => {
    if (!selectedDate) return null;
    const dayName = todayName(selectedDate);
    const classes = (classesByDay[dayName] || [])
      .slice()
      .sort((a, b) => toMin(a.start_time) - toMin(b.start_time));
    const evs = (eventsByDay[dayName] || [])
      .slice()
      .sort((a, b) => toMin(a.start_time) - toMin(b.start_time));
    const dl = (deadlinesByDate[selectedDate.toDateString()] || [])
      .slice()
      .sort((a, b) => new Date(a.due_date) - new Date(b.due_date));
    return { classes, events: evs, deadlines: dl };
  }, [selectedDate, classesByDay, eventsByDay, deadlinesByDate]);

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 space-y-5">
      <div className="flex items-center gap-2">
        <button onClick={() => navigate("/")} className="p-1 -ml-1">
          <ArrowLeft className="w-6 h-6" />
        </button>
        <h1 className="text-xl font-bold">Calendar</h1>
      </div>

      <div className="flex items-center justify-between">
        <button onClick={() => setCursor((c) => new Date(c.getFullYear(), c.getMonth() - 1, 1))}>
          <ChevronLeft className="w-5 h-5" />
        </button>
        <p className="font-semibold">{MONTHS[cursor.getMonth()]} {cursor.getFullYear()}</p>
        <button onClick={() => setCursor((c) => new Date(c.getFullYear(), c.getMonth() + 1, 1))}>
          <ChevronRight className="w-5 h-5" />
        </button>
      </div>

      <div className="grid grid-cols-7 gap-1 text-center text-[11px] font-semibold text-muted-foreground">
        {DAYS.map((d) => <div key={d}>{d.slice(0, 2)}</div>)}
      </div>
      <div className="grid grid-cols-7 gap-1">
        {grid.map((date, i) => {
          if (!date) return <div key={i} />;
          const ds = date.toDateString();
          const dayName = todayName(date);
          const hasClasses = (classesByDay[dayName] || []).length > 0;
          const hasEvents = (eventsByDay[dayName] || []).length > 0;
          const hasDeadlines = !!(deadlinesByDate[ds] || []).length;
          const isToday = ds === todayStr;
          return (
            <button
              key={i}
              onClick={() => setSelectedDate(date)}
              className={`aspect-square rounded-xl p-1.5 flex flex-col items-center justify-start gap-1 border ${
                isToday ? "border-primary" : "border-transparent"
              } bg-secondary/40`}
            >
              <span className={`text-xs font-semibold ${isToday ? "text-primary" : ""}`}>
                {date.getDate()}
              </span>
              <div className="flex gap-0.5">
                {hasClasses && <span className="w-1.5 h-1.5 rounded-full bg-blue-500" />}
                {hasEvents && <span className="w-1.5 h-1.5 rounded-full bg-purple-500" />}
                {hasDeadlines && <span className="w-1.5 h-1.5 rounded-full bg-red-500" />}
              </div>
            </button>
          );
        })}
      </div>

      <div className="flex items-center gap-4 text-xs text-muted-foreground">
        <span className="inline-flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-blue-500" /> Classes</span>
        <span className="inline-flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-purple-500" /> Events</span>
        <span className="inline-flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-red-500" /> Deadlines</span>
      </div>

      <Sheet open={!!selectedDate} onOpenChange={(o) => { if (!o) setSelectedDate(null); }}>
        <SheetContent className="overflow-y-auto">
          <SheetHeader>
            <SheetTitle>
              {selectedDate
                ? selectedDate.toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long" })
                : ""}
            </SheetTitle>
          </SheetHeader>
          <div className="px-4 space-y-3 pb-8">
            {selectedItems &&
              selectedItems.classes.length === 0 &&
              selectedItems.events.length === 0 &&
              selectedItems.deadlines.length === 0 && (
                <p className="text-sm text-muted-foreground py-8 text-center">Nothing scheduled.</p>
              )}
            {selectedItems?.classes.map((c) => (
              <button
                key={c.id}
                onClick={() => setSelectedLecture(c)}
                className="w-full text-left p-3 rounded-xl bg-card border border-border"
              >
                <div className="flex items-center gap-2 text-xs">
                  <BookOpen className="w-3.5 h-3.5 text-blue-500" />
                  <span className="font-bold">{c.course_code}</span>
                  <span className="text-muted-foreground">{c.start_time}–{c.end_time}</span>
                </div>
                <p className="text-sm font-medium mt-0.5 line-clamp-1">{c.course_title}</p>
                {c.room && <p className="text-xs text-muted-foreground">{c.room}</p>}
              </button>
            ))}
            {selectedItems?.events.map((ev) => (
              <button
                key={ev.id}
                onClick={() => setEditingEvent(ev)}
                className="w-full text-left p-3 rounded-xl bg-purple-50 dark:bg-purple-950/30 border border-dashed border-purple-300"
              >
                <div className="flex items-center gap-2 text-xs">
                  <CalendarPlus className="w-3.5 h-3.5 text-purple-600" />
                  <span className="font-bold text-purple-600">{ev.start_time}</span>
                </div>
                <p className="text-sm font-medium mt-0.5 line-clamp-1">{ev.title}</p>
                {ev.location && <p className="text-xs text-muted-foreground">{ev.location}</p>}
              </button>
            ))}
            {selectedItems?.deadlines.map((a) => (
              <button
                key={a.id}
                onClick={() => setEditingAssignment(a)}
                className="w-full text-left p-3 rounded-xl bg-red-50 dark:bg-red-950/30 border border-red-200"
              >
                <div className="flex items-center gap-2 text-xs">
                  <ClipboardList className="w-3.5 h-3.5 text-red-600" />
                  <span className="font-bold text-red-600">
                    {new Date(a.due_date).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}
                  </span>
                </div>
                <p className="text-sm font-medium mt-0.5 line-clamp-1">{a.title}</p>
                {a.course_code && <p className="text-xs text-muted-foreground">{a.course_code}</p>}
              </button>
            ))}
          </div>
        </SheetContent>
      </Sheet>

      <LectureDetailSheet entry={selectedLecture} onClose={() => setSelectedLecture(null)} />
      <EventSheet event={editingEvent} onClose={() => setEditingEvent(null)} onSaved={loadAll} />
      <AssignmentSheet assignment={editingAssignment} onClose={() => setEditingAssignment(null)} onSaved={loadAll} />
    </div>
  );
}