import React, { useEffect, useMemo, useState } from "react";
import { base44 } from "@/api/base44Client";
import SyncHeader from "@/components/timetable/SyncHeader";
import ProgrammeSelector from "@/components/timetable/ProgrammeSelector";
import NextUpCard from "@/components/timetable/NextUpCard";
import DaySelector from "@/components/timetable/DaySelector";
import OverlapTimeline from "@/components/timetable/OverlapTimeline";
import LectureDetailSheet from "@/components/timetable/LectureDetailSheet";
import { DAYS, findNextUp, todayName, dedupeShared } from "@/lib/timetableUtils";
import { useToast } from "@/components/ui/use-toast";

export default function Home() {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [programme, setProgramme] = useState(
    () => localStorage.getItem("must_programme") || "MBR I"
  );
  const [selectedDay, setSelectedDay] = useState(() => todayName());
  const [selectedEntry, setSelectedEntry] = useState(null);
  const [now, setNow] = useState(new Date());
  const [syncing, setSyncing] = useState(false);
  const [lastSynced, setLastSynced] = useState(null);
  const { toast } = useToast();

  useEffect(() => {
    localStorage.setItem("must_programme", programme);
  }, [programme]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const list = await base44.entities.TimetableEntry.list("-created_date", 2000);
        if (active) setEntries(list);
      } catch {
        /* ignore */
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 30000);
    return () => clearInterval(t);
  }, []);

  const programmes = useMemo(() => {
    const set = new Set();
    entries.forEach((e) => set.add(e.program_group));
    return Array.from(set).sort();
  }, [entries]);

  const myEntries = useMemo(() => {
    const mine = entries.filter(
      (e) =>
        e.program_group === programme ||
        (e.shared_with || []).includes(programme)
    );
    return dedupeShared(mine);
  }, [entries, programme]);

  const dayEntries = useMemo(
    () =>
      myEntries
        .filter((e) => e.day === selectedDay)
        .sort((a, b) => a.start_time.localeCompare(b.start_time)),
    [myEntries, selectedDay]
  );

  const nextUp = useMemo(() => findNextUp(myEntries, now), [myEntries, now]);

  const reloadEntries = async () => {
    const list = await base44.entities.TimetableEntry.list("-created_date", 2000);
    setEntries(list);
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      const res = await base44.functions.invoke("refreshTimetable", {});
      setLastSynced(new Date());
      await reloadEntries();
      toast({ title: `Timetable updated • ${res.data.imported} classes` });
    } catch (e) {
      toast({ title: "Sync failed", description: e.message, variant: "destructive" });
    } finally {
      setSyncing(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-slate-200 border-t-slate-800 rounded-full animate-spin"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-3xl mx-auto px-4 py-6 space-y-6">
        <SyncHeader
          draftVersion="Final 2026/27 S1"
          isSyncing={syncing}
          lastSynced={lastSynced}
          onSync={handleSync}
        />
        <ProgrammeSelector
          programmes={programmes}
          value={programme}
          onChange={setProgramme}
        />
        <NextUpCard nextUp={nextUp} onSelect={setSelectedEntry} />
        <DaySelector selected={selectedDay} onSelect={setSelectedDay} />
        <OverlapTimeline entries={dayEntries} onSelect={setSelectedEntry} />
      </div>
      <LectureDetailSheet entry={selectedEntry} onClose={() => setSelectedEntry(null)} />
    </div>
  );
}