import React from "react";
import LectureCard from "./LectureCard";

export default function OverlapTimeline({ entries, onSelect }) {
  if (!entries || entries.length === 0) {
    return (
      <div className="py-16 text-center text-sm text-muted-foreground">
        No classes scheduled for this day.
      </div>
    );
  }
  return (
    <div className="space-y-3">
      {entries.map((e) => (
        <LectureCard key={e.id} entry={e} onSelect={onSelect} />
      ))}
    </div>
  );
}