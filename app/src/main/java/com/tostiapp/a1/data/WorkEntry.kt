package com.tostiapp.a1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_entries")
data class WorkEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startTime: String,
    val endTime: String,
    val breakTime: Int? = null,
    val task: String,
    val date: Long = System.currentTimeMillis(),
    val entryType: String,
    val travelKms: Double? = null,
    val travelHours: Double? = null,
    val company: String? = null
)