package com.must.timetable.core.database

import androidx.room.*
import com.must.timetable.features.timetable.domain.Assignment
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {

    @Query("SELECT * FROM assignments ORDER BY dueAtMillis ASC")
    fun getAll(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments")
    suspend fun getAllListOnce(): List<Assignment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(assignment: Assignment): Long

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun delete(id: Long)
}