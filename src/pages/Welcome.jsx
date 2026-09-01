import React from "react";
import { useNavigate } from "react-router-dom";
import { Calendar, ArrowRight, Bell, StickyNote, WifiOff, BookOpen } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function Welcome() {
  const navigate = useNavigate();
  const start = () => {
    localStorage.setItem("must_welcome_done", "1");
    navigate("/");
  };

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-b from-indigo-100 to-indigo-50">
      <div className="flex-1 flex flex-col items-center justify-center px-6 relative">
        <BookOpen className="absolute top-16 left-10 w-9 h-9 text-indigo-200/60" />
        <BookOpen className="absolute bottom-44 right-12 w-7 h-7 text-indigo-200/50" />
        <div className="space-y-2">
          <div className="w-40 h-8 rounded-lg bg-blue-700 shadow-lg flex items-center justify-center text-white text-[10px] font-bold tracking-wider">
            MUST
          </div>
          <div className="w-44 h-8 rounded-lg bg-blue-600 shadow-lg mx-auto" />
          <div className="w-40 h-8 rounded-lg bg-blue-500 shadow-lg" />
          <div className="w-36 h-8 rounded-lg bg-blue-400 shadow-lg mx-auto" />
        </div>
      </div>

      <div className="bg-background rounded-t-3xl px-6 pt-8 pb-10 shadow-2xl">
        <div className="flex items-center gap-2 mb-5">
          <span className="flex items-center justify-center w-9 h-9 rounded-xl bg-primary text-primary-foreground">
            <Calendar className="w-5 h-5" />
          </span>
          <span className="text-xl font-bold text-primary tracking-tight">MUSTimetable</span>
        </div>
        <h1 className="text-2xl font-bold text-foreground leading-tight">Start Learning Today</h1>
        <p className="text-sm text-muted-foreground mt-2 mb-6">
          Never miss a lecture. View your MUST timetable, set class alarms, and take notes — all offline.
        </p>
        <div className="flex items-center gap-3">
          <Button onClick={start} className="flex-1 rounded-full h-12 text-base">
            Get Started
          </Button>
          <Button
            onClick={start}
            size="icon"
            className="h-12 w-12 rounded-full bg-indigo-100 hover:bg-indigo-200 text-indigo-700 border-0"
          >
            <ArrowRight className="w-5 h-5" />
          </Button>
        </div>
        <div className="grid grid-cols-3 gap-3 mt-8">
          <Feature icon={Bell} label="Alarms" className="bg-indigo-100 text-indigo-700" />
          <Feature icon={StickyNote} label="Notes" className="bg-green-100 text-green-700" />
          <Feature icon={WifiOff} label="Offline" className="bg-orange-100 text-orange-700" />
        </div>
      </div>
    </div>
  );
}

function Feature({ icon: Icon, label, className }) {
  return (
    <div className="flex flex-col items-center gap-1.5">
      <span className={`flex items-center justify-center w-12 h-12 rounded-xl ${className}`}>
        <Icon className="w-6 h-6" />
      </span>
      <span className="text-xs font-medium text-muted-foreground">{label}</span>
    </div>
  );
}