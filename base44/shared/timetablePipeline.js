/**
 * MUST Timetable Pipeline — canonical, environment-independent.
 *
 * Pure ESM JavaScript. No Base44 SDK, no framework imports, no Node-only APIs.
 * Consumed by:
 *   - base44/functions/refreshTimetable/entry.ts   (Base44 scheduled/manual sync)
 *   - scraper/scrape.mjs                          (standalone runner, Node >= 18)
 *
 * Pipeline stages: fetch (caller) -> HTML table extraction -> cell parsing ->
 * group canonicalization -> time normalization -> validation -> TimetableEntry records.
 *
 * Keep PROGRAMME_CODES in sync with src/data/faculties.js.
 */

export const TIMETABLE_URL = "https://timetable.must.ac.ug/index_teaching.html";
export const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
export const ROMAN_YEARS = ["I", "II", "III", "IV", "V", "VI"];
export const MAX_FLAGGED_SAMPLES = 50;

const ROMAN_SET = new Set(ROMAN_YEARS);

/** Timetable-system code variants mapped to official programme codes
 *  (observed in the live source: MLC/MLS, BNC/BNS, BSPC/BSP, PEM/PEEM, CIV/CVE, BAF/BSAF). */
export const CODE_ALIASES = {
  MLC: "MLS",
  BNC: "BNS",
  BSPC: "BSP",
  PEM: "PEEM",
  CIV: "CVE",
  BAF: "BSAF",
};

/** Official programme codes (source of truth: src/data/faculties.js). */
export const PROGRAMME_CODES = new Set([
  "MBR", "PHA", "BNS", "MLS", "BSP", "PHS", "DCM", "DEM", "DCAM", "BS", "DLT",
  "BME", "EEE", "PEEM", "CVE", "MIE", "BCS", "BIT", "BSE", "BBA", "BSAF",
  "ECO", "BPSM", "BSAL", "BGWH", "BPCD",
]);

/** Known misspellings in source subject tokens. */
const SUBJECT_FIXUPS = { BIOLOCAL: "BIOLOGICAL" };

/**
 * Parse a raw timetable group label (e.g. "MBR V", "BS-CHEM MATHS II", "MLC I")
 * into a canonical { code, year, subject, group } object.
 * Returns null for labels that don't map to an official programme code
 * (scraper artifacts such as "CD-MED/PCH", service groups such as "MATHEMATICS").
 * Subject tokens may appear before OR after the year token
 * ("BS-CHEM MATHS II" vs "BS III-PHYSICS"), so both sides are collected.
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
  if (!PROGRAMME_CODES.has(code)) return null;
  const year = parts[yearIdx].toUpperCase();
  const subjectWords = [...parts.slice(1, yearIdx), ...parts.slice(yearIdx + 1)]
    .join(" ")
    .toUpperCase()
    .trim();
  const subject = SUBJECT_FIXUPS[subjectWords] || subjectWords;
  const group = [code, subject, year].filter(Boolean).join(" ");
  return { code, year, subject, group };
}

/** Canonical group label when parseable; otherwise the cleaned raw label. */
export function canonicalGroupLabel(raw) {
  const clean = String(raw || "").replace(/\s+/g, " ").trim();
  if (!clean) return "";
  const p = parseGroup(clean);
  return p ? p.group : clean;
}

/**
 * Normalize any time notation to zero-padded 24-hour "HH:MM", or null.
 * Handles: "8:00", "08:00", "8.00", "14h30", "8:00 AM", "2:30 PM", "9".
 */
export function normalizeTime(raw) {
  if (raw == null) return null;
  const s = String(raw).trim().toLowerCase();
  if (!s) return null;
  const isAm = /\b(a\.?m\.?|am)\b/.test(s);
  const isPm = /\b(p\.?m\.?|pm)\b/.test(s);
  const m = s.match(/(\d{1,2})\s*[:.h]?\s*(\d{2})?/);
  if (!m) return null;
  let h = parseInt(m[1], 10);
  const min = m[2] ? parseInt(m[2], 10) : 0;
  if (Number.isNaN(h) || h > 23 || min > 59) return null;
  if (isPm && h < 12) h += 12;
  if (isAm && h === 12) h = 0;
  return `${String(h).padStart(2, "0")}:${String(min).padStart(2, "0")}`;
}

