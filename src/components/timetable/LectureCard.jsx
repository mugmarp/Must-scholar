import React from "react";
import { BookOpen, Clock, MapPin, User, ChevronRight } from "lucide-react";
import { sessionStyle } from "@/lib/timetableUtils";

export default function LectureCard({ entry, onSelect }) {
  const style = sessionStyle(entry);
  return (
    <div
      onClick={() => onSelect(entry)}
      className="relative p-4 rounded-2xl bg-card border border-border cursor-pointer hover:shadow-md transition overflow-hidden active:scale-[0.99]"
    >
      <div className={`absolute left-0 top-0 bottom-0 w-1 ${style.accent}`} />
      <div className="pl-2">
        <div className="flex items-center justify-between gap-1">
          <div className="flex items-center gap-1.5">
            <BookOpen className="w-3.5 h-3.5 text-muted-foreground" />
            <span className={`text-xs font-bold ${style.text}`}>{entry.course_code}</span>
          </div>
          <span className={`text-[10px] px-2 py-0.5 rounded-full ${style.badge}`}>
            {style.label}
          </span>
        </div>
        <p className="text-base font-semibold mt-1.5 line-clamp-2">
          {entry.course_title}
        </p>
        <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground mt-2">
          <span className="inline-flex items-center gap-1">
            <Clock className="w-3.5 h-3.5" />
            {entry.start_time}–{entry.end_time}
          </span>
          {entry.room && (
            <span className="inline-flex items-center gap-1">
              <MapPin className="w-3.5 h-3.5" />
              {entry.room}
            </span>
          )}
        </div>
        <div className="flex items-center justify-between mt-3">
          <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
            {entry.lecturer && (
              <>
                <User className="w-3.5 h-3.5" />
                {entry.lecturer.split(" ")[0]}
              </>
            )}
            {entry.shared_with && entry.shared_with.length > 1 && (
              <span className="ml-1">+{entry.shared_with.length - 1}</span>
            )}
          </span>
          <span className="flex items-center justify-center w-8 h-8 rounded-full bg-primary/10 text-primary">
            <ChevronRight className="w-4 h-4" />
          </span>
        </div>
      </div>
    </div>
  );
}