import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { base44 } from "@/api/base44Client";
import { ArrowLeft, Plus, CheckCircle2, Circle, Calendar, Bell, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import AssignmentSheet from "@/components/tasks/AssignmentSheet";

const PRIORITY_BADGE = {
  high: "bg-red-100 text-red-700",
  medium: "bg-amber-100 text-amber-700",
  low: "bg-blue-100 text-blue-700",
};

function dueLabel(due) {
  if (!due) return null;
  const d = new Date(due);
  const now = new Date();
  const today = new Date(now);
  today.setHours(0, 0, 0, 0);
  const dd = new Date(d);
  dd.setHours(0, 0, 0, 0);
  const diffDays = Math.round((dd - today) / 86400000);
  const time = d.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" });
  if (diffDays < 0) return { text: `Overdue • ${d.toLocaleDateString("en-GB")}`, cls: "text-red-600 font-semibold" };
  if (diffDays === 0) return { text: `Today • ${time}`, cls: "text-amber-600 font-semibold" };
  if (diffDays === 1) return { text: "Tomorrow", cls: "text-muted-foreground" };
  return { text: d.toLocaleDateString("en-GB", { dateStyle: "medium" }), cls: "text-muted-foreground" };
}

export default function Tasks() {
  const navigate = useNavigate();
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null);
  const [query, setQuery] = useState("");
  const [hideDone, setHideDone] = useState(false);

  const load = async () => {
    try {
      const list = await base44.entities.Assignment.list("-created_date", 500);
      setTasks(list);
    } catch {
      /* ignore */
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const sorted = useMemo(
    () =>
      [...tasks].sort((a, b) => {
        if (!!a.completed !== !!b.completed) return a.completed ? 1 : -1;
        const ad = a.due_date ? new Date(a.due_date).getTime() : Infinity;
        const bd = b.due_date ? new Date(b.due_date).getTime() : Infinity;
        return ad - bd;
      }),
    [tasks]
  );

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return sorted.filter((t) => {
      if (hideDone && t.completed) return false;
      if (!q) return true;
      return (
        (t.title || "").toLowerCase().includes(q) ||
        (t.course_code || "").toLowerCase().includes(q)
      );
    });
  }, [sorted, query, hideDone]);

  const toggle = async (t) => {
    try {
      await base44.entities.Assignment.update(t.id, { completed: !t.completed });
      load();
    } catch {
      /* ignore */
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 space-y-5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button onClick={() => navigate("/")} className="p-1 -ml-1">
            <ArrowLeft className="w-6 h-6" />
          </button>
          <h1 className="text-xl font-bold">Tasks</h1>
        </div>
        <Button size="icon" onClick={() => setEditing({})} className="rounded-full">
          <Plus className="w-5 h-5" />
        </Button>
      </div>

      <div className="flex items-center gap-2">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by title or course..."
            className="pl-9"
          />
        </div>
        <Button
          size="sm"
          variant={hideDone ? "default" : "outline"}
          onClick={() => setHideDone((v) => !v)}
          className="rounded-full whitespace-nowrap"
        >
          {hideDone ? "Hide done" : "Show all"}
        </Button>
      </div>

      {loading ? (
        <div className="flex justify-center py-20">
          <div className="w-8 h-8 border-4 border-muted border-t-primary rounded-full animate-spin" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="py-20 text-center text-sm text-muted-foreground">
          {tasks.length === 0 ? "No tasks yet. Tap + to add an assignment or deadline." : "No matching tasks."}
        </div>
      ) : (
        <div className="space-y-2">
          {filtered.map((t) => {
            const dl = dueLabel(t.due_date);
            return (
              <div key={t.id} className="flex items-start gap-3 p-3 rounded-xl bg-card border border-border">
                <button onClick={() => toggle(t)} className="mt-0.5">
                  {t.completed ? (
                    <CheckCircle2 className="w-5 h-5 text-primary" />
                  ) : (
                    <Circle className="w-5 h-5 text-muted-foreground" />
                  )}
                </button>
                <button onClick={() => setEditing(t)} className="flex-1 text-left">
                  <div className="flex items-center gap-2">
                    <p className={`text-sm font-medium ${t.completed ? "line-through text-muted-foreground" : ""}`}>
                      {t.title}
                    </p>
                    <span className={`text-[10px] px-1.5 py-0.5 rounded ${PRIORITY_BADGE[t.priority] || PRIORITY_BADGE.medium}`}>
                      {t.priority || "medium"}
                    </span>
                  </div>
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-0.5 text-xs mt-1">
                    {t.course_code && <span className="text-muted-foreground">{t.course_code}</span>}
                    {dl && (
                      <span className={`inline-flex items-center gap-1 ${dl.cls}`}>
                        <Calendar className="w-3 h-3" />
                        {dl.text}
                      </span>
                    )}
                    {t.reminder_minutes != null && (
                      <span className="inline-flex items-center gap-1 text-muted-foreground">
                        <Bell className="w-3 h-3" />
                        {t.reminder_minutes}m
                      </span>
                    )}
                  </div>
                </button>
              </div>
            );
          })}
        </div>
      )}

      <AssignmentSheet assignment={editing} onClose={() => setEditing(null)} onSaved={load} />
    </div>
  );
}