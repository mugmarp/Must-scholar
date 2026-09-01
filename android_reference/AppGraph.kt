package com.must.timetable

import android.content.Context
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.must.timetable.core.database.AppDatabase
import com.must.timetable.core.network.ApiClient
import com.must.timetable.features.calendar.ui.CalendarViewModel
import com.must.timetable.features.notes.ui.NotesViewModel
import com.must.timetable.features.tasks.ui.TasksViewModel
import com.must.timetable.features.timetable.data.ETagStore
import com.must.timetable.features.timetable.data.TimetableRepository
import com.must.timetable.features.timetable.ui.TimetableViewModel

object AppGraph {

    fun repository(context: Context): TimetableRepository {
        val db = AppDatabase.get(context)
        val prefs = context.getSharedPreferences("must_prefs", Context.MODE_PRIVATE)
        return TimetableRepository(
            ApiClient.api,
            db.timetableDao(),
            db.customEventDao(),
            db.assignmentDao(),
            ETagStore(prefs)
        )
    }

    fun factory(context: Context) = viewModelFactory {
        initializer { TimetableViewModel(repository(context)) }
        initializer { TasksViewModel(repository(context)) }
        initializer { CalendarViewModel(repository(context)) }
        initializer { NotesViewModel(repository(context)) }
    }
}