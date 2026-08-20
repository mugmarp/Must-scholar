import React from "react";
import LectureCard from "./LectureCard";
import { groupByTimeSlot } from "@/lib/timetableUtils";

export default function OverlapTimeline({ entries, onSelect }) {
  if (!entries || entries.length === 0) {
    return (
      <div className="py-12 text-center text-sm text-muted-foreground">
        No classes scheduled for this day.
      </div>
    );
  }

  const slots = groupByTimeSlot(entries);

  return (
    <div className="space-y-3">
      {slots.map(([time, items]) => (
        <div key={time} className="flex gap-3">
          <div className="w-14 shrink-0 pt-3 text-xs text-muted-foreground font-mono">
            {time}
          </div>
          <div
            className="flex-1 grid gap-2"
            style={{ gridTemplateColumns: `repeat(${items.length}, minmax(0, 1fr))` }}
          >
            {items.map((e) => (
              <LectureCard key={e.id} entry={e} onSelect={onSelect} />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}