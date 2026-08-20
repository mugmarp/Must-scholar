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

## Package structure
```
com.must.timetable/
  core/
    alarm/      AlarmScheduler, BootReceiver, ClassAlarmReceiver
    database/   AppDatabase, TimetableDao, Converters
    network/    ApiService, SafeApiCall
  features/timetable/
    data/       TimetableRepository, ETagStore
    domain/     TimetableEntry, LectureNote
    ui/         TimetableScreen, TimetableViewModel, NextUpCard,
                OverlapTimeline, LectureDetailSheet
```

## Setup
1. Copy .kt files into the matching package paths.
2. Add the receiver entries from AndroidManifest_snippet.xml to your manifest.
3. Add dependencies from build_gradle_dependencies.kts to build.gradle.kts.
4. Enable KSP plugin in your build.gradle.kts:
   `plugins { id("com.google.devtools.ksp") }