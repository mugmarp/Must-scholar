# MUST Timetable Scraper — Engineering Specification

Standalone, environment-independent scraper that reproduces the exact pipeline used by the
MUST Scholar app: **fetch → HTML table extraction → cell parsing → group canonicalization →
time normalization → validation → TimetableEntry records**.

The engine lives in **`base44/shared/timetablePipeline.js`** (pure ESM JavaScript — no Base44
SDK, no framework imports). This runner (`scrape.mjs`) uses only the Node stdlib and global
`fetch` (Node >= 18). Copy `scraper/` **and** `base44/shared/timetablePipeline.js` together
into the target environment; no `npm install` is required.

```bash
node scraper/scrape.mjs --pretty            # live scrape -> timetable.json
node scraper/scrape.mjs --file saved.html   # parse a downloaded copy
node scraper/scrape.mjs --out data.json     # custom output path
```

Output shape: `{ generated_at, source, count, entries[], report }` — `entries` maps 1:1 to
the `TimetableEntry` schema below and can be bulk-inserted into any database.

---

## 1. The Complete Backend Schema

`TimetableEntry` — one record = one timetable cell (a class block for one cohort on one day):

| Field | Type | Constraints |
|---|---|---|
| `program_group` | string | Canonical cohort label, e.g. `"MBR I"`, `"BS CHEM MATHS II"`. From the table's `<th colspan="7">` header, canonicalized (see §4). Non-programme teaching blocks (e.g. `"CD-MED/PCH"`, `"MATHEMATICS"`) are kept verbatim — they carry real classes via `shared_with`. |
| `day` | string | Enum: `Monday \| Tuesday \| Wednesday \| Thursday \| Friday \| Saturday \| Sunday`. From `<th class="xAxis">` column headers. |
| `time_slot` | string | Raw row header, e.g. `"08:00-9:00"`. Kept verbatim for display fidelity. |
| `start_time` | string | Normalized 24h `HH:MM` (zero-padded). Required. |
| `end_time` | string \| null | Normalized 24h `HH:MM`; derived from the last row spanned by the cell's `rowspan`, else null. |
| `course_code` | string | Uppercase, whitespace stripped. Standard form `^[A-Z]{2,5}\d{3,5}[A-Z]?$`, dual codes joined with `/` (`"OBG5112/SUG5112"`); non-standard cells get a derived fallback code (see §3). Required. |
| `course_title` | string | Free text, stripped of `*`/`#` markers and of the trailing session-type token. |
| `session_type` | string \| null | Enum: `THEORY \| PRACTICAL \| CLINICAL \| LAB`. Inferred from a trailing token on the title (see §3), else null. |
| `lecturer` | string \| null | Third `<br />` line when the cell has >= 4 lines. |
| `room` | string \| null | Fourth line (or third when only 3 lines). Nulled if it starts with a weekday token (shifted-row artifact). |
| `shared_with` | string[] | Canonicalized additional cohort labels from the cell's first line (comma-separated), deduped, self-label excluded. May be empty. |

**Natural key** (stable across semesters, used for note linking — this is the contract):
`program_group + "|" + course_code + "|" + day + "|" + start_time`

Built-ins on every stored record: `id`, `created_date`, `updated_date`, `created_by_id`.

Sibling entities: `LectureNote { natural_key, content, alarm_minutes }`,
`Assignment { title, course_code, due_date, reminder_minutes, priority, notes, completed }`,
`CustomEvent { title, day, start_time, end_time, location, notes, repeat_weekly, alarm_minutes }`.

## 2. Target Endpoints & Source Utilities

- **URL**: `https://timetable.must.ac.ug/index_teaching.html` (constant `TIMETABLE_URL`).
- **Transport**: a single static HTML page — **no AJAX/JSON endpoints, no query parameters**.
  The page embeds every cohort's timetable as separate `<table id="table_N">` elements.
- **Request headers**: `User-Agent: MUST-Scholar-Scraper/1.0` (identify the bot).
- **HTML structure** (per table):
  - `<th colspan="7">GROUP LABEL</th>` — the cohort this table belongs to.
  - `<th class="xAxis">Day</th>` ×7 — the day columns (Monday..Saturday typically).
  - `<tbody>` rows: `<th class="yAxis">08:00-9:00</th>` row header (the hour band), then
    `<td>` cells; a `<td rowspan="N">` cell spans N hour bands.
- The Base44 sync (`base44/functions/refreshTimetable/entry.ts`) runs the same pipeline,
  is admin-only (`base44.auth.me()` + role check), guards against a malformed source page
  (`>= 200` entries required before it will wipe-and-replace), and bulk-inserts in batches of 400.

## 3. Data Extraction Logic (Regex & Parsing Rules)

**Table discovery** — `/<table[^>]*id="table_\d+"[^>]*>([\s\S]*?)<\/table>/g`

**Group header** — `/<th\s+colspan="7"[^>]*>([\s\S]*?)<\/th>/`, tags stripped.

**Day columns** — `/<th class="xAxis">([^<]+)<\/th>/g`

**Row time slot** — `/<th class="yAxis">([^<]+)<\/th>/`

**Cells + rowspan** — `/<td([^>]*)>([\s\S]*?)<\/td>/g` with `/rowspan="(\d+)"/`;
rowspan continuation is tracked with a per-column `pending[]` counter so a spanning cell
occupies the correct day column in subsequent rows (classic HTML-table matrix reconstruction).

