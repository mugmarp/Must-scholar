import React from "react";
import { Card } from "@/components/ui/card";

export default function FacultyStep({ faculties, selected, onSelect }) {
  return (
    <div className="space-y-3">
      {faculties.map((f) => {
        const Icon = f.icon;
        return (
          <Card
            key={f.id}
            onClick={() => onSelect(f)}
            className={`p-4 flex items-center gap-3 cursor-pointer transition ${
              selected === f.id ? "border-primary bg-primary/5" : "hover:bg-muted/50"
            }`}
          >
            <span className="flex items-center justify-center w-10 h-10 rounded-xl bg-primary/10 text-primary shrink-0">
              <Icon className="w-5 h-5" />
            </span>
            <div>
              <p className="font-medium text-sm">{f.name}</p>
              {f.subtitle && (
                <p className="text-xs text-muted-foreground">{f.subtitle}</p>
              )}
            </div>
          </Card>
        );
      })}
    </div>
  );
}