import React from "react";
import { Card } from "@/components/ui/card";
import { Clock, MapPin, User } from "lucide-react";
import { sessionStyle, formatCountdown } from "@/lib/timetableUtils";

export default function NextUpCard({ nextUp, onSelect }) {
  if (!nextUp || !nextUp.entry) {
    return (
      <Card className="p-6 bg-primary/5 border-dashed">
        <p className="text-sm text-muted-foreground">
          No upcoming classes in your timetable. Enjoy the break!
        </p>
      </Card>
    );
  }

  const e = nextUp.entry;
  const style = sessionStyle(e);

  return (
    <Card
      className="p-5 bg-primary text-primary-foreground cursor-pointer hover:opacity-95 transition"
      onClick={() => onSelect(e)}
    >
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs font-semibold tracking-wider opacity-80">NEXT UP</span>
        <span className={`text-xs px-2 py-0.5 rounded-full ${style.badge}`}>
          {style.label}
        </span>
      </div>
      <h2 className="text-xl font-bold leading-snug">{e.course_title}</h2>
      <p className="text-sm opacity-80 mt-1">
        {e.course_code} • {e.time_slot}
      </p>
      <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm mt-3 opacity-90">
        <span className="inline-flex items-center gap-1">
          <MapPin className="w-4 h-4" />
          {e.room}
        </span>
        {e.lecturer && (
          <span className="inline-flex items-center gap-1">
            <User className="w-4 h-4" />
            {e.lecturer}
          </span>
        )}
      </div>
      <div className="mt-4 flex items-center gap-2">
        <Clock className="w-5 h-5" />
        <span className="text-lg font-semibold">
          {formatCountdown(nextUp.minutesUntil)}
        </span>
      </div>
    </Card>
  );
}