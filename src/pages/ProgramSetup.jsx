import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { base44 } from "@/api/base44Client";
import { FACULTIES } from "@/data/faculties";
import { parseGroup, saveSelection, ROMAN_YEARS } from "@/lib/programmes";
import SetupHeader from "@/components/setup/SetupHeader";
import FacultyStep from "@/components/setup/FacultyStep";
import ProgrammeStep from "@/components/setup/ProgrammeStep";
import GroupStep from "@/components/setup/GroupStep";
import { Button } from "@/components/ui/button";

export default function ProgramSetup() {
  const navigate = useNavigate();
  const fromSettings =
    new URLSearchParams(window.location.search).get("from") === "settings";

  const [step, setStep] = useState(1);
  const [faculty, setFaculty] = useState(null);
  const [programme, setProgramme] = useState(null);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [dbGroups, setDbGroups] = useState([]);
  const [dbLoading, setDbLoading] = useState(true);

  // Validate available class groups against the real timetable database
  // (program_group values plus shared_with lists, alias-normalized).
  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const list = await base44.entities.TimetableEntry.list("-created_date", 2000);
        if (!active) return;
        const map = new Map();
        for (const e of list) {
          const seen = new Map();
          [e.program_group, ...(e.shared_with || [])].forEach((v) => {
            const p = parseGroup(v);
            if (p) seen.set(p.group, p);
          });
          seen.forEach((p, group) => {
            const prev = map.get(group);
            map.set(group, { ...p, count: (prev?.count || 0) + 1 });
          });
        }
        setDbGroups(Array.from(map.values()));
      } catch {
        /* database unavailable — validated fallback year options are used */
      } finally {
        if (active) setDbLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  const options = useMemo(() => {
    if (!programme) return [];
    const matching = dbGroups
      .filter((o) => o.code === programme.code)
      .sort(
        (a, b) =>
          ROMAN_YEARS.indexOf(a.year) - ROMAN_YEARS.indexOf(b.year) ||
          a.subject.localeCompare(b.subject)
      );
    if (matching.length > 0) return matching;
    return Array.from({ length: programme.years }, (_, i) => ({
      group: `${programme.code} ${ROMAN_YEARS[i]}`,
      code: programme.code,
      year: ROMAN_YEARS[i],
      subject: "",
      count: 0,
      fallback: true,
    }));
  }, [programme, dbGroups]);

  const back = () => {
    if (step === 3) {
      setSelectedGroup(null);
      setStep(2);
    } else if (step === 2) {
      setStep(1);
    } else {
      navigate(fromSettings ? "/settings" : "/welcome");
    }
  };

  const confirm = () => {
    if (!faculty || !programme || !selectedGroup) return;
    saveSelection({
      facultyId: faculty.id,
      code: programme.code,
      group: selectedGroup,
    });
    navigate(fromSettings ? "/settings" : "/");
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-indigo-100 to-indigo-50">
      <div className="max-w-lg mx-auto px-4 py-6">
        <SetupHeader step={step} onBack={back} />

        {step === 1 && (
          <FacultyStep
            faculties={FACULTIES}
            selected={faculty?.id}
            onSelect={(f) => {
              setFaculty(f);
              setProgramme(null);
              setStep(2);
            }}
          />
        )}

        {step === 2 && (
          <ProgrammeStep
            faculty={faculty}
            onSelect={(p) => {
              setProgramme(p);
              setSelectedGroup(null);
              setStep(3);
            }}
          />
        )}

        {step === 3 && (
          <>
            <GroupStep
              programme={programme}
              options={options}
              loading={dbLoading}
              selected={selectedGroup}
              onSelect={setSelectedGroup}
            />
            <Button
              className="w-full rounded-full h-12 text-base mt-5"
              disabled={!selectedGroup}
              onClick={confirm}
            >
              Confirm
            </Button>
          </>
        )}
      </div>
    </div>
  );
}