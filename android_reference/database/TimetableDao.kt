package com.must.timetable.core.database

import androidx.room.*
import com.must.timetable.features.timetable.domain.TimetableEntry
import com.must.timetable.features.timetable.domain.LectureNote
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Query("SELECT * FROM timetable_entries WHERE programmeGroup = :programme ORDER BY startTime ASC")
    fun getScheduleForProgramme(programme: String): Flow<List<TimetableEntry>>

    /**
     * Shared group matching via LIKE wildcards.
     * "MBR I" will also find entries where sharedWithRaw contains "MBR I".
     */
    @Query("""
        SELECT * FROM timetable_entries
        WHERE programmeGroup LIKE '%' || :programme || '%'
           OR sharedWithRaw LIKE '%' || :programme || '%'
        ORDER BY startTime ASC
    """)
    fun getScheduleIncludingShared(programme: String): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :day ORDER BY startTime ASC")
    fun getScheduleForDay(day: String): Flow<List<TimetableEntry>>

    @Query("SELECT DISTINCT programmeGroup FROM timetable_entries")
    fun getAllProgrammeGroups(): Flow<List<String>>

    @Query("SELECT DISTINCT draftVersion FROM timetable_entries ORDER BY lastSyncedAt DESC LIMIT 1")
    fun getLatestDraftVersion(): Flow<String?>

    @Upsert
    suspend fun upsertEntries(entries: List<TimetableEntry>)

    @Query("DELETE FROM timetable_entries WHERE draftVersion NOT IN (:keepVersions)")
    suspend fun deleteOldDrafts(keepVersions: List<String>)

    @Query("SELECT * FROM lecture_notes WHERE naturalKey = :key")
    fun getNoteForKey(key: String): Flow<LectureNote?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: LectureNote)

    @Query("DELETE FROM lecture_notes WHERE naturalKey = :key")
    suspend fun deleteNote(key: String)
}