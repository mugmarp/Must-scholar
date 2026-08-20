import React from "react";
import { DAYS } from "@/lib/timetableUtils";

export default function DaySelector({ selected, onSelect }) {
  return (
    <div className="flex gap-2 overflow-x-auto pb-1 -mx-4 px-4">
      {DAYS.map((day) => {
        const active = day === selected;
        return (
          <button
            key={day}
            onClick={() => onSelect(day)}
            className={`px-3.5 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition ${
              active
                ? "bg-primary text-primary-foreground"
                : "bg-secondary text-secondary-foreground hover:bg-accent"
            }`}
          >
            {day.slice(0, 3)}
          </button>
        );
      })}
    </div>
  );
}