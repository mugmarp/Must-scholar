import React from "react";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { GraduationCap } from "lucide-react";

export default function ProgrammeSelector({ programmes, value, onChange }) {
  if (!programmes || programmes.length === 0) return null;
  return (
    <div className="flex items-center gap-2">
      <GraduationCap className="w-5 h-5 text-muted-foreground shrink-0" />
      <Select value={value} onValueChange={onChange}>
        <SelectTrigger className="w-full">
          <SelectValue placeholder="Select your programme" />
        </SelectTrigger>
        <SelectContent>
          {programmes.map((p) => (
            <SelectItem key={p} value={p}>
              {p}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}