**Cell lines** — content is split on `/<br\s*\/?>/`, each line entity-decoded
(`&amp;`, `&nbsp;`) and whitespace-collapsed. Line 1 = cohort list (comma-separated),
line 2 = code + title, line 3 = lecturer, line 4 = room.

**Course codes** —
```js
/^([A-Z]{2,5}\s?\d{3,5}[A-Z]?(?:\s*\/\s*[A-Z]{2,5}\s?\d{3,5}[A-Z]?)?)\s+(.+)$/
```
matches `PHA3102`, `BIT 2204`, `OBG5112/SUG5112`, `BPH4125/BPS3123`. Non-matching lines fall
back to a derived code: first word if it looks like a token (`/^([A-Za-z][A-Za-z0-9-]{2,})\s+/`),
else alphanumerics of the title capped at 12 chars, else `CLASS` (covers supervisor-name rows
like "Felix Oyania" or blocks like "GRAND ROUND").

**Time normalization** (→ zero-padded 24h `HH:MM`):
```js
// /(\d{1,2})\s*[:.h]?\s*(\d{2})?/  with AM/PM handling
```
Handles `8:00`, `08:00`, `8.00`, `14h30`, `8:00 AM`, `2:30 PM`, bare `9` (→ `09:00`).
A slot `"08:00-9:00"` splits on `/[-–—]/` → start `08:00`; end time comes from the slot of the
row where the cell's rowspan ends (so a 3-band cell 10:00–13:00 gets end `13:00`, not `11:00`).

**Session type** — inferred from a trailing token on the title:
`/\s+(THEORY|PRACTICAL|CLINICAL|LAB)$/i` → uppercase enum, stripped from the title.
Rows without the token stay null (display layer additionally infers "WARD" styling heuristics).

**Room artifact guard** — if the room line starts with a weekday
(`/^(MON|TUE|WED|THU|FRI|SAT|SUN)/i`) it is nulled (shifted-row false positive).

## 4. Cohort Splitting & "Shared With" Mapping Logic

1. The **table header** (`<th colspan="7">`) defines `program_group`; the **cell's first
   line** is a comma-separated list of every cohort attending that block.
2. Each label is **canonicalized** by `parseGroup`:
   - Normalize whitespace; split on `/[\s-]+/`.
   - Find the Roman-year token (`I`..`VI`); the first token is the programme code.
   - Alias-map non-official spellings to official codes:
     `MLC→MLS, BNC→BNS, BSPC→BSP, PEM→PEEM, CIV→CVE, BAF→BSAF`.
   - Only labels whose code is in the official set (26 programme codes) canonicalize;
     everything else (course-title strings like `"OBG5111 Obstetrics ... THEORY"`, artifact
     blocks like `"CD-MED/PCH"`, service groups like `"MATHEMATICS"`) is kept verbatim.
   - Subject tokens may sit **before or after** the year (`"BS-CHEM MATHS II"` vs
     `"BS III-PHYSICS"`); both sides are collected. Known misspellings fixed
     (`BIOLOCAL→BIOLOGICAL`).
   - Canonical form: `CODE [SUBJECT] YEAR` → `"BS CHEM MATHS II"`.
3. `shared_with` = canonicalized first-line labels, **deduped** (`Set`), with the owning
   table's label (raw and canonical forms) excluded — no self-reference, no duplication.
   One row stays **one record** for the primary `program_group`; other cohorts discover it
   by matching against `shared_with`, so no record duplication.
4. Consequence: alias spellings merge into single canonical groups, so a student of
   `MLS III` sees both `MLS III`-owned and `MLC III`-labelled classes without any client fixup.

## 5. Error Handling & Data Validation Criteria

Per-entry validation (`validateEntry`) splits problems into **hard** (record dropped) and
**soft** (kept, but reported):

- **Hard**: `invalid_day` (day not in the 7-day enum), `invalid_start_time` (not normalizable
  to `HH:MM`), `missing_course_code`, `missing_program_group`.
- **Soft**: `invalid_end_time`, `end_not_after_start` (end <= start — overlaps/zero-length
  blocks from source quirks are surfaced, not silently imported), `missing_course_title`.

Pipeline-level guards:

- **Run report** — `{ tables, cells, kept, dropped, flagged, flaggedSamples[≤50], groups }`
  returned by every run; the Base44 sync includes it in its HTTP response so a bad
  semester upload is visible immediately.
- **Source sanity gate** — the sync refuses to wipe the database unless at least
  `200` entries parse (a shape change that yields near-zero data aborts with the report
  and leaves the DB untouched).
- **Fetch guard** — non-2xx HTTP aborts before any parse; runner exits non-zero so CI/cron
  can alert.
- **Empty parse guard** — zero entries = non-zero exit / HTTP 500 with report.
- **No destructive normalization** — labels that don't map to official codes are preserved
  verbatim rather than discarded, so ward/rotation blocks (`CD-MED/PCH`, `AB-OBG/SUG`)
  that carry real classes via `shared_with` survive imports.
- **Idempotence** — imports are full refreshes keyed on the natural key, so re-running on
  an unchanged source produces an identical dataset (no duplicate accumulation).

### Maintenance notes for future semesters

- If the university adds a new programme: add its code to `PROGRAMME_CODES` **and**
  `src/data/faculties.js`.
- If new alias spellings appear: add them to `CODE_ALIASES`.
- Notes are linked by natural key; canonicalizing an alias group changes its key
  (e.g. `MLC III|...` → `MLS III|...`), so run any key migration before the first sync
  of a semester that introduces new aliases.