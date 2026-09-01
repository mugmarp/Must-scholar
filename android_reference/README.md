# MUST Timetable - Android Reference Code

Production-grade Kotlin / Jetpack Compose architecture for the MUST timetable app.
These files are reference code to drop into an Android Studio project.
They are NOT part of the Base44 React web app.

## Architecture
- **Persistence:** Room (SQLite) with composite Business Natural Key
  (programme_group + course_code + day_of_week + start_time) so user notes
  survive timetable draft updates without being orphaned.
- **Background:** AlarmManager (exact alarms, Doze-resilient) + BootReceiver
  re-arms alarms after reboot / timezone change.
- **Networking:** Retrofit with ETag / If-None-Match for offline-first sync.
- **UI:** Jetpack Compose + MVVM (StateFlow) with overlap-aware timeline.

## Features (mirrors the Base44 web app)
- Offline timetable with ETag sync
- Per-class notes + reminders (minutes-before), persisted by Business Natural Key
- Custom events (with optional weekly repeat + reminder) shown in the timeline
- Assignments / deadlines with adjustable reminder time, priority, completion
- Unified AlarmManager reminders (classes, events, assignments) re-armed on boot
- Month calendar overview (classes / events / deadlines dots)
- Notes, Tasks (search + hide-done), Settings screens
- First-launch onboarding + bottom navigation
- Schedule conflict detection when saving a custom event
- Long-press a day to quick-add an event; horizontal swipe between days
- Today is highlighted in the day selector

## Package structure
```
com.must.timetable/
  core/
    alarm/      AlarmScheduler, ClassAlarmReceiver, BootReceiver, ReminderScheduler
    database/   AppDatabase, TimetableDao, CustomEventDao, AssignmentDao, Converters
    network/    ApiService, SafeApiCall, ApiClient
    util/       TimeUtil, NotificationPermission
  features/
    timetable/
      data/     TimetableRepository, ETagStore
      domain/   TimetableEntry, LectureNote, CustomEvent, Assignment
      ui/       TimetableScreen, TimetableViewModel, NextUpCard,
                OverlapTimeline, LectureDetailSheet, EventSheet, EventCard
    tasks/ui/      TasksScreen, TasksViewModel, AssignmentSheet
    calendar/ui/   CalendarScreen, CalendarViewModel
    notes/ui/      NotesScreen, NotesViewModel
    settings/ui/   SettingsScreen
    onboarding/ui/ WelcomeScreen
  ui/             MainScaffold (NavHost + NavigationBar)
  AppGraph        (manual DI: repository + ViewModel factory)
```

## Setup
1. Copy .kt files into the matching package paths.
2. Add the receiver entries from AndroidManifest_snippet.xml to your manifest.
3. Add dependencies from build_gradle_dependencies.kts to build.gradle.kts.
4. Enable KSP plugin in your build.gradle.kts:
   `plugins { id("com.google.devtools.ksp") }