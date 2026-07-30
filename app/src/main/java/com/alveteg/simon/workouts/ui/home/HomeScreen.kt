package com.alveteg.simon.workouts.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
  var visibleMonth by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
  var editorDate by remember { mutableStateOf<LocalDate?>(null) }
  val month = YearMonth.parse(visibleMonth)

  LaunchedEffect(Unit) {
    viewModel.uiEvent.collect { event ->
      if (event is UiEvent.Navigate) onNavigate(event)
    }
  }

  editorDate?.let { date ->
    NewSessionDialog(
      initialDate = date,
      onDismiss = { editorDate = null },
      onSave = { title, selectedDate, start, end, color ->
        viewModel.onEvent(HomeEvent.NewSession(title, selectedDate, start, end, color))
        editorDate = null
      }
    )
  }

  Scaffold(
    topBar = {
      CalendarTopBar(
        month = month,
        onPrevious = { visibleMonth = month.minusMonths(1).toString() },
        onNext = { visibleMonth = month.plusMonths(1).toString() },
        onToday = { visibleMonth = YearMonth.now().toString() },
        onSettings = { viewModel.onEvent(HomeEvent.OpenSettings) }
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = { editorDate = LocalDate.now() }) {
        Icon(Icons.Default.Add, contentDescription = "New workout")
      }
    }
  ) { innerPadding ->
    MonthCalendar(
      month = month,
      sessions = sessions,
      onDateClick = { editorDate = it },
      onSessionClick = { viewModel.onEvent(HomeEvent.SessionClicked(it)) },
      modifier = Modifier.padding(innerPadding)
    )
  }
}

@Composable
private fun CalendarTopBar(
  month: YearMonth,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onToday: () -> Unit,
  onSettings: () -> Unit
) {
  Surface(shadowElevation = 2.dp) {
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
  }
}

@Composable
private fun MonthCalendar(
  month: YearMonth,
  sessions: List<SessionWrapper>,
  onDateClick: (LocalDate) -> Unit,
  onSessionClick: (SessionWrapper) -> Unit,
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
          onSessionClick = onSessionClick
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
  onSessionClick: (SessionWrapper) -> Unit
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
      CalendarSessionCard(session, onSessionClick)
    }
    if (sessions.size > 3) {
      Text("+${sessions.size - 3}", style = MaterialTheme.typography.labelSmall)
    }
  }
}

@Composable
private fun CalendarSessionCard(session: SessionWrapper, onClick: (SessionWrapper) -> Unit) {
  val label = session.session.title.ifBlank {
    session.muscleGroups.firstOrNull()?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Workout"
  }
  Surface(
    color = Color(session.session.colorArgb),
    contentColor = Color.White,
    shape = RoundedCornerShape(3.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 2.dp),
    onClick = { onClick(session) }
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
private fun NewSessionDialog(
  initialDate: LocalDate,
  onDismiss: () -> Unit,
  onSave: (String, LocalDate, LocalTime, LocalTime, Long) -> Unit
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf("") }
  var date by remember { mutableStateOf(initialDate) }
  var start by remember { mutableStateOf(LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)) }
  var end by remember { mutableStateOf(start.plusHours(1)) }
  var selectedColor by remember { mutableStateOf(Session.DEFAULT_SESSION_COLOR) }
  val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM yyyy") }
  val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Schedule workout") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
      }
    },
    confirmButton = {
      Button(onClick = { onSave(title, date, start, end, selectedColor) }) {
        Text("Create")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}
