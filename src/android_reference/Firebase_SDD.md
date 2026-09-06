# MUST Scholar — Firebase Integration & Web→Android Port
## Software Design Document (SDD / TDD)

**Project:** MUST Scholar — Mbarara University Timetable & Academic Companion
**Firebase Project:** `mustttdb` (MustttDB) — existing project, Spark (free) plan
**Source of truth:** the **web app** in this repo (`src/` + `base44/`)
**Status:** Design (not yet implemented)

---

## 0. Guiding Principle — The Web App Is the Spec

The existing `android_reference/` folder is **deprecated as a baseline** — it was
generated speculatively, diverges from the shipped product, and its UI does not
match the web design. It must not be treated as a starting point.

Instead, this document derives the Android app from the **web app as actually
built and running** in this repo. Every data model, behavior, flow and visual
below is taken from the real web source files — not remembered or approximated:

| Web source (canonical) | Role in this spec |
|---|---|
| `base44/entities/TimetableEntry.jsonc`, `LectureNote.jsonc`, `Assignment.jsonc`, `CustomEvent.jsonc` | The data model (field names below are verbatim) |
| `src/pages/Home.jsx`, `Notes.jsx`, `Tasks.jsx`, `Calendar.jsx`, `Settings.jsx`, `Welcome.jsx` | Screens + flows |
| `src/lib/timetableUtils.js` | `DAYS`, `dedupeShared`, `findNextUp`, `naturalKey`, `sessionStyle`/`SESSION_STYLES` |
| `src/lib/theme.jsx` | Dark mode + 4 accent colors, persisted |
| `src/components/timetable/*`, `BottomNav.jsx`, `AppLayout.jsx` | Visual design (gradients, pills, spacing) |
| `base44/functions/refreshTimetable/entry.ts` | Timetable scraper/upsert (moves to Firebase, §7.4) |

**Rule for implementation:** when the Android code and the web code disagree, the
web code wins. Anything not in the web app does not get invented for Android.

---

## 1. Purpose & Scope

1. Replace the web app's Base44 backend (entities + `refreshTimetable` function)
   with **Firebase** on Android: Cloud Firestore + Firebase Auth (Google Sign-In),
   within Spark quotas.
2. Build the **Android (Kotlin / Jetpack Compose)** app as an accurate port of the
   web app: same data model, same behaviors, same visual design.
3. Preserve the app's core guarantees: offline-first, natural-key note persistence,
   high-precision reminders, free tier.

**Out of scope:** iOS, changing the web app itself, FCM push, analytics.

---

## 2. Canonical Spec — What the Web App Actually Does

### 2.1 Data model (verbatim field names — these ARE the contract)

**TimetableEntry** — shared catalogue (1,348 records at last export):
```
program_group: string   e.g. "MBR I"
day: string             "Monday".."Sunday" (full names)
time_slot: string
start_time: string      "HH:MM"
end_time: string
course_code: string
course_title: string
session_type: string|null   "THEORY" | "PRACTICAL" | "CLINICAL"
lecturer: string
room: string
shared_with: string[]   other programme groups present in the same class
required: [program_group, day, start_time, course_code]
```

**LectureNote** — one per class, keyed by natural key:
```
natural_key: string     "program_group|course_code|day|start_time"  ← format from src/lib/timetableUtils.js
content: string         default ""
alarm_minutes: number|null   15 | 30 | 60, null = off
required: [natural_key]
```

**Assignment** (web "Tasks"):
```
title, course_code, due_date (ISO date-time), reminder_minutes: number|null,
priority: "low"|"medium"|"high" (default "medium"), notes, completed: boolean
required: [title]
```

**CustomEvent** (personal events):
```
title, day ("Monday".."Sunday"), start_time, end_time, location, notes,
repeat_weekly: boolean (default true), alarm_minutes: number|null
required: [title, day, start_time]
```

### 2.2 Core behaviors (all from web source)

- **Programme selection** — persisted in `localStorage["must_programme"]`; user's
  day view = entries where `program_group === programme` **OR** `shared_with`
  contains it, then `dedupeShared()` collapses multi-group classes into one card.
- **Next Up** — `findNextUp()` scans forward across days for the next entry.
- **Timetable update** — `refreshTimetable` backend function scrapes the public
  MUST timetable and **upserts by natural key**; client reloads and re-renders.
  This upsert-by-natural-key is *why notes survive timetable updates* — the note
  table is untouched because its key is independent of row IDs.
