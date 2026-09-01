import React from "react";
import { Clock, MapPin, User, ChevronRight } from "lucide-react";
import { sessionStyle, formatCountdown } from "@/lib/timetableUtils";

export default function NextUpCard({ nextUp, onSelect }) {
  if (!nextUp || !nextUp.entry) return null;
  const e = nextUp.entry;
  const style = sessionStyle(e);

  return (
    <div
      onClick={() => onSelect(e)}
      className="relative rounded-3xl p-5 bg-gradient-to-br from-primary to-violet-700 text-white cursor-pointer overflow-hidden shadow-lg active:scale-[0.99] transition"
    >
      <div className="flex items-center justify-between mb-3">
        <span className="text-[11px] font-bold tracking-wider px-2 py-0.5 rounded-full bg-white/20">
          NEXT UP
        </span>
        <span className="text-sm font-semibold">
          {formatCountdown(nextUp.minutesUntil).replace("In ", "in ")}
        </span>
      </div>
      <div className="flex items-center gap-2 mb-1">
        <span className="text-sm font-semibold opacity-90">{e.course_code}</span>
        <span className="text-[11px] px-2 py-0.5 rounded-full bg-white/20">
          {style.label}
        </span>
      </div>
      <h2 className="text-2xl font-bold leading-tight">{e.course_title}</h2>
      <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm mt-3 opacity-90">
        <span className="inline-flex items-center gap-1">
          <Clock className="w-4 h-4" />
          {e.start_time} – {e.end_time}
        </span>
        <span className="inline-flex items-center gap-1">
          <MapPin className="w-4 h-4" />
          {e.room}
        </span>
      </div>
      <div className="flex items-center justify-between mt-4">
        <span className="inline-flex items-center gap-1 text-sm opacity-90">
          {e.lecturer && (
            <>
              <User className="w-4 h-4" />
              {e.lecturer.split(" ")[0]}
            </>
          )}
        </span>
        <span className="flex items-center justify-center w-9 h-9 rounded-full bg-white/90 text-primary">
          <ChevronRight className="w-5 h-5" />
        </span>
      </div>
    </div>
  );
}