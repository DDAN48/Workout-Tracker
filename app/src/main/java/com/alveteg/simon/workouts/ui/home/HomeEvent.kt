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
    val colorArgb: Long
  ) : HomeEvent()
  object OpenSettings : HomeEvent()
}
