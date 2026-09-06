import { createClientFromRequest } from 'npm:@base44/sdk@0.8.40';
import { buildEntries, TIMETABLE_URL } from '../../shared/timetablePipeline.js';

// Safety floor: refuse to wipe the DB when the source page changes shape
// and yields implausibly little data (current healthy dataset is ~1300+ entries).
const MIN_EXPECTED_ENTRIES = 200;

export default async function(req) {
  try {
    const base44 = createClientFromRequest(req);
    const user = await base44.auth.me();
    if (!user) return Response.json({ error: 'Unauthorized' }, { status: 401 });
    if (user.role !== 'admin') return Response.json({ error: 'Admin only' }, { status: 403 });

    const res = await fetch(TIMETABLE_URL, { headers: { 'User-Agent': 'MUST-Scholar/1.0' } });
    if (!res.ok) return Response.json({ error: `Fetch failed: ${res.status}` }, { status: 502 });
    const html = await res.text();

    const { entries, report } = buildEntries(html);
    if (entries.length < MIN_EXPECTED_ENTRIES) {
      return Response.json(
        {
          error: `Source looks malformed: parsed ${entries.length} entries (min ${MIN_EXPECTED_ENTRIES}). DB left untouched.`,
          report,
        },
        { status: 500 }
      );
    }

    // Full refresh: notes are linked by natural_key, so clearing timetable rows is safe.
    await base44.entities.TimetableEntry.deleteMany({});
    for (let i = 0; i < entries.length; i += 400) {
      await base44.entities.TimetableEntry.bulkCreate(entries.slice(i, i + 400));
    }

    return Response.json({
      ok: true,
      source: TIMETABLE_URL,
      imported: entries.length,
      report,
    });
  } catch (error) {
    return Response.json({ error: error.message }, { status: 500 });
  }
}