import React from "react";
import { ArrowLeft } from "lucide-react";

const STEP_COPY = [
  { title: "Choose your faculty", subtitle: "Which faculty are you in?" },
  { title: "Choose your programme", subtitle: "Pick your course of study" },
  { title: "Choose your class group", subtitle: "Select your year and group" },
];

export default function SetupHeader({ step, onBack }) {
  const copy = STEP_COPY[step - 1] || STEP_COPY[0];
  return (
    <div className="mb-6">
      <button onClick={onBack} className="p-1 -ml-1 mb-4">
        <ArrowLeft className="w-6 h-6" />
      </button>
      <div className="flex gap-1.5 mb-3">
        {[1, 2, 3].map((s) => (
          <span
            key={s}
            className={`h-1.5 rounded-full transition-all ${
              s === step ? "w-6 bg-primary" : s < step ? "w-6 bg-primary/40" : "w-4 bg-primary/20"
            }`}
          />
        ))}
      </div>
      <h1 className="text-2xl font-bold leading-tight">{copy.title}</h1>
      <p className="text-sm text-muted-foreground mt-1">{copy.subtitle}</p>
    </div>
  );
}