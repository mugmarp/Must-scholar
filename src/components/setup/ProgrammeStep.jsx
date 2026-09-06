import React from "react";
import { Card } from "@/components/ui/card";

export default function ProgrammeStep({ faculty, onSelect }) {
  if (!faculty) return null;
  return (
    <div className="space-y-3">
      {faculty.programmes.map((p) => (
        <Card
          key={p.code}
          onClick={() => onSelect(p)}
          className="p-4 flex items-center justify-between gap-3 cursor-pointer transition hover:bg-muted/50"
        >
          <div>
            <p className="font-medium text-sm">{p.name}</p>
            <p className="text-xs text-muted-foreground mt-0.5">
              {p.years} {p.years === 1 ? "year" : "years"}
            </p>
          </div>
          <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-primary/10 text-primary shrink-0">
            {p.code}
          </span>
        </Card>
      ))}
    </div>
  );
}