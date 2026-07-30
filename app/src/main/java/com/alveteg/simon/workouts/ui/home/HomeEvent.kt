package com.alveteg.simon.workouts.ui.home

import com.alveteg.simon.workouts.ui.SessionWrapper
import com.alveteg.simon.workouts.utils.Event
import java.time.LocalDate
import java.time.LocalTime

sealed class HomeEvent : Event {
  data class SessionClicked(val sessionWrapper: SessionWrapper) : HomeEvent()
  data class NewSession(
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val colorArgb: Long,
    val recurrenceFrequency: RecurrenceFrequency,
    val recurrenceInterval: Int,
    val recurrenceUntil: LocalDate?
  ) : HomeEvent()
  object OpenSettings : HomeEvent()
  data class SetCalendarView(val mode: CalendarViewMode) : HomeEvent()
  data class CopySession(val sessionId: Long, val date: LocalDate) : HomeEvent()
  data class MoveSession(
    val sessionId: Long,
    val date: LocalDate,
    val scope: RecurrenceEditScope
  ) : HomeEvent()
  data class DeleteSession(val sessionId: Long, val scope: RecurrenceEditScope) : HomeEvent()
}

enum class CalendarViewMode { DAY, WEEK, MONTH }
enum class RecurrenceFrequency { NONE, DAILY, WEEKLY, MONTHLY }
enum class RecurrenceEditScope { THIS, THIS_AND_FOLLOWING, ALL }
