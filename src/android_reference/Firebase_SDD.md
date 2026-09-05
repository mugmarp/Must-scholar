# MUST Scholar — Firebase Integration Software Design Document (SDD/TDD)

**Project:** MUST Scholar (Mbarara University Timetable & Academic Companion — Android)
**Target Firebase Project:** `mustttdb` (MustttDB) — existing project, Native Blaze-free (Spark) plan
**Platform:** Kotlin · Jetpack Compose · Room · MVVM
**Status:** Design (not yet implemented)

---

## 1. Purpose & Scope

This document specifies how to migrate MUST Scholar's backend from the current
Base44/Retrofit HTTP sync to **Firebase** (Cloud Firestore + Firebase Authentication
with Google Sign-In) while preserving the app's core design guarantees:

- **Offline-first**: the app is fully usable with no network.
- **Natural-key persistence**: lecture notes survive timetable updates because they
  are keyed by `program_group|course_code|day|start_time`, never by row IDs.
- **High-precision alarms**: AlarmManager + BootReceiver keep working regardless of sync.
- **Free tier**: everything must run within the Spark plan quotas.

**Out of scope:** iOS, web client, push notifications (FCM), analytics.

---

## 2. Current Architecture (Baseline)

| Concern | Current Implementation |
|---|---|
| Local persistence | Room (`timetable_entries`, `lecture_notes`, `assignments`, `custom_events`) |
| Identity of records | Composite **business natural keys** (not auto-increment IDs) |
| Cloud sync | Retrofit → Base44 backend (`refreshTimetable` function, ETag caching) |
| Auth | None (device-local app) |
| Reminders | `AlarmScheduler` / `ReminderScheduler` + `BootReceiver`, rescheduled after boot |
| Notes | Room only; keyed by `natural_key` column |

**Key domain models (unchanged):** `TimetableEntry`, `LectureNote`, `Assignment`,
`CustomEvent` — all already carry natural keys, which map cleanly onto Firestore
document IDs (see §5).

---

## 3. Can Firebase Work? — Decision Analysis

**Yes.** Firebase is a strong fit for this app for three reasons:

1. **Firestore has offline persistence built in**, which matches the offline-first
   requirement — but Room is *already* the source of truth with richer query needs
   (joins for conflict detection, alarm scheduling). So we keep Room as canonical
   and use Firestore as the cloud replica (see §4 for the two considered options).
2. **Google Sign-In** replaces the "no auth" model cheaply and is a prerequisite for
   per-user data isolation in Firestore.
3. **Spark plan is sufficient** at this app's scale (quota analysis in §8).

### Options Considered

| | Option A — Firestore cache only (drop Room) | Option B — Room + Firestore sync (chosen) |
|---|---|---|
| Offline | Firestore local cache | Room (unchanged behavior) |
| Alarm queries | Firestore queries (slower, cache-dependent) | Direct DAO (fast, guaranteed) |
| Migration cost | Rewrite of every DAO/ViewModel | Additive: new `SyncRepository` |
| Conflict detection (events vs classes) | Client-side joins on query results | Existing SQL stays |
| Risk | Cache eviction → alarms missed | Slightly more code (mapper layer) |

**Decision: Option B.** Room remains the single source of truth for UI and alarms;
Firestore mirrors user-owned collections and the shared timetable. Retrofit/Base44
sync is retired (timetable import moves to Firestore — §6.4).

---

## 4. Target Architecture

```
┌──────────────────────────────────────────────────────────┐
│                        UI (Compose)                       │
│   TimetableScreen · Notes · Tasks · Calendar · Settings   │
└───────────────┬───────────────────────────▲──────────────┘
                │ write/read                │ StateFlow
┌───────────────▼───────────────────────────┴──────────────┐
│                    ViewModel layer (MVVM)                 │
└───────────────┬───────────────────────────▲──────────────┘
                ▼                            │
┌──────────────────────────────────────────────────────────┐
│      Room DAOs  ◄──── SINGLE SOURCE OF TRUTH ────►  Alarm │
│                                                    Sched. │
└───────▲───────────────────────────────┬──────────────────┘
        │ local writes (mirror out)     │ listen (push in)
┌───────┴───────────────────────────────▼──────────────────┐
│              SyncRepository (new, Firebase)                │
│   • Outbox: Room writes → Firestore writes                │
│   • Listeners: Firestore snapshots → Room upserts        │
│   • Natural-key upserts (no row-ID coupling)             │
└───────┬───────────────────────────────┬──────────────────┘
        ▼                               ▼
┌───────────────────────┐   ┌──────────────────────────────┐
│ Firebase Auth         │   │ Cloud Firestore (mustttdb)   │
│ (Google Sign-In)      │   │ offline persistence ON       │
└───────────────────────┘   └──────────────────────────────┘
```

