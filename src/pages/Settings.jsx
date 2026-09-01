import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  HelpCircle,
  Moon,
  Sun,
  Check,
  RefreshCw,
  Trash2,
  AlarmClockOff,
  Database,
  ChevronRight,
  Bell,
} from "lucide-react";
import { Switch } from "@/components/ui/switch";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useToast } from "@/components/ui/use-toast";
import { base44 } from "@/api/base44Client";
import { useTheme, ACCENTS } from "@/lib/theme";
import { requestNotificationPermission, notificationsSupported } from "@/lib/alarms";

export default function Settings() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const { dark, setDark, accent, setAccent } = useTheme();
  const [syncing, setSyncing] = useState(false);
  const [lastSynced, setLastSynced] = useState(
    () => localStorage.getItem("must_last_synced") || ""
  );
  const [perm, setPerm] = useState(() =>
    notificationsSupported() ? Notification.permission : "unsupported"
  );

  const enableNotifications = async () => {
    const ok = await requestNotificationPermission();
    setPerm(notificationsSupported() ? Notification.permission : "unsupported");
    toast(ok ? { title: "Notifications enabled" } : { title: "Could not enable", variant: "destructive" });
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      const res = await base44.functions.invoke("refreshTimetable", {});
      const when = new Date().toLocaleString("en-GB", {
        dateStyle: "medium",
        timeStyle: "short",
      });
      localStorage.setItem("must_last_synced", when);
      setLastSynced(when);
      toast({ title: `Timetable updated • ${res.data.imported} classes` });
    } catch (e) {
      toast({ title: "Sync failed", description: e.message, variant: "destructive" });
    } finally {
      setSyncing(false);
    }
  };

  const clearCache = () => {
    localStorage.removeItem("must_programme");
    toast({ title: "Cache cleared" });
  };

  const resetNotes = async () => {
    if (!window.confirm("Permanently delete ALL lecture notes? This cannot be undone.")) return;
    try {
      await base44.entities.LectureNote.deleteMany({});
      toast({ title: "All notes deleted" });
    } catch (e) {
      toast({ title: "Failed", description: e.message, variant: "destructive" });
    }
  };

  const clearAlarms = async () => {
    try {
      await base44.entities.LectureNote.updateMany({}, { $set: { alarm_minutes: null } });
      toast({ title: "All alarms cleared" });
    } catch (e) {
      toast({ title: "Failed", description: e.message, variant: "destructive" });
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 space-y-6">
      <div className="flex items-center justify-between">
        <button onClick={() => navigate("/")} className="p-1 -ml-1">
          <ArrowLeft className="w-6 h-6" />
        </button>
        <h1 className="text-xl font-bold">Settings</h1>
        <button className="p-1 -mr-1">
          <HelpCircle className="w-6 h-6 text-muted-foreground" />
        </button>
      </div>

      {/* Notifications */}
      <div className="space-y-2">
        <p className="text-xs font-bold tracking-wider text-primary">NOTIFICATIONS</p>
        <Card className="p-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Bell className="w-5 h-5 text-muted-foreground" />
            <div>
              <p className="font-medium text-sm">Class & deadline reminders</p>
              <p className="text-xs text-muted-foreground">
                {perm === "granted"
                  ? "Enabled"
                  : perm === "denied"
                  ? "Blocked in browser settings"
                  : perm === "unsupported"
                  ? "Not supported on this device"
                  : "Tap to enable browser notifications"}
              </p>
            </div>
          </div>
          {perm !== "granted" && perm !== "unsupported" && (
            <Button size="sm" onClick={enableNotifications} className="rounded-full">
              Enable
            </Button>
          )}
        </Card>
      </div>

      {/* Appearance */}
      <div className="space-y-2">
        <p className="text-xs font-bold tracking-wider text-primary">APPEARANCE</p>
        <Card className="p-4 space-y-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              {dark ? (
                <Moon className="w-5 h-5 text-muted-foreground" />
              ) : (
                <Sun className="w-5 h-5 text-muted-foreground" />
              )}
              <div>
                <p className="font-medium text-sm">Dark Mode</p>
                <p className="text-xs text-muted-foreground">{dark ? "On" : "Off"}</p>
              </div>
            </div>
            <Switch checked={dark} onCheckedChange={setDark} />
          </div>
          <div>
            <p className="font-medium text-sm mb-3">Accent Colour</p>
            <div className="flex gap-4">
              {Object.entries(ACCENTS).map(([key, a]) => (
                <button
                  key={key}
                  onClick={() => setAccent(key)}
                  className="flex items-center justify-center w-9 h-9 rounded-full transition"
                  style={{ backgroundColor: a.swatch }}
                >
                  {accent === key && <Check className="w-4 h-4 text-white" />}
                </button>
              ))}
            </div>
          </div>
        </Card>
      </div>

      {/* Data & Sync */}
      <div className="space-y-2">
        <p className="text-xs font-bold tracking-wider text-primary">DATA & SYNC</p>
        <Card className="p-4 divide-y divide-border">
          <Row icon={RefreshCw} title="Sync Timetable" subtitle={lastSynced ? `Last synced: ${lastSynced}` : "Not synced yet"}>
            <Button
              size="sm"
              onClick={handleSync}
              disabled={syncing}
              className="rounded-full bg-indigo-100 text-indigo-700 hover:bg-indigo-200 border-0"
            >
              {syncing ? "Syncing" : "Sync Now"}
            </Button>
          </Row>
          <Row icon={Database} title="Clear Cache" subtitle="Remove cached timetable data">
            <Button size="sm" variant="outline" onClick={clearCache} className="rounded-full">
              Clear
            </Button>
          </Row>
          <Row icon={Check} title="Timetable Version" subtitle="Final v3 — Semester 2, 2025/2026">
            <span className="text-[11px] font-bold px-2.5 py-1 rounded-full bg-green-100 text-green-700">
              FINAL
            </span>
          </Row>
        </Card>
      </div>

      {/* Danger Zone */}
      <div className="space-y-2">
        <p className="text-xs font-bold tracking-wider text-red-500">DANGER ZONE</p>
        <Card className="p-4 divide-y divide-red-100 border-red-200">
          <Row icon={Trash2} title="Reset All Notes" subtitle="Permanently delete all lecture notes" onClick={resetNotes}>
            <ChevronRight className="w-4 h-4 text-muted-foreground" />
          </Row>
          <Row icon={AlarmClockOff} title="Clear All Alarms" subtitle="Cancel all scheduled class reminders" onClick={clearAlarms}>
            <ChevronRight className="w-4 h-4 text-muted-foreground" />
          </Row>
        </Card>
      </div>
    </div>
  );
}

function Row({ icon: Icon, title, subtitle, children, onClick }) {
  return (
    <div
      className={`flex items-center justify-between py-3 first:pt-0 last:pb-0 ${
        onClick ? "cursor-pointer" : ""
      }`}
      onClick={onClick}
    >
      <div className="flex items-center gap-3">
        <Icon className="w-5 h-5 text-muted-foreground" />
        <div>
          <p className="font-medium text-sm">{title}</p>
          <p className="text-xs text-muted-foreground">{subtitle}</p>
        </div>
      </div>
      <div>{children}</div>
    </div>
  );
}