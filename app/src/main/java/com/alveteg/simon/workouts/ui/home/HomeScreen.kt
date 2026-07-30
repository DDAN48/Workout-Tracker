package com.alveteg.simon.workouts.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alveteg.simon.workouts.db.entities.Session
import com.alveteg.simon.workouts.ui.SessionWrapper
import com.alveteg.simon.workouts.utils.UiEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val sessionColors = listOf(
  0xFF6750A4,
  0xFF386A20,
  0xFF006A6A,
  0xFF8C4A60,
  0xFF8B5000,
  0xFF984061,
  0xFF3F5F90,
  0xFF755B00
)

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  onNavigate: (UiEvent.Navigate) -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  viewModel: HomeViewModel = hiltViewModel()
) {
  val sessions by viewModel.sessions.collectAsState()
  val viewMode by viewModel.calendarViewMode.collectAsState()
  var visibleMonth by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
  var focusedDateValue by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
  var editorDate by remember { mutableStateOf<LocalDate?>(null) }
  var actionSession by remember { mutableStateOf<SessionWrapper?>(null) }
  val month = YearMonth.parse(visibleMonth)
  val focusedDate = LocalDate.parse(focusedDateValue)

  LaunchedEffect(Unit) {
    viewModel.uiEvent.collect { event ->
      if (event is UiEvent.Navigate) onNavigate(event)
    }
  }

  editorDate?.let { date ->
    NewSessionDialog(
      initialDate = date,
      onDismiss = { editorDate = null },
      onSave = { title, selectedDate, start, end, color, frequency, interval, until ->
        viewModel.onEvent(
          HomeEvent.NewSession(title, selectedDate, start, end, color, frequency, interval, until)
        )
        editorDate = null
      }
    )
  }

  actionSession?.let { session ->
    SessionActionsDialog(
      session = session,
      onDismiss = { actionSession = null },
      onCopy = { date ->
        viewModel.onEvent(HomeEvent.CopySession(session.session.sessionId, date))
        actionSession = null
      },
      onMove = { date, scope ->
        viewModel.onEvent(HomeEvent.MoveSession(session.session.sessionId, date, scope))
        actionSession = null
      },
      onDelete = { scope ->
        viewModel.onEvent(HomeEvent.DeleteSession(session.session.sessionId, scope))
        actionSession = null
      }
    )
  }

  Scaffold(
    topBar = {
      CalendarTopBar(
        month = month,
        viewMode = viewMode,
        onPrevious = {
          when (viewMode) {
            CalendarViewMode.MONTH -> visibleMonth = month.minusMonths(1).toString()
            CalendarViewMode.WEEK -> focusedDate.minusWeeks(1).let {
              focusedDateValue = it.toString()
              visibleMonth = YearMonth.from(it).toString()
            }
            CalendarViewMode.DAY -> focusedDate.minusDays(1).let {
              focusedDateValue = it.toString()
              visibleMonth = YearMonth.from(it).toString()
            }
          }
        },
        onNext = {
          when (viewMode) {
            CalendarViewMode.MONTH -> visibleMonth = month.plusMonths(1).toString()
            CalendarViewMode.WEEK -> focusedDate.plusWeeks(1).let {
              focusedDateValue = it.toString()
              visibleMonth = YearMonth.from(it).toString()
            }
            CalendarViewMode.DAY -> focusedDate.plusDays(1).let {
              focusedDateValue = it.toString()
              visibleMonth = YearMonth.from(it).toString()
            }
          }
        },
        onToday = {
          visibleMonth = YearMonth.now().toString()
          focusedDateValue = LocalDate.now().toString()
        },
        onViewMode = { viewModel.onEvent(HomeEvent.SetCalendarView(it)) },
        onSettings = { viewModel.onEvent(HomeEvent.OpenSettings) }
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = { editorDate = if (viewMode == CalendarViewMode.MONTH) LocalDate.now() else focusedDate }) {
        Icon(Icons.Default.Add, contentDescription = "New workout")
      }
    }
  ) { innerPadding ->
    when (viewMode) {
      CalendarViewMode.MONTH -> MonthCalendar(
        month = month,
        sessions = sessions,
        onDateClick = {
          focusedDateValue = it.toString()
          editorDate = it
        },
        onSessionClick = { viewModel.onEvent(HomeEvent.SessionClicked(it)) },
        onSessionLongClick = { actionSession = it },
        modifier = Modifier.padding(innerPadding)
      )
      CalendarViewMode.WEEK -> WeekCalendar(
        focusedDate = focusedDate,
        sessions = sessions,
        onDateChange = {
          focusedDateValue = it.toString()
          visibleMonth = YearMonth.from(it).toString()
        },
        onNewSession = { editorDate = it },
        onSessionClick = { viewModel.onEvent(HomeEvent.SessionClicked(it)) },
        onSessionLongClick = { actionSession = it },
        modifier = Modifier.padding(innerPadding)
      )
      CalendarViewMode.DAY -> DayCalendar(
        date = focusedDate,
        sessions = sessions,
        onDateChange = {
          focusedDateValue = it.toString()
          visibleMonth = YearMonth.from(it).toString()
        },
        onNewSession = { editorDate = it },
        onSessionClick = { viewModel.onEvent(HomeEvent.SessionClicked(it)) },
        onSessionLongClick = { actionSession = it },
        modifier = Modifier.padding(innerPadding)
      )
    }
  }
}