/** "08:00-9:00" -> { start: "08:00", end: "09:00" }; null when unparseable. */
export function parseTimeSlot(raw) {
  if (!raw) return null;
  const parts = String(raw).split(/[-–—]/);
  if (parts.length < 2) return null;
  const start = normalizeTime(parts[0]);
  const end = normalizeTime(parts.slice(1).join(""));
  if (!start || !end) return null;
  return { start, end };
}

/** Derive a course code for non-standard cells (special activities, dual listings). */
export function fallbackCode(title) {
  const t = String(title).replace(/[*#]+/g, "").trim();
  const m = t.match(/^([A-Za-z][A-Za-z0-9-]{2,})\s+/);
  if (m) return m[1].toUpperCase();
  return t.replace(/[^A-Za-z0-9]+/g, "").toUpperCase().slice(0, 12) || "CLASS";
}

/** Parse a cell's <br />-separated lines into structured fields. */
export function cellToFields(content) {
  const lines = String(content)
    .split(/<br\s*\/?>/)
    .map((l) =>
      l.replace(/&amp;/g, "&").replace(/&nbsp;/g, " ").replace(/\s+/g, " ").trim()
    )
    .filter((l) => l.length);
  if (!lines.length) return null;

  const sharedRaw = lines[0].split(",").map((s) => s.trim()).filter(Boolean);

  let courseCode = null;
  let courseTitle = null;
  let sessionType = null;
  if (lines[1]) {
    // Standard code (2-5 letters + 3-5 digits, optional trailing letter),
    // optionally a second code after "/": e.g. "OBG5112/SUG5112".
    const cm = lines[1].match(
      /^([A-Z]{2,5}\s?\d{3,5}[A-Z]?(?:\s*\/\s*[A-Z]{2,5}\s?\d{3,5}[A-Z]?)?)\s+(.+)$/
    );
    if (cm) {
      courseCode = cm[1].replace(/\s+/g, "").toUpperCase();
      courseTitle = cm[2].trim();
    } else {
      courseTitle = lines[1].replace(/[*#]+/g, "").trim();
      courseCode = fallbackCode(courseTitle);
    }
    const st = courseTitle.match(/\s+(THEORY|PRACTICAL|CLINICAL|LAB)$/i);
    if (st) {
      sessionType = st[1].toUpperCase();
      courseTitle = courseTitle.slice(0, st.index).trim();
    }
  }

  let lecturer = null;
  let room = null;
  if (lines.length >= 4) {
    lecturer = lines[2] || null;
    room = lines[3] || null;
  } else if (lines.length === 3) {
    room = lines[2] || null;
  }
  // A leading weekday in the "room" position is a shifted row, not a room.
  if (room && /^(MON|TUE|WED|THU|FRI|SAT|SUN)/i.test(room)) room = null;

  return { sharedRaw, courseCode, courseTitle, sessionType, lecturer, room };
}

/** Structural validation. "hard" problems cause the record to be dropped,
 *  "soft" problems keep the record but are listed in the report. */
export function validateEntry(entry) {
  const hard = [];
  const soft = [];
  if (!DAYS.includes(entry.day)) hard.push("invalid_day");
  if (!/^\d{2}:\d{2}$/.test(entry.start_time || "")) hard.push("invalid_start_time");
  if (!entry.course_code) hard.push("missing_course_code");
  if (!entry.program_group) hard.push("missing_program_group");
  if (entry.end_time && !/^\d{2}:\d{2}$/.test(entry.end_time)) soft.push("invalid_end_time");
  if (
    entry.start_time &&
    entry.end_time &&
    /^\d{2}:\d{2}$/.test(entry.end_time) &&
    entry.end_time <= entry.start_time
  ) {
    soft.push("end_not_after_start");
  }
  if (!entry.course_title) soft.push("missing_course_title");
  return { hard, soft };
}

/**
 * Extract, normalize and validate TimetableEntry records from the published
 * timetable HTML. Returns { entries, report } — entries are ready for bulk
 * insert; report carries parsing/validation telemetry.
 */
export function buildEntries(html) {
  const report = { tables: 0, cells: 0, kept: 0, dropped: 0, flagged: 0, flaggedSamples: [] };
  const entries = [];
  const tableRegex = /<table[^>]*id="table_\d+"[^>]*>([\s\S]*?)<\/table>/g;

  const flag = (entry, problems) => {
    report.flagged++;
    if (report.flaggedSamples.length < MAX_FLAGGED_SAMPLES) {
      report.flaggedSamples.push({
        program_group: entry.program_group,
        day: entry.day,
        time_slot: entry.time_slot,
        course_code: entry.course_code,
        problems,
      });
    }
  };

  let m;
  while ((m = tableRegex.exec(html)) !== null) {
    const inner = m[1];
    const grpMatch = inner.match(/<th\s+colspan="7"[^>]*>([\s\S]*?)<\/th>/);
    const groupRaw = grpMatch ? grpMatch[1].replace(/<[^>]+>/g, "").trim() : null;
    if (!groupRaw) continue;
    report.tables++;
    const group = canonicalGroupLabel(groupRaw);

    const days = [...inner.matchAll(/<th class="xAxis">([^<]+)<\/th>/g)].map((x) => x[1].trim());
    const tbodyMatch = inner.match(/<tbody>([\s\S]*?)<\/tbody>/);
    if (!tbodyMatch) continue;

    const tbody = tbodyMatch[1];
    const rowHtmls = [...tbody.matchAll(/<tr>([\s\S]*?)<\/tr>/g)].map((r) => r[1]);
    const rows = rowHtmls.map((rowHtml) => {
      const tsMatch = rowHtml.match(/<th class="yAxis">([^<]+)<\/th>/);
      const timeSlot = tsMatch ? tsMatch[1].trim() : null;
      const realCells = [];
      const tdRegex = /<td([^>]*)>([\s\S]*?)<\/td>/g;
      let c;
      while ((c = tdRegex.exec(rowHtml)) !== null) {
        const rs = c[1].match(/rowspan="(\d+)"/);
        realCells.push({ rowspan: rs ? parseInt(rs[1]) : 1, content: (c[2] || "").trim() });
      }
      return { timeSlot, realCells };
    });

    const numCols = days.length;
    const pending = new Array(numCols).fill(0);
    rows.forEach((row, ri) => {
      let col = 0;
      for (const cell of row.realCells) {
        while (col < numCols && pending[col] > 0) {
          pending[col]--;
          col++;
        }
        if (col >= numCols) break;
        if (cell.content) {
          report.cells++;
          const f = cellToFields(cell.content);
          if (f && f.courseCode) {
            const slot = parseTimeSlot(row.timeSlot);
            const endSlot = rows[ri + cell.rowspan - 1];
            const endSlotParsed = endSlot ? parseTimeSlot(endSlot.timeSlot) : null;
            const sharedWith = [
              ...new Set(f.sharedRaw.map(canonicalGroupLabel).filter(Boolean)),
            ].filter(
              (g) =>
                g.toUpperCase() !== groupRaw.toUpperCase() &&
                g.toUpperCase() !== group.toUpperCase()
            );
            const entry = {
              program_group: group,
              day: days[col],
              time_slot: (row.timeSlot || "").trim(),
              start_time: slot ? slot.start : null,
              end_time: endSlotParsed ? endSlotParsed.end : null,
              course_code: f.courseCode,
              course_title: f.courseTitle,
              session_type: f.sessionType,
              lecturer: f.lecturer,
              room: f.room,
              shared_with: sharedWith,
            };
            const { hard, soft } = validateEntry(entry);
            if (hard.length) {
              report.dropped++;
              flag(entry, hard);
            } else {
              if (soft.length) flag(entry, soft);
              entries.push(entry);
              report.kept++;
            }
          }
        }
        if (cell.rowspan > 1) pending[col] = cell.rowspan - 1;
        col++;
      }
      while (col < numCols) {
        if (pending[col] > 0) pending[col]--;
        col++;
      }
    });
  }

  report.groups = new Set(entries.map((e) => e.program_group)).size;
  return { entries, report };
}