- **Reminders (web)** — `requestNotificationPermission()` + in-app scheduler; set
  per item at 15/30/60 minutes before.
- **Day navigation** — horizontal swipe between days; long-press a day pill for
  quick-add of a personal event.
- **Notes tab** — lists every saved note grouped by course, fully editable.
- **Tasks tab** — CRUD assignments with priority filtering and completion toggling.
- **Calendar tab** — month grid + itemized day sheets (classes, events, deadlines).
- **Theme** — Dark mode + 4 accent colors, persisted (`src/lib/theme.jsx`).
- **Welcome screen** — shown only on first launch (persisted flag).
- **No auth** on web — direct access. Android adds Google Sign-In *only* because
  Firestore per-user security requires it (§6.5); sign-out must never lock the
  app out of its local data.

### 2.3 Visual design system (the part the old Android port got wrong)

Derived from the web components; these tokens are **normative** for Android:

- **Layout**: max-width centered column, 16dp screen padding, 12dp card gaps
  (`space-y-3`), 24dp card corner radius on the hero card, 16dp on list cards.
- **Home header**: 40dp circular avatar "M" on `primary/15`; greeting text;
  programme subtitle; 36dp circular "+" button (`surfaceVariant`); green pill
  "Synced" (`green-100` bg `#DCFCE7`, `green-700` text `#15803D`, 6dp green dot).
- **H1**: "Your Schedule Today", bold.
- **Next Up card**: horizontal gradient `accent → #6D28D9` (violet-700),
  24dp radius, "NEXT UP" white pill (`white/20` bg), countdown text right,
  session label pill, 24sp bold title, meta rows (clock + room), footer with
  lecturer first name + 36dp white circle holding accent chevron.
- **Day pills**: 16dp-radius pills (not circles), selected = solid `primary`,
  today = 2dp `primary` ring when not selected, day abbrev + date number.
- **Lecture cards**: 16dp radius, 1dp `outline` border, 4dp left accent bar in
  session color, book icon + `course_code`, session badge (10sp white on badge
  color), 16sp semibold title, time + room meta row, lecturer first name,
  "+N" when shared, 32dp `primary/10` circle with chevron.
- **Session styles** (`SESSION_STYLES`): Theory `#2563EB` badge/`#3B82F6` bar,
  Practical `#16A34A`/`#22C55E`, Clinical `#D97706`/`#F59E0B`; fallback slate.
  Session type inferred from `session_type`, else room contains "LAB" → practical,
  else title contains CLINICAL/WARD → clinical, else theory.
- **Event cards**: purple family — bg `#FAF5FF`, border `#D8B4FE`
  (dashed on web; nearest Compose equivalent allowed), `#A855F7` bar,
  "PERSONAL" `#9333EA` label, bell icon if alarm set.
- **Bottom nav**: divider, then 5 tabs — active tab icon in a 44×32dp
  `primary/15` rounded-full pill + 10sp primary label.
- **Welcome**: indigo gradient (`#E0E7FF → #EEF2FF`), decorative book icons,
  stacked blue bars ("MUST" on `#1D4ED8`), rounded top-3xl white card,
  "MUSTimetable" brand row, "Start Learning Today", Get Started button +
  circular indigo arrow, Alarms/Notes/Offline feature icon row.
- **Theme tokens** (web `src/index.css`): light bg `#FFFFFF`, fg `#0A0A0A`,
  surface-variant `#F5F5F5`, on-surface-variant `#737373`, border `#E5E5E5`;
  dark bg `#0A0A0A`, fg `#FAFAFA`, surface-variant `#262626`,
  on-surface-variant `#A3A3A3`, border `#262626`. Accents: Indigo `#4F46E5`,
  Purple `#6D28D9`, Green `#059669`, Orange `#F97316`.

---

## 3. Can Firebase Work? — Yes

- Firestore offline cache exists, but the app needs structured local queries
  (dedupe, conflict detection, alarm scheduling) → **Room stays the source of
  truth**, Firestore is the cloud replica.
- Google Sign-In gives per-user isolation (§7 rules) — the one intentional
  addition over the web's direct-access model.
