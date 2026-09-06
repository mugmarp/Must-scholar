import { FACULTIES } from "@/data/faculties";

export const ROMAN_YEARS = ["I", "II", "III", "IV", "V", "VI"];
const ROMAN_SET = new Set(ROMAN_YEARS);

// Timetable-system code variants mapped to their official programme codes
// (observed in the live timetable database: MLC/MLS, BNC/BNS, PEM/PEEM, ...)
const CODE_ALIASES = {
  MLC: "MLS",
  BNC: "BNS",
  BSPC: "BSP",
  PEM: "PEEM",
  CIV: "CVE",
  BAF: "BSAF",
};

const OFFICIAL_CODES = new Set(
  FACULTIES.flatMap((f) => f.programmes.map((p) => p.code))
);

export function findFaculty(id) {
  return FACULTIES.find((f) => f.id === id) || null;
}

export function findProgramme(code) {
  for (const f of FACULTIES) {
    const p = f.programmes.find((x) => x.code === code);
    if (p) return p;
  }
  return null;
}

/**
 * Parses a raw timetable group label (e.g. "MBR V", "BS-CHEM MATHS II",
 * "MLC I") into a canonical { code, year, subject, group } object.
 * Returns null for labels that don't map to an official programme code
 * (scraper artifacts such as "CD-MED/PCH", service groups such as "PHYSICS").
 */
export function parseGroup(raw) {
  if (!raw) return null;
  const clean = String(raw).replace(/\s+/g, " ").trim();
  if (!clean) return null;
  const parts = clean.split(/[\s-]+/).filter(Boolean);
  const yearIdx = parts.findIndex((p) => ROMAN_SET.has(p.toUpperCase()));
  if (yearIdx <= 0) return null;
  const rawCode = parts[0].toUpperCase().replace(/[^A-Z]/g, "");
  const code = CODE_ALIASES[rawCode] || rawCode;
  if (!OFFICIAL_CODES.has(code)) return null;
  const year = parts[yearIdx].toUpperCase();
  // Subject tokens may sit before OR after the year token
  // ("BS-CHEM MATHS II" vs "BS III-PHYSICS"), so collect both sides.
  const SUBJECT_FIXUPS = { BIOLOCAL: "BIOLOGICAL" };
  const subjectWords = [...parts.slice(1, yearIdx), ...parts.slice(yearIdx + 1)]
    .join(" ")
    .toUpperCase()
    .trim();
  const subject = SUBJECT_FIXUPS[subjectWords] || subjectWords;
  const group = [code, subject, year].filter(Boolean).join(" ");
  return { code, year, subject, group };
}

/** True when a timetable entry belongs to the user's selected group
 *  (matched on the entry itself or its shared_with list, aliases normalized). */
export function entryMatchesGroup(entry, group) {
  const target = parseGroup(group);
  if (!target) return false;
  const same = (v) => {
    const p = parseGroup(v);
    return !!p && p.group === target.group;
  };
  return same(entry.program_group) || (entry.shared_with || []).some(same);
}

export function hasSelection() {
  return !!localStorage.getItem("must_programme");
}

export function loadSelection() {
  const group = localStorage.getItem("must_programme");
  if (!group) return null;
  return {
    group,
    code: localStorage.getItem("must_prog_code") || parseGroup(group)?.code || "",
    facultyId: localStorage.getItem("must_faculty") || "",
  };
}

export function saveSelection({ facultyId, code, group }) {
  localStorage.setItem("must_programme", group);
  localStorage.setItem("must_prog_code", code);
  localStorage.setItem("must_faculty", facultyId);
}