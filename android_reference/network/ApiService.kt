package com.must.timetable.core.network

import com.must.timetable.features.timetable.domain.TimetableEntry
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiService {

    /**
     * ETag-based sync: If-None-Match sends the last-known ETag.
     * Server returns 304 (no body) if unchanged, or 200 + new data + new ETag.
     */
    @GET("api/v1/schedule")
    suspend fun fetchSchedule(
        @Query("programme") programmeCode: String,
        @Header("If-None-Match") etag: String?
    ): Response<ScheduleResponse>
}

data class ScheduleResponse(
    val metadata: ScheduleMetadata,
    val lessons: List<TimetableEntry>,
    val etag: String? = null
)

data class ScheduleMetadata(
    val institution: String,
    val academicYear: String,
    val semester: String,
    val draftVersion: String,
    val generatedOn: String
)