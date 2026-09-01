package com.must.timetable.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.must.timetable.features.timetable.domain.Assignment
import com.must.timetable.features.timetable.domain.CustomEvent
import com.must.timetable.features.timetable.domain.LectureNote
import com.must.timetable.features.timetable.domain.TimetableEntry

@Database(
    entities = [TimetableEntry::class, LectureNote::class, CustomEvent::class, Assignment::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timetableDao(): TimetableDao
    abstract fun customEventDao(): CustomEventDao
    abstract fun assignmentDao(): AssignmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "must_timetable.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}