- Spark quotas are ample at this scale (§8).
- `refreshTimetable`'s scrape+upsert moves to a Firebase-compatible importer
  (§7.4) — **scheduled Cloud Functions are NOT available on Spark**, so the
  importer is an Admin-SDK script run manually (weekly/semesterly), which also
  matches how the web sync button is a manual action today.

---

## 4. Target Android Architecture

```
Compose UI  ←  exact port of web screens (§2.2 behaviors, §2.3 visuals)
   │  StateFlow
ViewModels  ←  port of web page logic (dedupeShared, findNextUp, filters)
   │
Room DAOs   ←  SINGLE SOURCE OF TRUTH (tables mirror §2.1 exactly,
   │           column names = web field names, snake_case)
   │ local writes ──mirror──► SyncRepository ──► Firestore (best-effort,
   │                                                  SDK queues offline)
   └── snapshot listeners ◄── Firestore deltas → upsertByNaturalKey
AlarmScheduler / BootReceiver  ←  reads Room only (as on web)
AuthGate  ←  Google Sign-In gates cloud sync, never local access
```

**Data model rule:** Android Room entities and Firestore documents use the
**web field names verbatim** (`program_group`, `start_time`, `shared_with`,
`natural_key`, `alarm_minutes`, …) with a String→String mapping table in
`timetableUtils.kt`. No camelCase reinterpretation — the natural key string is
byte-identical to the web's `program_group|course_code|day|start_time`.

**Behavior rule:** `dedupeShared`, `findNextUp`, `sessionStyle`, `DAYS` are ported
1:1 from `src/lib/timetableUtils.js` into `core/util/TimetableUtils.kt`, including
the same day ordering (`Monday..Sunday`) and the same "first programme group
wins" dedupe logic.

---

## 5. Firestore Data Model (web field names)

```
timetable/{naturalKey}                    ← shared; naturalKey = program_group|course_code|day|start_time
  program_group, day, time_slot, start_time, end_time,
  course_code, course_title, session_type, lecturer, room, shared_with[]

users/{uid}
  /notes/{natural_key}                    ← LectureNote; doc ID = natural_key
    content, alarm_minutes, updated_at
  /events/{event_key}                    ← CustomEvent; event_key = title|day|start_time
    title, day, start_time, end_time, location, notes,
    repeat_weekly, alarm_minutes, updated_at
  /assignments/{assignment_key}          ← Assignment; key = title|due_date
    title, course_code, due_date, reminder_minutes, priority, notes,
    completed, updated_at
  /profile/settings
    programme, theme_dark, theme_accent, last_synced_at
```

`updated_at: FieldValue.serverTimestamp()` resolves last-writer-wins for the rare
same-key edit on two devices.

---

## 6. Firebase Configuration

### 6.1 Console checklist (owner actions, project `mustttdb` — no new project)
1. Firestore → Create database (production mode), region `europe-west1`.
2. Authentication → Sign-in method → **Google**; register the Android app with
   package name + **debug AND release SHA-1** (`./gradlew signingReport`).
3. Download `google-services.json` → `app/google-services.json`.
4. Offline persistence is on by default in the Android SDK.

### 6.2 Gradle
```kotlin
// root
plugins { id("com.google.gms.google-services") version "4.4.2" apply false }
// app
plugins { id("com.google.gms.google-services") }
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
}
```

### 6.3 Initialization
No manual init file needed (the `google-services` plugin auto-initializes via a
`ContentProvider`; `google-services.json` **is** the config). Thin accessor:

```kotlin
// core/network/FirebaseFactory.kt
object FirebaseFactory {
    val auth: FirebaseAuth by lazy { Firebase.auth }
    val db: FirebaseFirestore by lazy { Firebase.firestore }
    val uid: String get() = auth.currentUser?.uid
        ?: error("Sync called before sign-in")
}
```

### 6.4 Timetable importer (replaces `refreshTimetable`)
Python/Kotlin Admin-SDK script, run manually by the maintainer:
- Scrape exactly as `base44/functions/refreshTimetable/entry.ts` does today
  (same source page, same parsing, same field mapping to §2.1).
- `set(..., merge=True)` per natural key → **upsert**, never delete+recreate,
  mirroring the web function's guarantee that notes survive updates.
- Clients never write `timetable/`; they listen and upsert into Room.

