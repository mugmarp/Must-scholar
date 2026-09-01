import React, { useEffect, useState } from "react";
import {
  Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Trash2 } from "lucide-react";
import { base44 } from "@/api/base44Client";
import { useToast } from "@/components/ui/use-toast";
import { requestNotificationPermission } from "@/lib/alarms";

const REMINDERS = [
  { value: "off", label: "Off" },
  { value: "15", label: "15 min before" },
  { value: "30", label: "30 min before" },
  { value: "60", label: "1 hour before" },
  { value: "120", label: "2 hours before" },
  { value: "1440", label: "1 day before" },
];
const PRIORITIES = [
  { value: "low", label: "Low" },
  { value: "medium", label: "Medium" },
  { value: "high", label: "High" },
];

function toLocalInput(d) {
  if (!d) return "";
  const date = new Date(d);
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export default function AssignmentSheet({ assignment, onClose, onSaved }) {
  const { toast } = useToast();
  const open = !!assignment;
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!assignment) return;
    if (assignment.id) {
      setForm({
        title: assignment.title || "",
        course_code: assignment.course_code || "",
        due_date: assignment.due_date ? toLocalInput(assignment.due_date) : "",
        reminder_minutes: assignment.reminder_minutes != null ? String(assignment.reminder_minutes) : "off",
        priority: assignment.priority || "medium",
        notes: assignment.notes || "",
        completed: !!assignment.completed,
      });
    } else {
      setForm({
        title: "",
        course_code: "",
        due_date: "",
        reminder_minutes: "off",
        priority: "medium",
        notes: "",
        completed: false,
      });
    }
  }, [assignment]);

  if (!assignment || !form) return null;
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const isNew = !assignment.id;

  const save = async () => {
    if (!form.title.trim()) {
      toast({ title: "Title is required", variant: "destructive" });
      return;
    }
    setSaving(true);
    try {
      const reminder = form.reminder_minutes === "off" ? null : Number(form.reminder_minutes);
      const due = form.due_date ? new Date(form.due_date).toISOString() : null;
      const payload = {
        title: form.title.trim(),
        course_code: form.course_code || null,
        due_date: due,
        reminder_minutes: reminder,
        priority: form.priority,
        notes: form.notes || "",
        completed: form.completed,
      };
      if (reminder != null) requestNotificationPermission();
      if (isNew) await base44.entities.Assignment.create(payload);
      else await base44.entities.Assignment.update(assignment.id, payload);
      toast({ title: isNew ? "Task added" : "Task updated" });
      onSaved?.();
      onClose();
    } catch (e) {
      toast({ title: "Save failed", description: e.message, variant: "destructive" });
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    if (!window.confirm("Delete this task?")) return;
    try {
      await base44.entities.Assignment.delete(assignment.id);
      toast({ title: "Task deleted" });
      onSaved?.();
      onClose();
    } catch (e) {
      toast({ title: "Delete failed", description: e.message, variant: "destructive" });
    }
  };

  const toggleComplete = async () => {
    const next = !form.completed;
    set("completed", next);
    if (!isNew) {
      try {
        await base44.entities.Assignment.update(assignment.id, { completed: next });
        onSaved?.();
      } catch {
        /* ignore */
      }
    }
  };

  return (
    <Sheet open={open} onOpenChange={(o) => { if (!o) onClose(); }}>
      <SheetContent className="overflow-y-auto">
        <SheetHeader>
          <SheetTitle>{isNew ? "New task" : "Edit task"}</SheetTitle>
          <SheetDescription>Assignments, deadlines and to-dos with reminders.</SheetDescription>
        </SheetHeader>
        <div className="px-4 space-y-4 pb-8">
          <div>
            <Label>Title</Label>
            <Input
              value={form.title}
              onChange={(e) => set("title", e.target.value)}
              placeholder="e.g. Submit Bio lab report"
              className="mt-1"
            />
          </div>
          <div>
            <Label>Course code (optional)</Label>
            <Input
              value={form.course_code}
              onChange={(e) => set("course_code", e.target.value)}
              placeholder="e.g. BIO 2103"
              className="mt-1"
            />
          </div>
          <div>
            <Label>Deadline</Label>
            <Input
              type="datetime-local"
              value={form.due_date}
              onChange={(e) => set("due_date", e.target.value)}
              className="mt-1"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Priority</Label>
              <Select value={form.priority} onValueChange={(v) => set("priority", v)}>
                <SelectTrigger className="mt-1"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {PRIORITIES.map((p) => <SelectItem key={p.value} value={p.value}>{p.label}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label>Reminder</Label>
              <Select value={form.reminder_minutes} onValueChange={(v) => set("reminder_minutes", v)}>
                <SelectTrigger className="mt-1"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {REMINDERS.map((r) => <SelectItem key={r.value} value={r.value}>{r.label}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div>
            <Label>Notes</Label>
            <Textarea value={form.notes} onChange={(e) => set("notes", e.target.value)} rows={3} className="mt-1" />
          </div>
          <Button variant={form.completed ? "default" : "outline"} size="sm" onClick={toggleComplete}>
            {form.completed ? "✓ Completed" : "Mark complete"}
          </Button>
          <div className="flex gap-2 pt-1">
            <Button className="flex-1" onClick={save} disabled={saving}>
              {saving ? "Saving..." : "Save task"}
            </Button>
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