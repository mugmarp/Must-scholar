#!/usr/bin/env node
/**
 * Standalone MUST timetable scraper — runs in any Node.js >= 18 environment,
 * completely independent of Base44.
 *
 * Usage:
 *   node scraper/scrape.mjs                        # scrape live source -> timetable.json
 *   node scraper/scrape.mjs --url <url>            # custom source URL
 *   node scraper/scrape.mjs --file index.html      # parse a saved HTML file instead
 *   node scraper/scrape.mjs --out data.json        # output path (default: timetable.json)
 *   node scraper/scrape.mjs --pretty               # pretty-print the JSON
 *
 * Deployment note: copy `scraper/` together with `base44/shared/timetablePipeline.js`
 * (the engine this runner imports). No npm install needed — stdlib + global fetch only.
 */
import { readFile, writeFile } from "node:fs/promises";
import { buildEntries, TIMETABLE_URL } from "../base44/shared/timetablePipeline.js";

const args = process.argv.slice(2);
const get = (name, fallback = null) => {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
};
const url = get("--url", TIMETABLE_URL);
const file = get("--file");
const out = get("--out", "timetable.json");
const pretty = args.includes("--pretty");

let html;
let source;
if (file) {
  html = await readFile(file, "utf8");
  source = file;
} else {
  const res = await fetch(url, { headers: { "User-Agent": "MUST-Scholar-Scraper/1.0" } });
  if (!res.ok) {
    console.error(`Fetch failed: HTTP ${res.status} for ${url}`);
    process.exit(1);
  }
  html = await res.text();
  source = url;
}

const { entries, report } = buildEntries(html);
if (!entries.length) {
  console.error("No entries parsed — source layout may have changed.");
  console.error(JSON.stringify(report, null, 2));
  process.exit(1);
}

const payload = {
  generated_at: new Date().toISOString(),
  source,
  count: entries.length,
  entries,
  report,
};

await writeFile(out, JSON.stringify(payload, null, pretty ? 2 : 0), "utf8");
console.log(`Wrote ${entries.length} entries to ${out}`);
console.log(
  `Report: ${report.tables} tables, ${report.cells} cells, kept ${report.kept}, dropped ${report.dropped}, flagged ${report.flagged}, ${report.groups} groups`
);
if (report.flaggedSamples.length) {
  console.log("Flagged samples:");
  for (const s of report.flaggedSamples) console.log(` - ${JSON.stringify(s)}`);
}