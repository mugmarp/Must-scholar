import React, { useEffect, useState } from "react";
import {
  Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Trash2, Bell, Repeat } from "lucide-react";
import { base44 } from "@/api/base44Client";
import { useToast } from "@/components/ui/use-toast";
import { DAYS } from "@/lib/timetableUtils";
import { requestNotificationPermission } from "@/lib/alarms";

function toMin(t) {
  if (!t) return null;
  const [h, m] = t.split(":").map(Number);
  return h * 60 + (m || 0);
}

export default function EventSheet({ event, onClose, onSaved }) {
  const { toast } = useToast();
  const open = !!event;
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!event) return;
    if (event.id) {
      setForm({ ...event, repeat_weekly: event.repeat_weekly !== false });
    } else {
      setForm({
        title: "",
        day: event.day || "Monday",
        start_time: event.start_time || "08:00",
        end_time: event.end_time || "",
        location: event.location || "",
        notes: event.notes || "",
        repeat_weekly: event.repeat_weekly !== false,
        alarm_minutes: null,
      });
    }
  }, [event]);

  if (!event || !form) return null;
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const isNew = !event.id;

  const checkConflict = async () => {
    const programme = localStorage.getItem("must_programme") || "MBR I";
    try {
      const entries = await base44.entities.TimetableEntry.list("-created_date", 2000);
      const evS = toMin(form.start_time);
      const evE = form.end_time ? toMin(form.end_time) : evS + 60;
      return entries
        .filter(
          (e) =>
            (e.program_group === programme ||
              (e.shared_with || []).includes(programme)) &&
            e.day === form.day
        )
        .filter((e) => {
          const cs = toMin(e.start_time);
          const ce = toMin(e.end_time) || cs + 60;
          return evS < ce && cs < evE;
        });
    } catch {
      return [];
    }
  };

  const save = async () => {
    if (!form.title.trim()) {
      toast({ title: "Title is required", variant: "destructive" });
      return;
    }
    setSaving(true);
    try {
      const conflicts = await checkConflict();
      if (conflicts.length > 0) {
        const c = conflicts[0];
        const ok = window.confirm(
          `This overlaps with ${c.course_code} (${c.start_time}${c.end_time ? "–" + c.end_time : ""}). Save anyway?`
        );
        if (!ok) {
          setSaving(false);
          return;
        }
      }
      if (form.alarm_minutes != null) requestNotificationPermission();
      const payload = {
        title: form.title.trim(),
        day: form.day,
        start_time: form.start_time,
        end_time: form.end_time || null,
        location: form.location || null,
        notes: form.notes || "",
        repeat_weekly: form.repeat_weekly,
        alarm_minutes: form.alarm_minutes,
      };
      if (isNew) await base44.entities.CustomEvent.create(payload);
      else await base44.entities.CustomEvent.update(event.id, payload);
      toast({ title: isNew ? "Event added" : "Event updated" });
      onSaved?.();
      onClose();
    } catch (e) {
      toast({ title: "Save failed", description: e.message, variant: "destructive" });
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    if (!window.confirm("Delete this event?")) return;
    try {
      await base44.entities.CustomEvent.delete(event.id);
      toast({ title: "Event deleted" });
      onSaved?.();
      onClose();
    } catch (e) {
      toast({ title: "Delete failed", description: e.message, variant: "destructive" });
    }
  };

  return (
    <Sheet open={open} onOpenChange={(o) => { if (!o) onClose(); }}>
      <SheetContent className="overflow-y-auto">
        <SheetHeader>
          <SheetTitle>{isNew ? "Add event" : "Edit event"}</SheetTitle>
          <SheetDescription>Personal study events appear in your timetable.</SheetDescription>
        </SheetHeader>
        <div className="px-4 space-y-4 pb-8">
          <div>
            <Label>Title</Label>
            <Input
              value={form.title}
              onChange={(e) => set("title", e.target.value)}
              placeholder="e.g. Group study, Catch-up..."
              className="mt-1"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Day</Label>
              <Select value={form.day} onValueChange={(v) => set("day", v)}>
                <SelectTrigger className="mt-1"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {DAYS.map((d) => <SelectItem key={d} value={d}>{d}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label>Start</Label>
              <Input type="time" value={form.start_time} onChange={(e) => set("start_time", e.target.value)} className="mt-1" />
            </div>
            <div className="col-span-2">
              <Label>End (optional)</Label>
              <Input type="time" value={form.end_time} onChange={(e) => set("end_time", e.target.value)} className="mt-1" />
            </div>
          </div>
          <div>
            <Label>Location (optional)</Label>
            <Input value={form.location} onChange={(e) => set("location", e.target.value)} placeholder="Room / venue" className="mt-1" />
          </div>
          <div className="flex items-center justify-between p-3 rounded-xl bg-secondary/50">
            <div className="flex items-center gap-2">
              <Repeat className="w-4 h-4 text-muted-foreground" />
              <div>
                <p className="text-sm font-medium">Repeats weekly</p>
                <p className="text-xs text-muted-foreground">{form.repeat_weekly ? "Every week" : "One-time"}</p>
              </div>
            </div>
            <Switch checked={form.repeat_weekly} onCheckedChange={(v) => set("repeat_weekly", v)} />
          </div>
          <div>
            <h4 className="text-sm font-semibold mb-2 flex items-center gap-1.5"><Bell className="w-4 h-4" /> Reminder</h4>
            <div className="flex gap-2 flex-wrap">
              {[15, 30, 60].map((m) => (
                <Button key={m} size="sm" variant={form.alarm_minutes === m ? "default" : "outline"} onClick={() => set("alarm_minutes", m)}>
                  {m}m
                </Button>
              ))}
              <Button size="sm" variant={form.alarm_minutes == null ? "default" : "outline"} onClick={() => set("alarm_minutes", null)}>
                Off
              </Button>
            </div>
          </div>
          <div>
            <Label>Notes</Label>
            <Textarea value={form.notes} onChange={(e) => set("notes", e.target.value)} rows={3} className="mt-1" />
          </div>
          <div className="flex gap-2 pt-1">
            <Button className="flex-1" onClick={save} disabled={saving}>{saving ? "Saving..." : "Save event"}</Button>
            {!isNew && (
              <Button variant="destructive" size="icon" onClick={remove}>
                <Trash2 className="w-4 h-4" />
              </Button>
            )}
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
}