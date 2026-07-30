package com.alveteg.simon.workouts.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workoutCalendars")
data class WorkoutCalendar(
  @PrimaryKey(autoGenerate = true) val calendarId: Long = 0L,
  val name: String,
  val colorArgb: Long,
  val visible: Boolean = true
)
