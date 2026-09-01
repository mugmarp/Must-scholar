package com.must.timetable.core.database

import androidx.room.*
import com.must.timetable.features.timetable.domain.CustomEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomEventDao {

    @Query("SELECT * FROM custom_events ORDER BY startTime ASC")
    fun getAll(): Flow<List<CustomEvent>>

    @Query("SELECT * FROM custom_events WHERE dayOfWeek = :day ORDER BY startTime ASC")
    fun getByDay(day: String): Flow<List<CustomEvent>>

    @Query("SELECT * FROM custom_events")
    suspend fun getAllListOnce(): List<CustomEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: CustomEvent): Long

    @Query("DELETE FROM custom_events WHERE id = :id")
    suspend fun delete(id: Long)
}