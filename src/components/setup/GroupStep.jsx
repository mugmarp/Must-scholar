import React from "react";
import { Check } from "lucide-react";
import { Card } from "@/components/ui/card";

export default function GroupStep({ programme, options, loading, selected, onSelect }) {
  if (loading) {
    return (
      <div className="py-16 flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin" />
      </div>
    );
  }
  return (
    <div className="space-y-3">
      {options.map((o) => (
        <Card
          key={o.group}
          onClick={() => onSelect(o.group)}
          className={`p-4 flex items-center justify-between gap-3 cursor-pointer transition ${
            selected === o.group ? "border-primary bg-primary/5" : "hover:bg-muted/50"
          }`}
        >
          <div>
            <p className="font-medium text-sm">{o.group}</p>
            <p className="text-xs text-muted-foreground mt-0.5">
              {o.fallback
                ? "No timetable in the database yet"
                : `${o.count} timetable entries`}
            </p>
          </div>
          {selected === o.group && <Check className="w-5 h-5 text-primary shrink-0" />}
        </Card>
      ))}
      {options[0]?.fallback && (
        <p className="text-xs text-muted-foreground px-1">
          {programme.code} isn't in the current timetable database yet. You can
          change this anytime in Settings.
        </p>
      )}
    </div>
  );
}