**Sync rules:**
- Local-first writes: UI always writes to Room immediately; `SyncRepository`
  mirrors the mutation to Firestore best-effort (with offline queueing via
  Firestore SDK's built-in retry).
- Remote-first reads: a **snapshot listener per collection** applies remote changes
  into Room with `upsertByNaturalKey()` DAO methods — the same pattern the Base44
  importer used, so note persistence guarantees are preserved.
- Timetable documents are **read-only for clients**; they arrive via listener and
  upsert into `timetable_entries`.

---

## 5. Firestore Data Model

Top-level layout — **user data lives under `users/{uid}`** (enables the §7 rules),
shared data in a root collection:

```
timetable/{naturalKey}                       ← shared, read-only for clients
  fields: TimetableEntry fields (programGroup, dayOfWeek, startTime, endTime,
          courseCode, courseTitle, sessionType, lecturer, room, sharedGroups[])

users/{uid}
  ────────────────────────────────────────
  /notes/{naturalKey}                        ← LectureNote
    content: string, alarmMinutes: int|null, updatedAt: serverTimestamp
  /events/{eventKey}                        ← CustomEvent (key: title|day|start)
    title, dayOfWeek, startTime, endTime?, location?, notes,
    repeatWeekly: bool, alarmMinutes: int|null
  /assignments/{assignmentKey}              ← Assignment (key: title|dueDate)
    title, courseCode, dueDate, reminderMinutes?, priority,
    notes, completed: bool
  /profile/{doc}
    displayName, programme (selected programme group), lastSyncAt
```

**Natural keys become Firestore document IDs.** `program_group|course_code|day|start_time`
maps to the doc ID (Firestore IDs allow `|`, letters, digits, `-`, `_`). Benefits:

- A note written on device A upserts onto the exact same doc on device B even if
  the timetable was re-imported in between — identical to the Room natural key.
- No server-side document-ID generation, so offline-created records merge without
  collisions.
- Timestamps (`updatedAt`) resolve last-writer-wins for the rare same-key edit.

---

## 6. Firebase Configuration & Console Steps (mustttdb)

> These must be done in the Firebase Console by the project owner — an agent/code
> generator cannot enable APIs on your behalf.

### 6.1 Console checklist (existing project — do **not** create a new one)

1. **Firestore**: Build → Firestore Database → Create database → Production mode,
   region `europe-west` (nearest to Uganda among low-latency regions) — or accept
   the default `nam5` if you prefer US multi-region.
2. **Authentication**: Build → Authentication → Sign-in method → enable **Google**.
   Add your Android app (package name, e.g. `com.must.timetable`) and both debug and
   release SHA-1 fingerprints (`./gradlew signingReport`).
3. **Download `google-services.json`** (Project settings → Your apps → Android) and
   place it in the app module root (`app/google-services.json`).
4. **Offline persistence** is enabled by default on Android — nothing to configure.

### 6.2 Gradle setup

```kotlin
// root build.gradle.kts
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// app/build.gradle.kts
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    // Firebase BOM keeps all versions aligned
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Credential Manager (modern Google Sign-In on Android)
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
}
```

### 6.3 Initialization — `FirebaseFactory.kt` (replaces the "firebase.ts" idea)

Kotlin/Compose does not need a manual init file: the `google-services` plugin +
`Firebase.initializeApp()` happen automatically via a `ContentProvider`. The only
"config file" needed is `google-services.json`. A thin accessor follows the app's
existing `AppGraph` pattern:

```kotlin
// core/network/FirebaseFactory.kt
object FirebaseFactory {
    val auth: FirebaseAuth by lazy { Firebase.auth }
    val db: FirebaseFirestore by lazy {
        Firebase.firestore.apply {
            // explicit, even though default on Android
            firestoreSettings = firestoreSettings { isPersistentCacheEnabled = true }
        }
    }
    val uid: String get() = auth.currentUser?.uid
        ?: error("Not signed in — gate all sync behind AuthGate")
}
```

### 6.4 Timetable import (replacing the Base44 `refreshTimetable` function)

The scraper currently runs server-side. On the Spark plan:

- **Scheduled Cloud Functions are NOT available** (Cloud Scheduler requires the
  Blaze plan).
- **Chosen approach:** run the import from the maintainer's machine/admin tool via
  the **Firebase Admin SDK** (Python or Kotlin script) with
  `SetOptions.merge` upserts keyed by natural key. Run it weekly/semesterly.
- Clients never write `timetable/*`; they just listen and upsert into Room.

### 6.5 Auth flow — `AuthGate` + Google Sign-In (Credential Manager)

```
App start → AppGraph.welcomed? → WelcomeScreen → AuthGate
AuthGate:
  FirebaseFactory.auth.currentUser == null  →  SignInScreen
  else                                       →  MainScaffold
SignInScreen:
  CredentialManager.getCredential(
      GetGoogleIdOption(serverClientId = WEB_CLIENT_ID, filterByAuthorizedAccounts = false))
  → GoogleIdTokenCredential → FirebaseAuth credential → signInWithCredential
  → on success, SyncRepository.startAllListeners()
```

Notes:
- `serverClientId` must be the **Web client ID** from Firebase Console
  (Authentication → Google → Web SDK config), not the Android one.
- A "Sign out" entry in Settings calls `auth.signOut()` + stops listeners; all
  local Room data stays (offline-first — sign-in only governs cloud sync).
- Anonymous/optional sign-in is intentionally rejected: it breaks §7's
  per-user isolation and burns Spark quota on unusable accounts.

---

## 7. Firestore Security Rules

`firestore.rules` — authenticated users can read/write **only their own** data;
timetable is readable by any signed-in user and writable by no client (Admin SDK
bypasses rules):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // ── User-owned data: only the owner ─────────────────────
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null
                         && request.auth.uid == uid;
    }

    // ── Shared timetable: read-only for all signed-in users ─
    // (written only via Admin SDK, which bypasses rules)
    match /timetable/{docId} {
      allow read:  if request.auth != null;
      allow write: if false;
    }

    // ── Everything else: deny by default ────────────────────
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