### 6.5 Auth flow
```
App start → first-launch flag (port of web Welcome) → WelcomeScreen
          → AuthGate: signed in? → MainScaffold
                        else    → SignInScreen (Google, Credential Manager)
```
- `serverClientId` = the **Web client ID** from the Google provider config.
- Settings gains **Sign out** = stop listeners + `auth.signOut()`; local Room
  data is retained (offline-first; matches web's no-auth freedom).
- Google Sign-In is the *only* addition to the web behavior set — everything
  else must behave identically to the web app.

---

## 7. Security Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // User-owned data: only the owner (path-enforced)
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null
                         && request.auth.uid == uid;
    }

    // Shared timetable: readable by any signed-in user, written only by
    // the Admin SDK importer (bypasses rules)
    match /timetable/{docId} {
      allow read:  if request.auth != null;
      allow write: if false;
    }

    // Default deny
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

Deploy with `firebase deploy --only firestore:rules`; test with the emulator
(user A cannot read `users/B/...`, unauthenticated reads nothing).

---

## 8. Spark Quota Analysis

| Resource | Spark/day | Estimate |
|---|---|---|
| Reads | 50,000 | ~1,350 on first timetable pull (1,348 docs) + small deltas after; listeners cost reads only on change |
| Writes | 20,000 | < 100 (user notes/events/assignments) |
| Deletes | 20,000 | < 20 |
| Storage | 1 GiB | ~5–10 MB |

Design rules: **one listener per collection**, attached once at sign-in (never
per screen); snapshot listeners not polling; flat denormalized timetable docs.
At ~35 *new* users/day the first-sync reads approach the daily read cap — that
is the point to consider Blaze (same free 50K/day, just no hard stop).

---

## 9. Web → Android Artifact Mapping

New clean module structure (old `android_reference/` is not reused):

| Android artifact (new) | Ported from (web source) |
|---|---|
| `ui/theme/Theme.kt` | `src/lib/theme.jsx` + `src/index.css` tokens (§2.3) |
| `ui/home/TimetableScreen.kt` | `src/pages/Home.jsx` |
| `ui/home/ProgrammeSelector.kt` | `src/components/timetable/ProgrammeSelector.jsx` |
| `ui/home/NextUpCard.kt` | `src/components/timetable/NextUpCard.jsx` (gradient spec §2.3) |
| `ui/home/DaySelector.kt` | `src/components/timetable/DaySelector.jsx` (pills, today ring, long-press quick-add) |
| `ui/home/LectureCard.kt` | `src/components/timetable/LectureCard.jsx` |
| `ui/home/EventCard.kt` | `src/components/timetable/EventCard.jsx` |
| `ui/home/LectureDetailSheet.kt` | `src/components/timetable/LectureDetailSheet.jsx` (badge, 15/30/60, note + natural-key caption, Save) |
| `ui/home/EventSheet.kt` | `src/components/timetable/EventSheet.jsx` |
| `ui/notes/NotesScreen.kt` | `src/pages/Notes.jsx` (grouped by course, fully editable) |
| `ui/tasks/TasksScreen.kt` + sheet | `src/pages/Tasks.jsx` + `src/components/tasks/AssignmentSheet.jsx` |
| `ui/calendar/CalendarScreen.kt` | `src/pages/Calendar.jsx` (month grid + day sheet) |
| `ui/settings/SettingsScreen.kt` | `src/pages/Settings.jsx` + Sign out (§6.5) |
| `ui/welcome/WelcomeScreen.kt` | `src/pages/Welcome.jsx` (§2.3) |
| `ui/nav/MainScaffold.kt` | `src/components/BottomNav.jsx` + `AppLayout.jsx` |
| `core/util/TimetableUtils.kt` | `src/lib/timetableUtils.js` (DAYS, naturalKey, dedupeShared, findNextUp, sessionStyle) |
| `core/util/AlarmScheduler.kt` + `BootReceiver` | `src/lib/alarms.js` + `useAlarmScheduler` semantics on AlarmManager |
| `data/` Room entities + DAOs | `base44/entities/*.jsonc` verbatim (§2.1) |
| `core/sync/SyncRepository.kt` | new (§10) |
| `core/auth/AuthGate.kt`, `SignInScreen.kt` | new (§6.5) |

### `SyncRepository.kt` sketch (web field names)

