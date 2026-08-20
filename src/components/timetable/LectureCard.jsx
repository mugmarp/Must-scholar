import React from "react";
import { Card } from "@/components/ui/card";
import { MapPin, User } from "lucide-react";
import { sessionStyle } from "@/lib/timetableUtils";

export default function LectureCard({ entry, onSelect }) {
  const style = sessionStyle(entry);
  return (
    <Card
      className="p-3 cursor-pointer hover:shadow-md transition relative overflow-hidden"
      onClick={() => onSelect(entry)}
    >
      <div className={`absolute left-0 top-0 bottom-0 w-1 ${style.accent}`} />
      <div className="pl-2">
        <div className="flex items-center justify-between gap-1">
          <span className={`text-xs font-bold ${style.text}`}>{entry.course_code}</span>
          <span className={`text-[10px] px-1.5 py-0.5 rounded ${style.badge}`}>
            {style.label}
          </span>
        </div>
        <p className="text-sm font-medium mt-1 line-clamp-2">{entry.course_title}</p>
        <div className="flex flex-wrap gap-x-3 gap-y-0.5 text-xs text-muted-foreground mt-2">
          <span className="inline-flex items-center gap-1">
            <MapPin className="w-3 h-3" />
            {entry.room}
          </span>
          {entry.lecturer && (
            <span className="inline-flex items-center gap-1">
              <User className="w-3 h-3" />
              {entry.lecturer.split(" ")[0]}
            </span>
          )}
        </div>
        {entry.shared_with && entry.shared_with.length > 0 && (
          <p className="text-[10px] text-muted-foreground mt-1">
            Shared: {entry.shared_with.join(", ")}
          </p>
        )}
      </div>
    </Card>
  );
}