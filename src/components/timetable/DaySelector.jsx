import React, { useRef } from "react";
import { DAYS, todayName } from "@/lib/timetableUtils";

export default function DaySelector({ selected, onSelect, onQuickAdd, now = new Date() }) {
  const jsDay = now.getDay();
  const monday = new Date(now);
  monday.setDate(now.getDate() - ((jsDay + 6) % 7));
  const today = todayName(now);
  const pressTimer = useRef(null);
  const suppressClick = useRef(false);

  const startPress = (day) => {
    suppressClick.current = false;
    pressTimer.current = setTimeout(() => {
      suppressClick.current = true;
      onQuickAdd?.(day);
      pressTimer.current = null;
    }, 450);
  };
  const cancelPress = () => {
    if (pressTimer.current) {
      clearTimeout(pressTimer.current);
      pressTimer.current = null;
    }
  };
  const handleClick = (day) => {
    if (suppressClick.current) {
      suppressClick.current = false;
      return;
    }
    onSelect(day);
  };

  return (
    <div className="flex gap-2 overflow-x-auto pb-1 -mx-4 px-4">
      {DAYS.map((day, i) => {
        const active = day === selected;
        const isToday = day === today;
        const d = new Date(monday);
        d.setDate(monday.getDate() + i);
        return (
          <button
            key={day}
            onClick={() => handleClick(day)}
            onTouchStart={() => startPress(day)}
            onTouchEnd={cancelPress}
            onTouchMove={cancelPress}
            onTouchCancel={cancelPress}
            className={`shrink-0 px-3 py-2 rounded-2xl text-center transition relative ${
              active
                ? "bg-primary text-primary-foreground"
                : isToday
                ? "bg-secondary text-secondary-foreground ring-2 ring-primary"
                : "bg-secondary text-secondary-foreground"
            }`}
          >
            <span className="block text-xs font-semibold">{day.slice(0, 3)}</span>
            <span
              className={`block text-sm font-bold ${
                active ? "" : isToday ? "text-primary" : "text-muted-foreground"
              }`}
            >
              {d.getDate()}
            </span>
          </button>
        );
      })}
    </div>
  );
}