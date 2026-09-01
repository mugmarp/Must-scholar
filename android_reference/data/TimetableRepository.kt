package com.must.timetable.features.timetable.data

import com.must.timetable.core.database.AssignmentDao
import com.must.timetable.core.database.CustomEventDao
import com.must.timetable.core.database.TimetableDao
import com.must.timetable.core.network.ApiService
import com.must.timetable.core.network.SafeApiCall
import com.must.timetable.core.util.TimeUtil
import com.must.timetable.features.timetable.domain.Assignment
import com.must.timetable.features.timetable.domain.CustomEvent
import com.must.timetable.features.timetable.domain.LectureNote
import com.must.timetable.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

class TimetableRepository(
    private val api: ApiService,
    private val dao: TimetableDao,
    private val eventDao: CustomEventDao,
    private val assignmentDao: AssignmentDao,
    private val etagStore: ETagStore
) {

    fun getLocalSchedule(programme: String): Flow<List<TimetableEntry>> =
        dao.getScheduleIncludingShared(programme)

    fun getAllProgrammeGroups(): Flow<List<String>> = dao.getAllProgrammeGroups()

    fun getNote(naturalKey: String): Flow<LectureNote?> = dao.getNoteForKey(naturalKey)

    fun getCustomEvents(): Flow<List<CustomEvent>> = eventDao.getAll()

    fun getAssignments(): Flow<List<Assignment>> = assignmentDao.getAll()

    fun getAllNotes(): Flow<List<LectureNote>> = dao.getAllNotes()

    fun getProgrammePref(): String = etagStore.getProgramme()
    fun setProgrammePref(programme: String) = etagStore.saveProgramme(programme)

    var welcomed: Boolean
        get() = etagStore.welcomed
        set(value) { etagStore.welcomed = value }

    suspend fun syncRemoteSchedule(programme: String): SyncResult {
        val cachedEtag = etagStore.getEtag(programme)
        val response = SafeApiCall.execute { api.fetchSchedule(programme, cachedEtag) }

        return when {
            response == null -> SyncResult.Offline
            response.code() == 304 -> SyncResult.NotModified
            response.isSuccessful -> {
                val body = response.body()!!
                val newEtag = response.headers()["ETag"]
                val entries = body.lessons.map {
                    it.copy(
                        draftVersion = body.metadata.draftVersion,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                }
                dao.upsertEntries(entries)
                dao.deleteOldDrafts(listOf(body.metadata.draftVersion))
                newEtag?.let { etagStore.saveEtag(programme, it) }
                SyncResult.Success(body.metadata.draftVersion)
            }
            else -> SyncResult.Error("HTTP ${response.code()}")
        }
    }

    suspend fun saveNote(naturalKey: String, content: String, alarmMinutes: Int? = null) {
        dao.upsertNote(
            LectureNote(
                naturalKey = naturalKey,
                content = content,
                updatedAt = System.currentTimeMillis(),
                alarmMinutes = alarmMinutes
            )
        )
    }

    suspend fun deleteNote(naturalKey: String) = dao.deleteNote(naturalKey)

    suspend fun saveEvent(event: CustomEvent): Long = eventDao.upsert(event)
    suspend fun deleteEvent(id: Long) = eventDao.delete(id)

    suspend fun saveAssignment(assignment: Assignment): Long = assignmentDao.upsert(assignment)
    suspend fun deleteAssignment(id: Long) = assignmentDao.delete(id)

    /** Returns timetable classes that overlap the proposed event window. */
    suspend fun checkEventConflict(
        programme: String, day: String, start: String, end: String?
    ): List<TimetableEntry> {
        val entries = dao.getEntriesForProgrammeOnce(programme).filter { it.dayOfWeek == day }
        val evS = TimeUtil.toMinutes(start)
        val evE = end?.let { TimeUtil.toMinutes(it) } ?: (evS + 60)
        return entries.filter {
            val cs = TimeUtil.toMinutes(it.startTime)
            val ce = if (it.endTime.isBlank()) cs + 60 else TimeUtil.toMinutes(it.endTime)
            TimeUtil.rangesOverlap(evS, evE, cs, ce)
        }
    }
}

class ETagStore(private val prefs: android.content.SharedPreferences) {
    fun getEtag(programme: String) = prefs.getString("etag_$programme", null)
    fun saveEtag(programme: String, etag: String) {
        prefs.edit().putString("etag_$programme", etag).apply()
    }
    fun getProgramme() = prefs.getString("programme", "MBR I") ?: "MBR I"
    fun saveProgramme(programme: String) = prefs.edit().putString("programme", programme).apply()
    var welcomed: Boolean
        get() = prefs.getBoolean("welcomed", false)
        set(value) = prefs.edit().putBoolean("welcomed", value).apply()
}

sealed class SyncResult {
    data class Success(val draftVersion: String) : SyncResult()
    object NotModified : SyncResult()
    object Offline : SyncResult()
    data class Error(val message: String) : SyncResult()
}