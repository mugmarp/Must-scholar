import React from "react";
import { Button } from "@/components/ui/button";
import { RefreshCw, Wifi, WifiOff } from "lucide-react";

export default function SyncHeader({ draftVersion, isSyncing, lastSynced, onSync }) {
  const hour = new Date().getHours();
  const greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
  const online = !!lastSynced;

  return (
    <div className="flex items-start justify-between">
      <div>
        <h1 className="text-2xl font-bold font-heading">{greeting}</h1>
        <p className="text-sm text-muted-foreground">MUST Timetable • {draftVersion}</p>
      </div>
      <div className="flex flex-col items-end gap-2">
        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
          {online ? (
            <Wifi className="w-3.5 h-3.5 text-green-500" />
          ) : (
            <WifiOff className="w-3.5 h-3.5" />
          )}
          {lastSynced ? "Synced" : "Offline"}
        </div>
        <Button size="sm" variant="outline" onClick={onSync} disabled={isSyncing}>
          <RefreshCw className={`w-4 h-4 mr-1.5 ${isSyncing ? "animate-spin" : ""}`} />
          {isSyncing ? "Syncing" : "Sync"}
        </Button>
      </div>
    </div>
  );
}