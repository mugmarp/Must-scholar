import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { base44 } from "@/api/base44Client";
import { StickyNote, BookOpen, MapPin, ArrowLeft } from "lucide-react";
import LectureDetailSheet from "@/components/timetable/LectureDetailSheet";
import { naturalKey } from "@/lib/timetableUtils";

export default function Notes() {
  const navigate = useNavigate();
  const [notes, setNotes] = useState([]);
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const [n, e] = await Promise.all([
          base44.entities.LectureNote.list("-updated_date", 500),
          base44.entities.TimetableEntry.list("-created_date", 2000),
        ]);
        if (active) {
          setNotes(n);
          setEntries(e);
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

  const entryByKey = useMemo(() => {
    const m = new Map();
    entries.forEach((e) => m.set(naturalKey(e), e));
    return m;
  }, [entries]);

  const grouped = useMemo(() => {
    const groups = {};
    notes.forEach((n) => {
      const parts = (n.natural_key || "").split("|");
      const code = parts[1] || "Unknown";
      if (!groups[code]) groups[code] = [];
      groups[code].push(n);
    });
    return Object.entries(groups).sort((a, b) => a[0].localeCompare(b[0]));
  }, [notes]);

  const openNote = (note) => {
    const entry = entryByKey.get(note.natural_key);
    if (entry) {
      setSelected(entry);
    } else {
      const [pg, code, day, start] = (note.natural_key || "").split("|");
      setSelected({
        program_group: pg,
        course_code: code,
        day,
        start_time: start,
        course_title: code,
        time_slot: start,
      });
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-8 h-8 border-4 border-muted border-t-primary rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 space-y-5">
      <div className="flex items-center gap-2">
        <button onClick={() => navigate("/")} className="p-1 -ml-1">
          <ArrowLeft className="w-6 h-6" />
        </button>
        <h1 className="text-xl font-bold">Notes</h1>
      </div>

      {notes.length === 0 ? (
        <div className="py-20 text-center">
          <StickyNote className="w-12 h-12 mx-auto text-muted-foreground/40" />
          <p className="text-sm text-muted-foreground mt-3">
            No notes yet. Tap a class to add notes.
          </p>
        </div>
      ) : (
        grouped.map(([code, items]) => (
          <div key={code} className="space-y-2">
            <p className="text-xs font-bold tracking-wider text-primary">
              {code} • {items.length}
            </p>
            {items.map((n) => {
              const entry = entryByKey.get(n.natural_key);
              const parts = (n.natural_key || "").split("|");
              return (
                <button key={n.id} onClick={() => openNote(n)} className="w-full text-left">
                  <div className="p-3 rounded-xl bg-card border border-border hover:shadow-sm transition">
                    <div className="flex items-center gap-2 text-xs text-muted-foreground mb-1">
                      <BookOpen className="w-3.5 h-3.5" />
                      <span>{parts[2]} {parts[3]}</span>
                      {entry?.room && (
                        <span className="inline-flex items-center gap-1">
                          <MapPin className="w-3 h-3" />
                          {entry.room}
                        </span>
                      )}
                    </div>
                    <p className="text-sm font-medium line-clamp-2">
                      {n.content || (
                        <span className="text-muted-foreground italic">Empty note</span>
                      )}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        ))
      )}

      <LectureDetailSheet entry={selected} onClose={() => setSelected(null)} />
    </div>
  );
}