@Composable
private fun CalendarTopBar(
  month: YearMonth,
  viewMode: CalendarViewMode,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onToday: () -> Unit,
  onViewMode: (CalendarViewMode) -> Unit,
  onSettings: () -> Unit
) {
  Surface(
    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
    shadowElevation = 2.dp
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")).replaceFirstChar { it.uppercase() },
          style = MaterialTheme.typography.headlineSmall,
          modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onToday) { Text("Today") }
        IconButton(onClick = onPrevious) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Previous month")
        }
        IconButton(onClick = onNext) {
          Icon(Icons.Default.ArrowForward, contentDescription = "Next month")
        }
        IconButton(onClick = onSettings) {
          Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        CalendarViewMode.entries.forEach { mode ->
          FilterChip(
            selected = viewMode == mode,
            onClick = { onViewMode(mode) },
            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
private fun MonthCalendar(
  month: YearMonth,
  sessions: List<SessionWrapper>,
  onDateClick: (LocalDate) -> Unit,
  onSessionClick: (SessionWrapper) -> Unit,
  onSessionLongClick: (SessionWrapper) -> Unit,
  modifier: Modifier = Modifier
) {
  val first = month.atDay(1)
  val leadingDays = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
  val gridStart = first.minusDays(leadingDays.toLong())
  val dates = List(42) { gridStart.plusDays(it.toLong()) }
  val sessionsByDate = remember(sessions) { sessions.groupBy { it.session.start.toLocalDate() } }

  Column(modifier = modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxWidth()) {
      DayOfWeek.entries.forEach { day ->
        Text(
          text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .weight(1f)
            .padding(vertical = 10.dp),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
      }
    }
    LazyVerticalGrid(
      columns = GridCells.Fixed(7),
      userScrollEnabled = false,
      modifier = Modifier.fillMaxSize()
    ) {
      items(dates, key = { it.toEpochDay() }) { date ->
        CalendarDay(
          date = date,
          inCurrentMonth = date.month == month.month,
          sessions = sessionsByDate[date].orEmpty(),
          onDateClick = { onDateClick(date) },
          onSessionClick = onSessionClick,
          onSessionLongClick = onSessionLongClick
        )
      }
    }
  }
}

@Composable
private fun CalendarDay(
  date: LocalDate,
  inCurrentMonth: Boolean,
  sessions: List<SessionWrapper>,
  onDateClick: () -> Unit,
  onSessionClick: (SessionWrapper) -> Unit,
  onSessionLongClick: (SessionWrapper) -> Unit
) {
  val isToday = date == LocalDate.now()
  Column(
    modifier = Modifier
      .aspectRatio(0.72f)
      .padding(1.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerLow)
      .clickable(onClick = onDateClick)
      .padding(3.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = date.dayOfMonth.toString(),
        style = MaterialTheme.typography.labelMedium,
        color = when {
          isToday -> MaterialTheme.colorScheme.onPrimary
          inCurrentMonth -> MaterialTheme.colorScheme.onSurface
          else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        }
      )
    }
    Spacer(Modifier.height(2.dp))
    sessions.take(3).forEach { session ->
      CalendarSessionCard(session, onSessionClick, onSessionLongClick)
    }
    if (sessions.size > 3) {
      Text("+${sessions.size - 3}", style = MaterialTheme.typography.labelSmall)
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarSessionCard(
  session: SessionWrapper,
  onClick: (SessionWrapper) -> Unit,
  onLongClick: (SessionWrapper) -> Unit
) {
  val label = session.session.title.ifBlank {
    session.muscleGroups.firstOrNull()?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Workout"
  }
  Surface(
    color = Color(session.session.colorArgb),
    contentColor = Color.White,
    shape = RoundedCornerShape(3.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 2.dp)
      .combinedClickable(
        onClick = { onClick(session) },
        onLongClick = { onLongClick(session) }
      )
  ) {
    Text(
      text = label,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      style = MaterialTheme.typography.labelSmall,
      modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
    )
  }
}

@Composable
private fun WeekCalendar(
  focusedDate: LocalDate,
  sessions: List<SessionWrapper>,
  onDateChange: (LocalDate) -> Unit,
  onNewSession: (LocalDate) -> Unit,
  onSessionClick: (SessionWrapper) -> Unit,
  onSessionLongClick: (SessionWrapper) -> Unit,
  modifier: Modifier = Modifier
) {
  val weekStart = focusedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
  val days = List(7) { weekStart.plusDays(it.toLong()) }
  val sessionsByDate = remember(sessions) { sessions.groupBy { it.session.start.toLocalDate() } }
  LazyColumn(modifier = modifier.fillMaxSize()) {
    items(days, key = { it.toEpochDay() }) { date ->
      AgendaDay(
        date = date,
        sessions = sessionsByDate[date].orEmpty(),
        selected = date == focusedDate,
        onDateClick = { onDateChange(date) },
        onNewSession = { onNewSession(date) },
        onSessionClick = onSessionClick,
        onSessionLongClick = onSessionLongClick
      )
    }
  }
}

@Composable
private fun DayCalendar(
  date: LocalDate,
  sessions: List<SessionWrapper>,
  onDateChange: (LocalDate) -> Unit,
  onNewSession: (LocalDate) -> Unit,
  onSessionClick: (SessionWrapper) -> Unit,
  onSessionLongClick: (SessionWrapper) -> Unit,
  modifier: Modifier = Modifier
) {
  val daySessions = remember(sessions, date) {
    sessions.filter { it.session.start.toLocalDate() == date }
  }
  LazyColumn(modifier = modifier.fillMaxSize()) {
    item {
      AgendaDay(
        date = date,
        sessions = daySessions,
        selected = true,
        onDateClick = { onDateChange(date) },
        onNewSession = { onNewSession(date) },
        onSessionClick = onSessionClick,
        onSessionLongClick = onSessionLongClick
      )
    }
  }
}

@Composable
private fun AgendaDay(
  date: LocalDate,
  sessions: List<SessionWrapper>,
  selected: Boolean,
  onDateClick: () -> Unit,
  onNewSession: () -> Unit,
  onSessionClick: (SessionWrapper) -> Unit,
  onSessionLongClick: (SessionWrapper) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(if (selected) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow)
      .clickable(onClick = onDateClick)
      .padding(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      IconButton(onClick = onNewSession) {
        Icon(Icons.Default.Add, contentDescription = "Add workout on this day")
      }
    }
    if (sessions.isEmpty()) {
      Text(
        text = "No workouts scheduled",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp)
      )
    } else {
      sessions.sortedBy { it.session.start }.forEach { session ->
        AgendaSessionCard(session, onSessionClick, onSessionLongClick)
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgendaSessionCard(
  session: SessionWrapper,
  onClick: (SessionWrapper) -> Unit,
  onLongClick: (SessionWrapper) -> Unit
) {
  val title = session.session.title.ifBlank { "Workout" }
  val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
  val end = session.session.scheduledEnd ?: session.session.end
  Surface(
    color = Color(session.session.colorArgb),
    contentColor = Color.White,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp)
      .combinedClickable(
        onClick = { onClick(session) },
        onLongClick = { onLongClick(session) }
      )
  ) {
    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
          session.muscleGroups.take(3).joinToString(),
          style = MaterialTheme.typography.bodySmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = buildString {
          append(session.session.start.format(timeFormatter))
          end?.let { append(" – ${it.format(timeFormatter)}") }
        },
        style = MaterialTheme.typography.labelLarge
      )
    }
  }
}

@Composable
private fun SessionActionsDialog(
  session: SessionWrapper,
  onDismiss: () -> Unit,
  onCopy: (LocalDate) -> Unit,
  onMove: (LocalDate, RecurrenceEditScope) -> Unit,
  onDelete: (RecurrenceEditScope) -> Unit
) {
  val context = LocalContext.current
  val sourceDate = session.session.start.toLocalDate()
  var action by remember { mutableStateOf<String?>(null) }
  var scope by remember { mutableStateOf(RecurrenceEditScope.THIS) }
  val isRecurring = session.session.recurrenceSeriesId != null

  fun selectDate(onDate: (LocalDate) -> Unit) {
    DatePickerDialog(
      context,
      { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) },
      sourceDate.year,
      sourceDate.monthValue - 1,
      sourceDate.dayOfMonth
    ).show()
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(session.session.title.ifBlank { "Workout" }) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Copy duplicates every exercise and set. Move keeps the workout contents and changes its date.")
        if (action != null && isRecurring) {
          Text("Apply to", style = MaterialTheme.typography.labelLarge)
          RecurrenceEditScope.entries.forEach { option ->
            FilterChip(
              selected = scope == option,
              onClick = { scope = option },
              label = {
                Text(
                  when (option) {
                    RecurrenceEditScope.THIS -> "Only this session"
                    RecurrenceEditScope.THIS_AND_FOLLOWING -> "This and following"
                    RecurrenceEditScope.ALL -> "All sessions"
                  }
                )
              }
            )
          }
        }
      }
    },
    confirmButton = {
      when (action) {
        "MOVE" -> Button(onClick = { selectDate { onMove(it, scope) } }) { Text("Select date") }
        "DELETE" -> Button(onClick = { onDelete(scope) }) { Text("Delete") }
        else -> Button(onClick = { selectDate(onCopy) }) { Text("Copy") }
      }
    },
    dismissButton = {
      Row {
        if (action == null) {
          TextButton(onClick = { action = "MOVE" }) { Text("Move") }
          TextButton(onClick = { action = "DELETE" }) { Text("Delete") }
        }
        TextButton(onClick = { if (action == null) onDismiss() else action = null }) { Text("Cancel") }
      }
    }
  )
}

@Composable
private fun NewSessionDialog(
  initialDate: LocalDate,
  onDismiss: () -> Unit,
  onSave: (String, LocalDate, LocalTime, LocalTime, Long, RecurrenceFrequency, Int, LocalDate?) -> Unit
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf("") }
  var date by remember { mutableStateOf(initialDate) }
  var start by remember { mutableStateOf(LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)) }
  var end by remember { mutableStateOf(start.plusHours(1)) }
  var selectedColor by remember { mutableStateOf(Session.DEFAULT_SESSION_COLOR) }
  var frequency by remember { mutableStateOf(RecurrenceFrequency.NONE) }
  var intervalText by remember { mutableStateOf("1") }
  var recurrenceUntil by remember { mutableStateOf(initialDate.plusMonths(3)) }
  val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM yyyy") }
  val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Schedule workout") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Workout name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
          onClick = {
            DatePickerDialog(
              context,
              { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
              date.year,
              date.monthValue - 1,
              date.dayOfMonth
            ).show()
          },
          modifier = Modifier.fillMaxWidth()
        ) { Text(date.format(dateFormatter)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              TimePickerDialog(context, { _, hour, minute -> start = LocalTime.of(hour, minute) }, start.hour, start.minute, true).show()
            },
            modifier = Modifier.weight(1f)
          ) { Text("Start ${start.format(timeFormatter)}") }
          OutlinedButton(
            onClick = {
              TimePickerDialog(context, { _, hour, minute -> end = LocalTime.of(hour, minute) }, end.hour, end.minute, true).show()
            },
            modifier = Modifier.weight(1f)
          ) { Text("End ${end.format(timeFormatter)}") }
        }
        Text("Color", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          sessionColors.forEach { color ->
            Box(
              modifier = Modifier
                .size(if (selectedColor == color) 32.dp else 26.dp)
                .clip(CircleShape)
                .background(Color(color))
                .clickable { selectedColor = color }
            )
          }
        }
        Text("Repeat", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          RecurrenceFrequency.entries.forEach { option ->
            FilterChip(
              selected = frequency == option,
              onClick = { frequency = option },
              label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
              modifier = Modifier.weight(1f)
            )
          }
        }
        if (frequency != RecurrenceFrequency.NONE) {
          OutlinedTextField(
            value = intervalText,
            onValueChange = { intervalText = it.filter(Char::isDigit).take(3) },
            label = { Text("Repeat every") },
            supportingText = { Text(frequency.name.lowercase()) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedButton(
            onClick = {
              DatePickerDialog(
                context,
                { _, year, month, day -> recurrenceUntil = LocalDate.of(year, month + 1, day) },
                recurrenceUntil.year,
                recurrenceUntil.monthValue - 1,
                recurrenceUntil.dayOfMonth
              ).show()
            },
            modifier = Modifier.fillMaxWidth()
          ) { Text("Repeat until ${recurrenceUntil.format(dateFormatter)}") }
        }
      }
    },
    confirmButton = {
      Button(onClick = {
        onSave(
          title,
          date,
          start,
          end,
          selectedColor,
          frequency,
          intervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
          recurrenceUntil.takeIf { frequency != RecurrenceFrequency.NONE }
        )
      }) {
        Text("Create")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}
