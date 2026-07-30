package com.alveteg.simon.workouts.db.entities

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.time.LocalDateTime


/**
 * A workout-session contains multiple SessionExercises. Has a start and end-time.
 */
@Entity(tableName = "sessions")
data class Session(
  @PrimaryKey(autoGenerate = true)
  val sessionId: Long = 0L,
  @ColumnInfo(defaultValue = "''")
  val title: String = "",
  val start: LocalDateTime = LocalDateTime.now(),
  val end: LocalDateTime? = null,
  val scheduledEnd: LocalDateTime? = null,
  @ColumnInfo(defaultValue = "4284960932")
  val colorArgb: Long = DEFAULT_SESSION_COLOR,
  val recurrenceSeriesId: String? = null,
  @ColumnInfo(defaultValue = "'NONE'")
  val recurrenceFrequency: String = "NONE",
  @ColumnInfo(defaultValue = "1")
  val recurrenceInterval: Int = 1,
  val recurrenceUntil: String? = null
) {
  companion object {
    const val DEFAULT_SESSION_COLOR: Long = 0xFF6750A4
  }
}
