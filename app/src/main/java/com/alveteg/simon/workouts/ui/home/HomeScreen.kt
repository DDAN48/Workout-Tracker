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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alveteg.simon.workouts.db.entities.Session
import com.alveteg.simon.workouts.db.entities.WorkoutCalendar
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
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
  onNavigate: (UiEvent.Navigate) -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  viewModel: HomeViewModel = hiltViewModel()
) {
  val sessions by viewModel.sessions.collectAsState()
  val viewMode by viewModel.calendarViewMode.collectAsState()
  val calendars by viewModel.workoutCalendars.collectAsState()
  val visibleCalendarIds = calendars.filter { it.visible }.map { it.calendarId }.toSet()
  val visibleSessions = sessions.filter { it.session.calendarId in visibleCalendarIds }
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  var visibleMonth by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
  var monthPickerVisible by rememberSaveable { mutableStateOf(false) }
  var focusedDateValue by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
  var editorDate by remember { mutableStateOf<LocalDate?>(null) }
  var actionSession by remember { mutableStateOf<SessionWrapper?>(null) }
  var selectedCalendarId by rememberSaveable { mutableStateOf(1L) }
  var calendarToDelete by remember { mutableStateOf<WorkoutCalendar?>(null) }
  var editingCalendarId by rememberSaveable { mutableStateOf<Long?>(null) }
  var calendarNameDraft by rememberSaveable { mutableStateOf("") }
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
          HomeEvent.NewSession(title, selectedDate, start, end, color, frequency, interval, until, selectedCalendarId)
        )
        editorDate = null
      }
    )
  }

  calendarToDelete?.let { calendar ->
    AlertDialog(
      onDismissRequest = { calendarToDelete = null },
      title = { Text("Delete ${calendar.name}?") },
      text = { Text("Its workouts will be moved to another available calendar. If none remain, they stay unassigned.") },
      confirmButton = {
        Button(onClick = {
          viewModel.onEvent(HomeEvent.DeleteCalendar(calendar))
          if (selectedCalendarId == calendar.calendarId) {
            selectedCalendarId = calendars.firstOrNull { it.calendarId != calendar.calendarId }?.calendarId ?: 0L
          }
          calendarToDelete = null
        }) { Text("Delete") }
      },
      dismissButton = { TextButton(onClick = { calendarToDelete = null }) { Text("Cancel") } }
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

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet {
        Text("Workout Calendar", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(24.dp))
        CalendarViewMode.entries.forEach { mode ->
          NavigationDrawerItem(
            label = { Text(mode.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) },
            icon = { Icon(when (mode) {
              CalendarViewMode.AGENDA -> Icons.Default.ViewAgenda
              CalendarViewMode.DAY -> Icons.Default.ViewDay
              CalendarViewMode.THREE_DAYS -> Icons.Default.DateRange
              CalendarViewMode.WEEK -> Icons.Default.ViewWeek
              CalendarViewMode.MONTH -> Icons.Default.CalendarMonth
            }, contentDescription = null) },
            selected = viewMode == mode,
            onClick = { viewModel.onEvent(HomeEvent.SetCalendarView(mode)); scope.launch { drawerState.close() } }
          )
        }
        HorizontalDivider()
        Text("Workout calendars", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp))
        calendars.forEach { calendar ->
          val focusRequester = remember(calendar.calendarId) { FocusRequester() }
          LaunchedEffect(editingCalendarId, calendar.calendarId) {
            if (editingCalendarId == calendar.calendarId) focusRequester.requestFocus()
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(28.dp))
              .combinedClickable(
                onClick = {
                  selectedCalendarId = calendar.calendarId
                  editingCalendarId = calendar.calendarId
                  calendarNameDraft = calendar.name
                },
                onLongClick = { calendarToDelete = calendar }
              )
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
              Box(
                modifier = Modifier
                  .size(22.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color(calendar.colorArgb))
                  .clickable { viewModel.onEvent(HomeEvent.ToggleCalendar(calendar)) },
                contentAlignment = Alignment.Center
              ) { if (calendar.visible) Text("✓", color = Color.White) }
            if (editingCalendarId == calendar.calendarId) {
              BasicTextField(
                value = calendarNameDraft,
                onValueChange = { calendarNameDraft = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                  viewModel.onEvent(HomeEvent.RenameCalendar(calendar, calendarNameDraft))
                  editingCalendarId = null
                }),
                modifier = Modifier
                  .weight(1f)
                  .padding(start = 16.dp)
                  .focusRequester(focusRequester)
              )
              IconButton(onClick = {
                viewModel.onEvent(HomeEvent.RenameCalendar(calendar, calendarNameDraft))
                editingCalendarId = null
              }) {
                Icon(Icons.Default.Check, contentDescription = "Save calendar name")
              }
            } else {
              Text(calendar.name, modifier = Modifier.weight(1f).padding(start = 16.dp))
            }
          }
        }
        NavigationDrawerItem(label = { Text("+ Add calendar") }, selected = false, onClick = {
          viewModel.onEvent(HomeEvent.AddCalendar("Calendar ${calendars.size + 1}", sessionColors[calendars.size % sessionColors.size]))
        })
        HorizontalDivider()
        NavigationDrawerItem(
          label = { Text("Settings") },
          icon = { Icon(Icons.Default.Settings, contentDescription = null) },
          selected = false,
          onClick = { viewModel.onEvent(HomeEvent.OpenSettings) }
        )
      }
    }
  ) {
  Scaffold(
    topBar = {
      CalendarTopBar(
        month = month,
        onMenu = { scope.launch { drawerState.open() } },
        monthPickerVisible = monthPickerVisible,
        onMonthClick = { monthPickerVisible = !monthPickerVisible },
        onMonthSelected = {
          visibleMonth = it.toString()
          focusedDateValue = it.atDay(1).toString()
        },
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
            CalendarViewMode.AGENDA, CalendarViewMode.THREE_DAYS -> focusedDate.minusDays(3).let {
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
            CalendarViewMode.AGENDA, CalendarViewMode.THREE_DAYS -> focusedDate.plusDays(3).let {
              focusedDateValue = it.toString()
              visibleMonth = YearMonth.from(it).toString()
            }
          }
        },
        onToday = {
          visibleMonth = YearMonth.now().toString()
          focusedDateValue = LocalDate.now().toString()
        }
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
        sessions = visibleSessions,
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
        sessions = visibleSessions,
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
        sessions = visibleSessions,
        onDateChange = {
          focusedDateValue = it.toString()
          visibleMonth = YearMonth.from(it).toString()
        },
        onNewSession = { editorDate = it },
        onSessionClick = { viewModel.onEvent(HomeEvent.SessionClicked(it)) },
        onSessionLongClick = { actionSession = it },
        modifier = Modifier.padding(innerPadding)
      )
      CalendarViewMode.AGENDA, CalendarViewMode.THREE_DAYS -> WeekCalendar(
        focusedDate = focusedDate,
        sessions = visibleSessions,
        onDateChange = { focusedDateValue = it.toString() },
        onNewSession = { editorDate = it },
        onSessionClick = { viewModel.onEvent(HomeEvent.SessionClicked(it)) },
        onSessionLongClick = { actionSession = it },
        modifier = Modifier.padding(innerPadding)
      )
    }
  }
  }
}