Deploy: `firebase deploy --only firestore:rules` (Firebase CLI).

**Why this is secure:** ownership is enforced by *path structure* — a user can
never guess another uid and read it, because their token must match the `{uid}`
segment. Firestore's default-deny plus explicit matches means no collection is
exposed by accident.

---

## 8. Spark Plan Quota Analysis

Free (Spark) limits per day, per project:

| Resource | Spark limit | MUST Scholar estimate |
|---|---|---|
| Reads | 50,000 docs | ~1,500–3,000 (initial timetable pull ≈ 1,400 docs; snapshot listeners deliver deltas after that) |
| Writes | 20,000 docs | < 100 (user notes/events/assignments) |
| Deletes | 20,000 docs | < 20 |
| Storage | 1 GiB | ~5–10 MB (timetable ≈ 1,400 × ~0.5 KB) |
| Egress | 10 GiB/mo | < 100 MB/mo |

**Design rules that keep it inside Spark:**
1. **Snapshot listeners, not polling** — listeners cost reads only when documents
   actually change; a repeated `get()` re-reads everything.
2. **One listener per collection** started once at sign-in (not per screen) —
   attaching/detaching repeatedly causes repeated initial reads.
3. **Timetable docs are flat and denormalized** — no `IN` query fan-outs.
4. **`limit()` + `orderBy(updatedAt)`** on user collections where pagination is
   ever needed.
5. With ~10–50 users the app stays far under every quota. At thousands of daily
   active users, reads from initial sync (~1,400/user) approach the 50K daily
   read limit around **35 new users/day** — at that point, Blaze's generous free
   tier (same 50K reads but no hard stop, billing only beyond) is the upgrade path.

---

## 9. New / Modified Artifacts

| File | New/Modified | Responsibility |
|---|---|---|
| `google-services.json` | New (downloaded) | Firebase config |
| `firestore.rules` | New | Security rules (§7) |
| `core/network/FirebaseFactory.kt` | New | Auth + Firestore singletons |
| `core/auth/AuthGate.kt` | New | Routes to SignInScreen or MainScaffold |
| `core/auth/SignInScreen.kt` | New | Google Sign-In (Credential Manager), themed like WelcomeScreen |
| `core/sync/SyncRepository.kt` | New | Listeners → Room upserts; local writes → Firestore |
| `core/sync/Mappers.kt` | New | Room entity ⇄ Firestore DTO (natural-key mapping) |
| `core/sync/OutboxWorker.kt` | New | WorkManager worker flushing queued writes when online |
| `core/network/ApiService.kt`, `ApiClient.kt`, `SafeApiCall.kt` | Deleted | Retrofit/Base44 sync retired |
| `data/TimetableRepository.kt` | Modified | Calls `SyncRepository` after local writes; drops ETag logic |
| `ui/SettingsScreen.kt` | Modified | Adds Sign out; drops Base44 sync button |
| `ui/MainScaffold.kt` | Modified | Wraps content in `AuthGate` |
| `AppGraph.kt` | Modified | Wires SyncRepository, starts/stops listeners with auth state |

### `SyncRepository.kt` — core sketch