```kotlin
class SyncRepository(private val daos: Daos) {
    private val db get() = FirebaseFactory.db
    private var listeners = listOf<ListenerRegistration>()

    fun startAll() {
        val user = db.collection("users").document(FirebaseFactory.uid)
        listeners = listOf(
            listenTimetable(db.collection("timetable")),
            listenCollection(user.collection("notes"), daos.notes()::upsertByNaturalKey),
            listenCollection(user.collection("events"), daos.events()::upsertByNaturalKey),
            listenCollection(user.collection("assignments"), daos.assignments()::upsertByNaturalKey)
        )
    }
    fun stopAll() = listeners.forEach { it.remove() }

    private fun listenTimetable(ref: CollectionReference) =
        ref.addSnapshotListener { snap, _ -> snap?.documentChanges?.forEach { c ->
            val e = c.document.toTimetableEntry()          // doc.id == natural_key
            when (c.type) {
                ADDED, MODIFIED -> daos.timetable().upsertByNaturalKey(e)
                REMOVED         -> daos.timetable().deleteByNaturalKey(e.naturalKey)
                else -> {}
        }}}

    /** Local-first mirror — called after every Room write. */
    suspend fun pushNote(note: LectureNote) = db.collection("users")
        .document(FirebaseFactory.uid).collection("notes")
        .document(note.naturalKey)                           // natural key = doc ID
        .set(note.toDto().plus("updated_at" to FieldValue.serverTimestamp()),
             SetOptions.merge())
}
```

DAO upsert (the mechanism that preserves notes across timetable updates —
identical semantics to the web importer):
```sql
INSERT INTO timetable_entries (...) VALUES (...)
ON CONFLICT(program_group, course_code, day, start_time)
DO UPDATE SET time_slot=excluded.time_slot, ... -- natural key = unique index
```

---

## 10. Data Flow — Save & Load

**Save (edit a lecture note):** sheet → write Room immediately (works offline) →
`pushNote` mirrors to Firestore (SDK queues if offline) → other devices' listener
upserts into their Room via natural key.

**Load (cold start):** render instantly from Room → `startAll()` attaches
listeners → deltas upsert via natural key → DAO Flows re-emit → Compose recomposes.

**Timetable update:** importer merges `timetable/*` docs → client listeners upsert
into Room → UI updates. Notes, events and assignments are separate user-owned
collections keyed independently — **note persistence across timetable updates is
guaranteed by construction**, exactly as on the web today.

---

## 11. Testing Plan

| Test | Checks |
|---|---|
| Parity walkthrough | Each web screen behavior reproduced step-by-step on Android (swipe, long-press quick-add, dedupe of shared classes, next-up countdown, priority filter, month grid) |
| Visual parity | Side-by-side against web for all §2.3 tokens, light + dark × 4 accents |
| Offline | Airplane-mode note edit → reboot → present; reconnect → 2nd device receives |
| Natural-key survival | Re-run importer → notes intact |
| Rules | Emulator suite: cross-user denied, unauthenticated denied, timetable read-only |
| Quotas | Usage tab after a simulated week |
| Alarms | Boot + timezone-change regression |

---

## 12. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Android drifts from web again | §0 rule: web source is the only reference; parity walkthrough is a release gate (§11) |
| SHA-1 mismatch breaks Google Sign-In | Register debug + release fingerprints up front |
| Listener storms burn reads | Attach once at sign-in, never per screen |
| Spark: no scheduled functions | Manual Admin-SDK importer (matches the web's manual Sync button) |
| Multi-device same-key edit | `updated_at` server timestamp, last-writer-wins |
| Firestore region latency | Choose `europe-west1` at creation (immutable) |

---

## 13. Implementation Order

1. Console checklist + `google-services.json` + Gradle (§6)
2. Room schema + entities from §2.1 (verbatim) + DAO upserts by natural key
3. `TimetableUtils.kt` ported from `src/lib/timetableUtils.js`
4. AuthGate + SignInScreen (end-to-end Google Sign-In)
5. Deploy rules + emulator tests (§7)
6. Theme (§2.3 tokens) + all screens ported web-first, one screen at a time,
   verifying against the live web preview before moving on
7. SyncRepository + listeners (notes first, then events/assignments/timetable)
8. Importer script (port of `refreshTimetable`) + full offline/conflict test pass