@Composable
private fun CalendarTopBar(
  month: YearMonth,
  onMenu: () -> Unit,
  monthPickerVisible: Boolean,
  onMonthClick: () -> Unit,
  onMonthSelected: (YearMonth) -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onToday: () -> Unit
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
        IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
        Text(
          text = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() },
          style = MaterialTheme.typography.headlineSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .weight(1f)
            .clickable(onClick = onMonthClick)
        )
        TextButton(onClick = onToday) { Text("Today") }
        IconButton(onClick = onPrevious) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Previous month")
        }
        IconButton(onClick = onNext) {
          Icon(Icons.Default.ArrowForward, contentDescription = "Next month")
        }
      }
      if (monthPickerVisible) {
        MonthPickerRibbon(selectedMonth = month, onMonthSelected = onMonthSelected)
      }
    }
  }
}

private data class MonthRibbonItem(val label: String, val month: YearMonth? = null)

@Composable
private fun MonthPickerRibbon(
  selectedMonth: YearMonth,
  onMonthSelected: (YearMonth) -> Unit
) {
  val items = remember(selectedMonth.year) {
    buildList {
      for (year in (selectedMonth.year - 2)..(selectedMonth.year + 3)) {
        (1..12).forEach { monthNumber ->
          val value = YearMonth.of(year, monthNumber)
          add(
            MonthRibbonItem(
              label = value.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
              month = value
            )
          )
        }
        add(MonthRibbonItem(label = (year + 1).toString()))
      }
    }
  }
  val listState = rememberLazyListState(initialFirstVisibleItemIndex = 26 + selectedMonth.monthValue - 1)
  LazyRow(
    state = listState,
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
  ) {
    items(items, key = { "${it.label}-${it.month}" }) { item ->
      if (item.month == null) {
        Box(
          modifier = Modifier
            .width(58.dp)
            .height(38.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(item.label, style = MaterialTheme.typography.labelMedium)
        }
      } else {
        Surface(
          color = if (item.month == selectedMonth) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceContainer,
          shape = RoundedCornerShape(14.dp),
          onClick = { onMonthSelected(item.month) },
          modifier = Modifier.width(58.dp)
        ) {
          Text(
            text = item.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
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