```kotlin
class SyncRepository(private val daoProvider: DaoProvider) {
    private val db get() = FirebaseFactory.db
    private val uid get() = FirebaseFactory.uid

    private var listeners = listOf<ListenerRegistration>()

    /** Call after successful sign-in (AuthGate). */
    fun startAll(scope: CoroutineScope) {
        val userRoot = db.collection("users").document(uid)
        listeners = listOf(
            listenNotes(userRoot.collection("notes")),
            listenEvents(userRoot.collection("events")),
            listenAssignments(userRoot.collection("assignments")),
            listenTimetable(db.collection("timetable"))
        )
    }

    fun stopAll() { listeners.forEach { it.remove() }; listeners = emptyList() }

    private fun listenTimetable(ref: CollectionReference) =
        ref.addSnapshotListener { snap, _ -> snap?.documentChanges?.forEach { c ->
            val e = c.document.toTimetableEntry()       // doc.id == naturalKey
            when (c.type) {
                ADDED, MODIFIED -> daoProvider.timetable().upsertByNaturalKey(e)
                REMOVED        -> daoProvider.timetable().deleteByNaturalKey(e.naturalKey)
                else -> Unit
            }
        }}

    /** Local-first mirror — call from repository after every Room write. */
    suspend fun pushNote(note: LectureNote) {
        db.collection("users").document(uid)
            .collection("notes").document(note.naturalKey)   // natural key = doc ID
            .set(note.toDto(), SetOptions.merge())
    }
    // pushEvent / pushAssignment / deleteEvent ... follow the same pattern
}
```

DAO additions (one per collection), e.g.:

```sql
-- upserts keyed on the composite natural key, preserving row IDs and FK relations
INSERT OR REPLACE INTO lecture_notes(natural_key, content, alarm_minutes)
VALUES(:naturalKey, :content, :alarmMinutes)
```

The `INSERT OR REPLACE` on the natural key is the **exact same mechanism** the
Base44 importer used — note persistence across timetable updates is therefore
guaranteed unchanged.

---

## 10. Data Flow — How Data Is Saved & Loaded (end-to-end)

**Saving (e.g. editing a lecture note):**
1. Compose `LectureDetailSheet` → ViewModel → `TimetableRepository.saveNote()`.
2. Note is written to Room **immediately** (UI reflects it offline).
3. Repository calls `SyncRepository.pushNote()` — Firestore write queued by the
   SDK if offline, flushed automatically on reconnect.
4. `updatedAt = FieldValue.serverTimestamp()` marks the merge version.

**Loading (cold start):**
1. `AuthGate` finds a signed-in user → `MainScaffold` renders **instantly from Room**.
2. `SyncRepository.startAll()` attaches 4 snapshot listeners.
3. Remote deltas upsert into Room via natural keys; `Flow`s from DAOs re-emit →
   Compose recomposes. Listeners survive process death (SDK resumes them).

**Timetable semester update:**
1. Maintainer runs the Admin-SDK importer → writes/merges `timetable/*` docs.
2. Every client's listener receives deltas → `upsertByNaturalKey` in Room.
3. Lecture notes are untouched (different table, keyed by natural key) — a note
   survives even if its class moves rooms or timeslots change its row ID.

**Alarms:** unchanged — `AlarmScheduler` reschedules from Room; a remote change
re-emits the DAO flow → ViewModel reschedules. `BootReceiver` still re-arms
everything after reboot.

---

## 11. Testing Plan

| Test | Method |
|---|---|
| Auth | SignInScreen on emulator + `firebase emulators:auth` suite; bad SHA-1 must fail visibly |
| Offline save | Airplane mode → edit note → reboot → note present; reconnect → appears on 2nd device |
| Natural-key survival | Import updated timetable (doc merge) → note for same key retained |
| Rules | Emulator unit tests: user A denied reading `users/B/...`; unauthenticated denied everything |
| Quotas | Watch usage tab after a simulated week (5 users × daily deltas) |
| Alarms | Boot + timezone-change receiver tests unchanged (regression only) |

---

## 12. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| SHA-1 mismatch → Google Sign-In fails at runtime | Add **both** debug & release fingerprints; verify with `signingReport` |
| Listener re-attachment storms burn reads | Single `startAll` at sign-in; never per-screen listeners |
| Timetable import needs Blaze for Cloud Scheduler | Admin-SDK script run manually (§6.4); revisit on Blaze |
| Same note edited on two devices | `updatedAt` server timestamp, last-writer-wins (acceptable for personal notes) |
| User signs out on a shared phone | Room data intentionally retained; add "wipe local data" toggle in Settings if required |
| Firestore region far from Uganda | Choose `europe-west1/3` at creation time — region is immutable |

---

## 13. Implementation Order

1. Console checklist (§6.1) + `google-services.json` + Gradle (§6.2)
2. `FirebaseFactory`, `AuthGate`, `SignInScreen` (auth works end-to-end)
3. `firestore.rules` deploy + emulator rule tests (§7, §11)
4. `SyncRepository` + Mappers + DAO upserts — notes first, then events/assignments
5. Timetable listener + retire Retrofit files
6. Settings: Sign out; OutboxWorker hardening
7. Full offline/conflict test pass (§11)