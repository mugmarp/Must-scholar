import React, { useEffect, useState } from "react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/use-toast";
import { MapPin, User, Clock, Bell } from "lucide-react";
import { base44 } from "@/api/base44Client";
import { naturalKey, sessionStyle } from "@/lib/timetableUtils";

export default function LectureDetailSheet({ entry, onClose }) {
  const { toast } = useToast();
  const open = !!entry;
  const [note, setNote] = useState("");
  const [alarm, setAlarm] = useState(null);
  const [noteId, setNoteId] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!entry) return;
    const key = naturalKey(entry);
    let active = true;
    setNote("");
    setAlarm(null);
    setNoteId(null);
    (async () => {
      try {
        const found = await base44.entities.LectureNote.filter({ natural_key: key });
        if (active && found && found.length > 0) {
          setNote(found[0].content || "");
          setAlarm(found[0].alarm_minutes ?? null);
          setNoteId(found[0].id);
        }
      } catch {
        /* ignore */
      }
    })();
    return () => {
      active = false;
    };
  }, [entry]);

  if (!entry) return null;
  const style = sessionStyle(entry);
  const key = naturalKey(entry);

  const saveNote = async () => {
    setSaving(true);
    try {
      if (noteId) {
        await base44.entities.LectureNote.update(noteId, {
          content: note,
          alarm_minutes: alarm,
        });
      } else {
        const created = await base44.entities.LectureNote.create({
          natural_key: key,
          content: note,
          alarm_minutes: alarm,
        });
        if (created) setNoteId(created.id);
      }
      toast({ title: "Note saved" });
    } catch (e) {
      toast({ title: "Save failed", description: e.message, variant: "destructive" });
    } finally {
      setSaving(false);
    }
  };

  const toggleAlarm = async (mins) => {
    const next = alarm === mins ? null : mins;
    setAlarm(next);
    try {
      if (noteId) {
        await base44.entities.LectureNote.update(noteId, { alarm_minutes: next });
      } else {
        const created = await base44.entities.LectureNote.create({
          natural_key: key,
          content: "",
          alarm_minutes: next,
        });
        if (created) setNoteId(created.id);
      }
      toast({
        title: next ? `Reminder set for ${next}m before` : "Reminder turned off",
      });
    } catch (e) {
      toast({ title: "Failed", description: e.message, variant: "destructive" });
    }
  };

  return (
    <Sheet
      open={open}
      onOpenChange={(o) => {
        if (!o) onClose();
      }}
    >
      <SheetContent className="overflow-y-auto">
        <SheetHeader>
          <SheetTitle>{entry.course_title}</SheetTitle>
          <SheetDescription>
            {entry.course_code} • {entry.day} {entry.time_slot}
          </SheetDescription>
        </SheetHeader>

        <div className="px-4 space-y-5 pb-8">
          <span className={`inline-block text-xs px-2 py-0.5 rounded-full ${style.badge}`}>
            {style.label}
          </span>

          <div className="space-y-2.5 text-sm">
            <DetailRow icon={MapPin} label="Room" value={entry.room} />
            <DetailRow icon={User} label="Lecturer" value={entry.lecturer || "Not assigned"} />
            <DetailRow icon={Clock} label="Time" value={`${entry.start_time} – ${entry.end_time}`} />
            {entry.shared_with && entry.shared_with.length > 0 && (
              <p className="text-xs text-muted-foreground pt-1">
                Shared with: {entry.shared_with.join(", ")}
              </p>
            )}
          </div>

          <div>
            <h4 className="text-sm font-semibold mb-2 flex items-center gap-1.5">
              <Bell className="w-4 h-4" /> Reminders
            </h4>
            <div className="flex gap-2">
              {[15, 30, 60].map((m) => (
                <Button
                  key={m}
                  size="sm"
                  variant={alarm === m ? "default" : "outline"}
                  onClick={() => toggleAlarm(m)}
                >
                  {m}m before
                </Button>
              ))}
            </div>
            <p className="text-[11px] text-muted-foreground mt-1.5">
              Reminders fire while the app is open (web build).
            </p>
          </div>

          <div>
            <h4 className="text-sm font-semibold mb-2">Personal notes</h4>
            <Textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Add notes for this lecture..."
              rows={4}
            />
            <p className="text-[10px] text-muted-foreground mt-1 break-all">
              Linked by: {key}
            </p>
            <Button className="mt-2" size="sm" onClick={saveNote} disabled={saving}>
              {saving ? "Saving..." : "Save note"}
            </Button>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
}

function DetailRow({ icon: Icon, label, value }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-muted-foreground inline-flex items-center gap-1.5">
        <Icon className="w-4 h-4" /> {label}
      </span>
      <span className="font-medium text-right">{value}</span>
    </div>
  );
}