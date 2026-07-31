package com.alveteg.simon.workouts.db

import android.content.Context
import com.alveteg.simon.workouts.db.entities.*
import com.alveteg.simon.workouts.ui.DatabaseModel
import com.alveteg.simon.workouts.ui.ExerciseWrapper
import com.alveteg.simon.workouts.ui.SessionWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import com.alveteg.simon.workouts.ui.home.RecurrenceEditScope
import com.alveteg.simon.workouts.ui.home.RecurrenceFrequency
import timber.log.Timber
import java.io.File
import kotlin.collections.filter
import kotlin.collections.mapNotNull
import kotlin.collections.sortedByDescending


class GymRepository(
  private val dao: GymDAO,
  private val database: GymDatabase
) {
  private val DB_NAME = "gym_database.db"

  fun checkpoint() {
    // This merges the -wal file into the .db file without closing the connection
    database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use {
      it.moveToFirst()
    }
  }

  fun checkpointAndClose() {
    if (database.isOpen) {
      database.close()
    }
  }

  fun getDatabaseFile(context: Context): File {
    return context.getDatabasePath(DB_NAME)
  }

  fun getSessionById(sessionId: Long) = dao.getSessionById(sessionId)
  fun getSetById(setId: Long) = dao.getSetById(setId)

  fun getAllSessions() = dao.getAllSessions()
  fun getWorkoutCalendars() = dao.getWorkoutCalendars()
  suspend fun addWorkoutCalendar(name: String, color: Long) =
    dao.insertWorkoutCalendar(WorkoutCalendar(name = name, colorArgb = color))
  suspend fun toggleWorkoutCalendar(calendar: WorkoutCalendar) =
    dao.updateWorkoutCalendar(calendar.copy(visible = !calendar.visible))
  suspend fun renameWorkoutCalendar(calendar: WorkoutCalendar, name: String) {
    val normalizedName = name.trim()
    if (normalizedName.isNotEmpty()) dao.updateWorkoutCalendar(calendar.copy(name = normalizedName))
  }
  suspend fun deleteWorkoutCalendar(calendar: WorkoutCalendar) = database.withTransaction {
    val fallbackId = dao.getWorkoutCalendarList()
      .firstOrNull { it.calendarId != calendar.calendarId }
      ?.calendarId ?: 0L
    dao.moveSessionsToCalendar(calendar.calendarId, fallbackId)
    dao.deleteWorkoutCalendar(calendar)
  }

  fun getAllSets() = dao.getAllSets()
  fun getAllExercises() = dao.getAllExercises()
  fun getAllExercisesWithSessionCount() = dao.getAllExercisesWithSessionCount()
  fun getLastSession() = dao.getLastSession()

  fun getAllSessionExercises() = dao.getAllSessionExercises()

  suspend fun updateSessionExercises(exercises: List<SessionExercise>) = dao.updateSessionExercises(exercises)

  @OptIn(ExperimentalCoroutinesApi::class)
  fun getExercisesForSession(session: Flow<Session>): Flow<List<SessionExerciseWithExercise>> {
    return session.flatMapLatest {
      dao.getExercisesForSession(it.sessionId)
    }
  }

  suspend fun getHistoryForExercise(exercise: Exercise): List<Pair<SessionWrapper, ExerciseWrapper>> {
    return withContext(Dispatchers.IO) {
      val allSessionExercises = getAllSessionExercises().first()
      val relevantSessionExercises = allSessionExercises.filter { it.exercise.id == exercise.id }

      relevantSessionExercises
        .map { sessionExercise ->
          getSessionById(sessionExercise.sessionExercise.parentSessionId).let { session ->
            val sets =
              getSetsForExercise(sessionExercise.sessionExercise.sessionExerciseId).first()
            val sessionWrapper = SessionWrapper(session, emptyList())
            val exerciseWrapper = ExerciseWrapper(
              sessionExercise = sessionExercise.sessionExercise, exercise = exercise, sets = sets
            )

            sessionWrapper to exerciseWrapper
          }
        }
        .sortedByDescending { it.first.session.start }
    }
  }

  fun getExercisesForSession(session: Session): Flow<List<SessionExerciseWithExercise>> {
    Timber.d("Retrieving exercises for session: $session")
    return dao.getExercisesForSession(session.sessionId)
  }

  fun getSetsForExercise(sessionExerciseId: Long) = dao.getSetsForExercise(sessionExerciseId)


  suspend fun insertExercise(exercise: Exercise) = dao.insertExercise(exercise)

  suspend fun insertSession(session: Session) = dao.insertSession(session)

  suspend fun createScheduledSessions(
    title: String,
    start: LocalDateTime,
    scheduledEnd: LocalDateTime,
    colorArgb: Long,
    frequency: RecurrenceFrequency,
    interval: Int,
    until: LocalDate?,
    calendarId: Long
  ): Long = database.withTransaction {
    val safeInterval = interval.coerceAtLeast(1)
    val seriesId = if (frequency == RecurrenceFrequency.NONE) null else UUID.randomUUID().toString()
    val finalDate = if (frequency == RecurrenceFrequency.NONE) start.toLocalDate() else until
      ?: start.toLocalDate().plusYears(1)
    var occurrenceStart = start
    var occurrenceEnd = scheduledEnd
    var firstId = 0L
    do {
      val id = dao.insertSession(
        Session(
          title = title,
          start = occurrenceStart,
          scheduledEnd = occurrenceEnd,
          colorArgb = colorArgb,
          recurrenceSeriesId = seriesId,
          recurrenceFrequency = frequency.name,
          recurrenceInterval = safeInterval,
          recurrenceUntil = finalDate.toString(),
          calendarId = calendarId
        )
      )
      if (firstId == 0L) firstId = id
      when (frequency) {
        RecurrenceFrequency.NONE -> break
        RecurrenceFrequency.DAILY -> {
          occurrenceStart = occurrenceStart.plusDays(safeInterval.toLong())
          occurrenceEnd = occurrenceEnd.plusDays(safeInterval.toLong())
        }
        RecurrenceFrequency.WEEKLY -> {
          occurrenceStart = occurrenceStart.plusWeeks(safeInterval.toLong())
          occurrenceEnd = occurrenceEnd.plusWeeks(safeInterval.toLong())
        }
        RecurrenceFrequency.MONTHLY -> {
          occurrenceStart = occurrenceStart.plusMonths(safeInterval.toLong())
          occurrenceEnd = occurrenceEnd.plusMonths(safeInterval.toLong())
        }
      }
    } while (!occurrenceStart.toLocalDate().isAfter(finalDate))
    firstId
  }

  suspend fun removeSession(session: Session) = dao.removeSession(session)

  suspend fun updateSession(session: Session) = dao.updateSession(session)

  suspend fun moveSession(sessionId: Long, date: LocalDate, scope: RecurrenceEditScope) = database.withTransaction {
    val session = dao.getSessionById(sessionId)
    val days = date.toEpochDay() - session.start.toLocalDate().toEpochDay()
    val affected = sessionsForScope(session, scope)
    affected.forEach { affectedSession ->
      dao.updateSession(
        affectedSession.copy(
          start = affectedSession.start.plusDays(days),
          scheduledEnd = affectedSession.scheduledEnd?.plusDays(days),
          end = affectedSession.end?.plusDays(days),
          recurrenceSeriesId = if (scope == RecurrenceEditScope.THIS) null else affectedSession.recurrenceSeriesId,
          recurrenceFrequency = if (scope == RecurrenceEditScope.THIS) RecurrenceFrequency.NONE.name else affectedSession.recurrenceFrequency
        )
      )
    }
  }

  suspend fun deleteSession(sessionId: Long, scope: RecurrenceEditScope) = database.withTransaction {
    val session = dao.getSessionById(sessionId)
    sessionsForScope(session, scope).forEach { dao.removeSession(it) }
  }

  private fun sessionsForScope(session: Session, scope: RecurrenceEditScope): List<Session> {
    val seriesId = session.recurrenceSeriesId ?: return listOf(session)
    val series = dao.getSessionList().filter { it.recurrenceSeriesId == seriesId }
    return when (scope) {
      RecurrenceEditScope.THIS -> listOf(session)
      RecurrenceEditScope.THIS_AND_FOLLOWING -> series.filter { !it.start.isBefore(session.start) }
      RecurrenceEditScope.ALL -> series
    }
  }

  suspend fun copySession(sessionId: Long, date: LocalDate): Long = database.withTransaction {
    val source = dao.getSessionById(sessionId)
    val days = date.toEpochDay() - source.start.toLocalDate().toEpochDay()
    val copiedSessionId = dao.insertSession(
      source.copy(
        sessionId = 0L,
        start = source.start.plusDays(days),
        scheduledEnd = source.scheduledEnd?.plusDays(days),
        end = null,
        recurrenceSeriesId = null,
        recurrenceFrequency = RecurrenceFrequency.NONE.name,
        recurrenceInterval = 1,
        recurrenceUntil = null
      )
    )
    val sourceExercises = dao.getSessionExerciseList().filter { it.parentSessionId == sessionId }
    val sourceSets = dao.getSetList()
    sourceExercises.forEach { sourceExercise ->
      val copiedExerciseId = dao.insertSessionExercise(
        sourceExercise.copy(sessionExerciseId = 0L, parentSessionId = copiedSessionId)
      )
      sourceSets
        .filter { it.parentSessionExerciseId == sourceExercise.sessionExerciseId }
        .forEach { sourceSet ->
          dao.insertSet(sourceSet.copy(setId = 0L, parentSessionExerciseId = copiedExerciseId))
        }
    }
    copiedSessionId
  }

  suspend fun insertSessionExercise(sessionExercise: SessionExercise): Long {
    return database.withTransaction {
      val session = dao.getSessionById(sessionExercise.parentSessionId)
      val allExercises = dao.getSessionExerciseList()
      val exerciseOrder = allExercises
        .filter { it.parentSessionId == session.sessionId }
        .maxOfOrNull { it.exerciseOrder }
        ?.plus(1) ?: 0
      dao.insertSessionExercise(sessionExercise.copy(exerciseOrder = exerciseOrder))
    }
  }

  suspend fun removeSessionExercise(sessionExercise: SessionExercise) = dao.removeSessionExercise(sessionExercise)

  suspend fun insertSet(gymSet: GymSet) = dao.insertSet(gymSet)

  suspend fun updateSet(set: GymSet) = dao.updateSet(set)

  suspend fun deleteSet(set: GymSet) = dao.deleteSet(set)

  suspend fun createSet(sessionExercise: SessionExercise) =
    dao.insertSet(GymSet(parentSessionExerciseId = sessionExercise.sessionExerciseId))

  suspend fun syncRecurringPlan(sessionId: Long, scope: RecurrenceEditScope) = database.withTransaction {
    if (scope == RecurrenceEditScope.THIS) return@withTransaction
    var source = dao.getSessionById(sessionId)
    var seriesId = source.recurrenceSeriesId
    if (seriesId == null) {
      val frequency = runCatching { RecurrenceFrequency.valueOf(source.recurrenceFrequency) }
        .getOrDefault(RecurrenceFrequency.NONE)
      if (frequency == RecurrenceFrequency.NONE) return@withTransaction
      seriesId = UUID.randomUUID().toString()
      source = source.copy(recurrenceSeriesId = seriesId)
      dao.updateSession(source)
      val until = source.recurrenceUntil?.let(LocalDate::parse) ?: source.start.toLocalDate().plusYears(1)
      var nextStart = nextOccurrence(source.start, frequency, source.recurrenceInterval)
      val duration = source.scheduledEnd?.let { java.time.Duration.between(source.start, it) }
      while (!nextStart.toLocalDate().isAfter(until)) {
        dao.insertSession(
          source.copy(
            sessionId = 0L,
            start = nextStart,
            scheduledEnd = duration?.let { nextStart.plus(it) },
            end = null
          )
        )
        nextStart = nextOccurrence(nextStart, frequency, source.recurrenceInterval)
      }
    }
    val configuredFrequency = runCatching { RecurrenceFrequency.valueOf(source.recurrenceFrequency) }
      .getOrDefault(RecurrenceFrequency.NONE)
    val existingSeries = dao.getSessionList().filter { it.recurrenceSeriesId == seriesId }
    val pendingToReplace = existingSeries.filter {
      it.sessionId != sessionId && it.end == null && !it.start.isBefore(source.start)
    }
    pendingToReplace.forEach { dao.removeSession(it) }
    if (configuredFrequency == RecurrenceFrequency.NONE) {
      source = source.copy(recurrenceSeriesId = null, recurrenceUntil = null)
      dao.updateSession(source)
    } else {
      val until = source.recurrenceUntil?.let(LocalDate::parse) ?: source.start.toLocalDate().plusYears(1)
      val duration = source.scheduledEnd?.let { java.time.Duration.between(source.start, it) }
      val occupiedDates = dao.getSessionList()
        .filter { it.recurrenceSeriesId == seriesId }
        .map { it.start.toLocalDate() }
        .toSet()
      var nextStart = nextOccurrence(source.start, configuredFrequency, source.recurrenceInterval)
      while (!nextStart.toLocalDate().isAfter(until)) {
        if (nextStart.toLocalDate() !in occupiedDates) {
          dao.insertSession(
            source.copy(
              sessionId = 0L,
              start = nextStart,
              scheduledEnd = duration?.let { nextStart.plus(it) },
              end = null
            )
          )
        }
        nextStart = nextOccurrence(nextStart, configuredFrequency, source.recurrenceInterval)
      }
    }
    val allSessions = dao.getSessionList()
    val allExercises = dao.getSessionExerciseList()
    val allSets = dao.getSetList()
    val sourceExercises = allExercises.filter { it.parentSessionId == sessionId }
    val targets = allSessions.filter {
      it.recurrenceSeriesId == seriesId && it.sessionId != sessionId &&
        (scope == RecurrenceEditScope.ALL || !it.start.isBefore(source.start))
    }
    targets.forEach { target ->
      dao.updateSession(
        target.copy(
          title = source.title,
          colorArgb = source.colorArgb,
          calendarId = source.calendarId,
          recurrenceFrequency = source.recurrenceFrequency,
          recurrenceInterval = source.recurrenceInterval,
          recurrenceUntil = source.recurrenceUntil
        )
      )
      if (target.end != null) return@forEach
      allExercises.filter { it.parentSessionId == target.sessionId }.forEach { dao.removeSessionExercise(it) }
      sourceExercises.forEach { sourceExercise ->
        val newExerciseId = dao.insertSessionExercise(
          sourceExercise.copy(sessionExerciseId = 0L, parentSessionId = target.sessionId)
        )
        allSets.filter { it.parentSessionExerciseId == sourceExercise.sessionExerciseId }.forEach { set ->
          dao.insertSet(set.copy(setId = 0L, parentSessionExerciseId = newExerciseId))
        }
      }
    }
  }

  private fun nextOccurrence(
    dateTime: LocalDateTime,
    frequency: RecurrenceFrequency,
    interval: Int
  ): LocalDateTime = when (frequency) {
    RecurrenceFrequency.NONE -> dateTime
    RecurrenceFrequency.DAILY -> dateTime.plusDays(interval.coerceAtLeast(1).toLong())
    RecurrenceFrequency.WEEKLY -> dateTime.plusWeeks(interval.coerceAtLeast(1).toLong())
    RecurrenceFrequency.MONTHLY -> dateTime.plusMonths(interval.coerceAtLeast(1).toLong())
  }

  private fun futureOccurrences(session: Session): List<Session> {
    val seriesId = session.recurrenceSeriesId ?: return listOf(session)
    return dao.getSessionList()
      .filter { it.recurrenceSeriesId == seriesId && !it.start.isBefore(session.start) && it.end == null }
      .sortedBy { it.start }
  }

  private fun relatedSets(sourceSet: GymSet): List<GymSet> {
    val allExercises = dao.getSessionExerciseList()
    val sourceExercise = allExercises.first { it.sessionExerciseId == sourceSet.parentSessionExerciseId }
    val sourceSession = dao.getSessionById(sourceExercise.parentSessionId)
    val allSets = dao.getSetList()
    val sourceIndex = allSets
      .filter { it.parentSessionExerciseId == sourceExercise.sessionExerciseId }
      .sortedBy { it.setId }
      .indexOfFirst { it.setId == sourceSet.setId }
    return futureOccurrences(sourceSession).mapNotNull { occurrence ->
      val targetExercise = allExercises.firstOrNull {
        it.parentSessionId == occurrence.sessionId &&
          it.parentExerciseId == sourceExercise.parentExerciseId &&
          it.exerciseOrder == sourceExercise.exerciseOrder
      }
      targetExercise?.let { target ->
        allSets.filter { it.parentSessionExerciseId == target.sessionExerciseId }
          .sortedBy { it.setId }
          .getOrNull(sourceIndex)
      }
    }
  }

  fun getDatabaseModel() =
    DatabaseModel(
      sessions = dao.getSessionList(),
      exercises = dao.getExerciseList(),
      sessionExercises = dao.getSessionExerciseList(),
      sets = dao.getSetList()
    )

  suspend fun clearDatabase() {
    dao.clearSessions()
    dao.clearSessionExercises()
    dao.clearExercises()
    dao.clearSets()
  }
}
