import React from "react";
import { CalendarPlus, Clock, MapPin, Bell, ChevronRight } from "lucide-react";

export default function EventCard({ event, onSelect }) {
  return (
    <div
      onClick={() => onSelect(event)}
      className="relative p-4 rounded-2xl bg-purple-50 dark:bg-purple-950/30 border border-dashed border-purple-300 dark:border-purple-700 cursor-pointer hover:shadow-md transition overflow-hidden active:scale-[0.99]"
    >
      <div className="absolute left-0 top-0 bottom-0 w-1 bg-purple-500" />
      <div className="pl-2">
        <div className="flex items-center justify-between gap-1">
          <div className="flex items-center gap-1.5">
            <CalendarPlus className="w-3.5 h-3.5 text-purple-600" />
            <span className="text-xs font-bold text-purple-600">PERSONAL</span>
          </div>
          {event.alarm_minutes != null && <Bell className="w-3.5 h-3.5 text-purple-500" />}
        </div>
        <p className="text-base font-semibold mt-1.5 line-clamp-2">{event.title}</p>
        <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground mt-2">
          <span className="inline-flex items-center gap-1">
            <Clock className="w-3.5 h-3.5" />
            {event.start_time}
            {event.end_time ? `–${event.end_time}` : ""}
          </span>
          {event.location && (
            <span className="inline-flex items-center gap-1">
              <MapPin className="w-3.5 h-3.5" />
              {event.location}
            </span>
          )}
        </div>
        <div className="flex items-center justify-end mt-3">
          <span className="flex items-center justify-center w-8 h-8 rounded-full bg-purple-600/10 text-purple-600">
            <ChevronRight className="w-4 h-4" />
          </span>
        </div>
      </div>
    </div>
  );
}