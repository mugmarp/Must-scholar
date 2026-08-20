import { createClientFromRequest } from 'npm:@base44/sdk@0.8.40';

const TIMETABLE_URL = "https://timetable.must.ac.ug/index_teaching.html";

// Derive a course code for non-standard cells (special activities, dual listings).
function fallbackCode(title) {
  const t = title.replace(/[*#]+/g, '').trim();
  const m = t.match(/^([A-Za-z][A-Za-z0-9-]{2,})\s+/);
  if (m) return m[1].toUpperCase();
  return (t.replace(/[^A-Za-z0-9]+/g, '').toUpperCase().slice(0, 12)) || 'CLASS';
}

// Parse a cell's <br />-separated lines into structured fields.
function cellToFields(content) {
  const lines = content
    .split(/<br\s*\/?>/)
    .map(l => l.replace(/&amp;/g, '&').replace(/&nbsp;/g, ' ').replace(/\s+/g, ' ').trim())
    .filter(l => l.length);
  if (!lines.length) return null;

  const sharedRaw = lines[0].split(',').map(s => s.trim()).filter(Boolean);

  let courseCode = null, courseTitle = null, sessionType = null;
  if (lines[1]) {
    // Standard code (up to 5 digits, optional trailing letter), optionally a second code after "/".
    const cm = lines[1].match(/^([A-Z]{2,5}\s?\d{3,5}[A-Z]?(?:\s*\/\s*[A-Z]{2,5}\s?\d{3,5}[A-Z]?)?)\s+(.+)$/);
    if (cm) {
      courseCode = cm[1].replace(/\s+/g, '').toUpperCase();
      courseTitle = cm[2].trim();
    } else {
      courseTitle = lines[1].replace(/[*#]+/g, '').trim();
      courseCode = fallbackCode(courseTitle);
    }
    const st = courseTitle.match(/\s+(THEORY|PRACTICAL|CLINICAL|LAB)$/i);
    if (st) {
      sessionType = st[1].toUpperCase();
      courseTitle = courseTitle.slice(0, st.index).trim();
    }
  }

  let lecturer = null, room = null;
  if (lines.length >= 4) {
    lecturer = lines[2] || null;
    room = lines[3] || null;
  } else if (lines.length === 3) {
    room = lines[2] || null;
  }
  if (room && /^(MON|TUE|WED|THU|FRI|SAT|SUN)/i.test(room)) room = null;

  return { sharedRaw, courseCode, courseTitle, sessionType, lecturer, room };
}

// Fetch the published HTML and return normalized TimetableEntry records.
function buildEntries(html) {
  const tableRegex = /<table[^>]*id="table_\d+"[^>]*>([\s\S]*?)<\/table>/g;
  const entries = [];
  let m;
  while ((m = tableRegex.exec(html)) !== null) {
    const inner = m[1];
    const grpMatch = inner.match(/<th\s+colspan="7"[^>]*>([\s\S]*?)<\/th>/);
    const group = grpMatch ? grpMatch[1].replace(/<[^>]+>/g, '').trim() : null;
    if (!group) continue;

    const days = [...inner.matchAll(/<th class="xAxis">([^<]+)<\/th>/g)].map(x => x[1].trim());
    const tbodyMatch = inner.match(/<tbody>([\s\S]*?)<\/tbody>/);
    if (!tbodyMatch) continue;

    const tbody = tbodyMatch[1];
    const rowHtmls = [...tbody.matchAll(/<tr>([\s\S]*?)<\/tr>/g)].map(r => r[1]);
    const rows = rowHtmls.map(rowHtml => {
      const tsMatch = rowHtml.match(/<th class="yAxis">([^<]+)<\/th>/);
      const timeSlot = tsMatch ? tsMatch[1].trim() : null;
      const realCells = [];
      const tdRegex = /<td([^>]*)>([\s\S]*?)<\/td>/g;
      let c;
      while ((c = tdRegex.exec(rowHtml)) !== null) {
        const rs = c[1].match(/rowspan="(\d+)"/);
        realCells.push({ rowspan: rs ? parseInt(rs[1]) : 1, content: (c[2] || '').trim() });
      }
      return { timeSlot, realCells };
    });

    const numCols = days.length;
    const pending = new Array(numCols).fill(0);
    rows.forEach((row, ri) => {
      let col = 0;
      for (const cell of row.realCells) {
        while (col < numCols && pending[col] > 0) { pending[col]--; col++; }
        if (col >= numCols) break;
        if (cell.content) {
          const f = cellToFields(cell.content);
          if (f && f.courseCode) {
            const ts = row.timeSlot || '';
            const start_time = ts.split('-')[0].trim();
            const endSlot = rows[ri + cell.rowspan - 1];
            const end_time = endSlot ? (endSlot.timeSlot || '').split('-')[1].trim() : null;
            const shared = f.sharedRaw.filter(g => g.toUpperCase() !== group.toUpperCase());
            entries.push({
              program_group: group,
              day: days[col],
              time_slot: ts,
              start_time,
              end_time,
              course_code: f.courseCode,
              course_title: f.courseTitle,
              session_type: f.sessionType,
              lecturer: f.lecturer,
              room: f.room,
              shared_with: shared
            });
          }
        }
        if (cell.rowspan > 1) pending[col] = cell.rowspan - 1;
        col++;
      }
      while (col < numCols) { if (pending[col] > 0) pending[col]--; col++; }
    });
  }
  return entries;
}

export default async function(req) {
  try {
    const base44 = createClientFromRequest(req);
    const user = await base44.auth.me();
    if (!user) return Response.json({ error: 'Unauthorized' }, { status: 401 });
    if (user.role !== 'admin') return Response.json({ error: 'Admin only' }, { status: 403 });

    const res = await fetch(TIMETABLE_URL, { headers: { 'User-Agent': 'MUST-Scholar/1.0' } });
    if (!res.ok) return Response.json({ error: `Fetch failed: ${res.status}` }, { status: 502 });
    const html = await res.text();

    const entries = buildEntries(html);
    if (!entries.length) return Response.json({ error: 'No entries parsed' }, { status: 500 });

    // Full refresh: notes are linked by natural_key, so clearing timetable rows is safe.
    await base44.entities.TimetableEntry.deleteMany({});
    for (let i = 0; i < entries.length; i += 400) {
      await base44.entities.TimetableEntry.bulkCreate(entries.slice(i, i + 400));
    }

    return Response.json({
      ok: true,
      source: TIMETABLE_URL,
      imported: entries.length,
      groups: new Set(entries.map(e => e.program_group)).size
    });
  } catch (error) {
    return Response.json({ error: error.message }, { status: 500 });
  }
}