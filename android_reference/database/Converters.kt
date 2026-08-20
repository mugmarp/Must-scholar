package com.must.timetable.core.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String?): String = value ?: ""

    @TypeConverter
    fun toString(value: String): String = value
}