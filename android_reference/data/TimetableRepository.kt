package com.must.timetable.features.timetable.data

import com.must.timetable.core.database.TimetableDao
import com.must.timetable.core.network.ApiService
import com.must.timetable.core.network.SafeApiCall
import com.must.timetable.features.timetable.domain.LectureNote
import com.must.timetable.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

class TimetableRepository(
    private val api: ApiService,
    private val dao: TimetableDao,
    private val etagStore: ETagStore
) {

    fun getLocalSchedule(programme: String): Flow<List<TimetableEntry>> =
        dao.getScheduleIncludingShared(programme)

    fun getNote(naturalKey: String): Flow<LectureNote?> =
        dao.getNoteForKey(naturalKey)

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

    suspend fun saveNote(naturalKey: String, content: String) {
        dao.upsertNote(
            LectureNote(
                naturalKey = naturalKey,
                content = content,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteNote(naturalKey: String) = dao.deleteNote(naturalKey)
}

class ETagStore(private val prefs: android.content.SharedPreferences) {
    fun getEtag(programme: String) = prefs.getString("etag_$programme", null)
    fun saveEtag(programme: String, etag: String) {
        prefs.edit().putString("etag_$programme", etag).apply()
    }
}

sealed class SyncResult {
    data class Success(val draftVersion: String) : SyncResult()
    object NotModified : SyncResult()
    object Offline : SyncResult()
    data class Error(val message: String) : SyncResult()
}