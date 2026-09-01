import React from "react";
import { Link, useLocation } from "react-router-dom";
import { Calendar, CalendarDays, StickyNote, CheckCircle2, Settings as SettingsIcon } from "lucide-react";

const TABS = [
  { to: "/", label: "Timetable", icon: Calendar },
  { to: "/calendar", label: "Calendar", icon: CalendarDays },
  { to: "/notes", label: "Notes", icon: StickyNote },
  { to: "/tasks", label: "Tasks", icon: CheckCircle2 },
  { to: "/settings", label: "Settings", icon: SettingsIcon },
];

export default function BottomNav() {
  const { pathname } = useLocation();
  return (
    <nav className="fixed bottom-0 inset-x-0 z-40 border-t border-border bg-background/95 backdrop-blur">
      <div className="max-w-3xl mx-auto grid grid-cols-5 px-2 py-2">
        {TABS.map(({ to, label, icon: Icon }) => {
          const active = pathname === to;
          return (
            <Link key={to} to={to} className="flex flex-col items-center gap-1">
              <span
                className={`flex items-center justify-center w-11 h-8 rounded-full transition ${
                  active ? "bg-primary/15 text-primary" : "text-muted-foreground"
                }`}
              >
                <Icon className="w-5 h-5" />
              </span>
              <span
                className={`text-[10px] font-medium ${
                  active ? "text-primary" : "text-muted-foreground"
                }`}
              >
                {label}
              </span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}