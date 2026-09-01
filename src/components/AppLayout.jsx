import React from "react";
import { Outlet, Navigate } from "react-router-dom";
import BottomNav from "@/components/BottomNav";
import { useAlarmScheduler } from "@/hooks/useAlarmScheduler";

export default function AppLayout() {
  useAlarmScheduler();
  const welcomed = localStorage.getItem("must_welcome_done");
  if (!welcomed) return <Navigate to="/welcome" replace />;
  return (
    <div className="min-h-screen bg-background pb-24">
      <Outlet />
      <BottomNav />
    </div>
  );
}