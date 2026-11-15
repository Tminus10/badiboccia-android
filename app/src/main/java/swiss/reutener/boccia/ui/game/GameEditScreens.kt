package swiss.reutener.boccia.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.app.LocalNavigator
import swiss.reutener.boccia.model.Game
import swiss.reutener.boccia.model.Phase
import swiss.reutener.boccia.model.Score
import swiss.reutener.boccia.model.SwissTime
import swiss.reutener.boccia.model.Team
import swiss.reutener.boccia.model.allowedScores
import swiss.reutener.boccia.model.game
import swiss.reutener.boccia.model.hasScore
import swiss.reutener.boccia.model.shownSetsA
import swiss.reutener.boccia.model.shownSetsB
import swiss.reutener.boccia.model.teamsById
import swiss.reutener.boccia.net.ApiException
import swiss.reutener.boccia.ui.components.DetailScaffold
import swiss.reutener.boccia.ui.components.InfoCard
import swiss.reutener.boccia.ui.components.TeamAvatar
import swiss.reutener.boccia.ui.format
import swiss.reutener.boccia.ui.message
import swiss.reutener.boccia.ui.res
import swiss.reutener.boccia.ui.theme.Theme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** The game being edited plus its two teams, looked up in the loaded season data. */
private class EditedGame(val game: Game, val teamA: Team?, val teamB: Team?)

@Composable
private fun rememberEditedGame(gameId: Int): EditedGame? {
    val model = LocalAppModel.current
    val detail = model.details.values.firstOrNull { it.game(gameId) != null } ?: return null
    val game = detail.game(gameId) ?: return null
    val teams = detail.teamsById
    return EditedGame(game, game.teamAId?.let { teams[it] }, game.teamBId?.let { teams[it] })
}

/**
 * Entering (or correcting) a result: pick the set score, optionally when it was played. Mirrors
 * the website's result form: 2:0, 2:1, 1:2, 0:2, and 1:1 for an interrupted group game.
 */
@Composable
fun ResultEntryScreen(gameId: Int) {
    val edited = rememberEditedGame(gameId) ?: return
    val game = edited.game
    val model = LocalAppModel.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val start = game.startDate()

    var score by remember {
        val a = game.shownSetsA
        val b = game.shownSetsB
        mutableStateOf(if (a != null && b != null) Score(a, b) else null)
    }
    // Off for a game without a date, so saving a result never invents one; switching it on suggests today.
    var hasDate by rememberSaveable { mutableStateOf(game.playedDate != null) }
    var date by rememberSaveable { mutableStateOf(start?.toLocalDate() ?: LocalDate.now(SwissTime.zone)) }
    var hasTime by rememberSaveable { mutableStateOf(game.gameTime != null) }
    var time by rememberSaveable { mutableStateOf(start?.takeIf { game.gameTime != null }?.toLocalTime() ?: DEFAULT_TIME) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ApiException?>(null) }

    fun save() {
        val chosen = score ?: return
        isSaving = true
        scope.launch {
            try {
                model.submitResult(game, chosen, if (hasDate) date else null, if (hasDate && hasTime) time else null)
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                navigator.back()
            } catch (e: ApiException) {
                error = e
            } finally {
                isSaving = false
            }
        }
    }

    DetailScaffold(
        stringResource(if (game.hasScore) R.string.correct_result else R.string.enter_result),
        actions = { SaveAction(isSaving, enabled = score != null, ::save) },
    ) {
        Section(stringResource(R.string.result_sets_header, edited.teamA?.name ?: "–", edited.teamB?.name ?: "–")) {
            game.allowedScores.forEachIndexed { index, option ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = 16.dp), color = Theme.separator)
                ScoreOptionRow(option, edited.teamA, edited.teamB, option == score) { score = option }
            }
        }
        if (game.phase == Phase.GROUP) Note(stringResource(R.string.draw_only_interrupted))

        Section(stringResource(R.string.played_on)) {
            ToggleRow(stringResource(R.string.date), hasDate) { hasDate = it }
            if (hasDate) {
                HorizontalDivider(Modifier.padding(start = 16.dp), color = Theme.separator)
                DateRow(date) { date = it }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = Theme.separator)
                ToggleRow(stringResource(R.string.time_of_day), hasTime) { hasTime = it }
                if (hasTime) {
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = Theme.separator)
                    QuarterHourTimeRow(time) { time = it }
                }
            }
        }
    }
    error?.let { ErrorDialog(it) { error = null } }
}

/** "2 : 1  [avatar] Sandra & Thomi gewinnt" -- the score with whoever it makes the winner. */
@Composable
private fun ScoreOptionRow(score: Score, teamA: Team?, teamB: Team?, isSelected: Boolean, select: () -> Unit) {
    val winner = if (score.a > score.b) teamA else if (score.b > score.a) teamB else null
    val description = stringResource(R.string.score_description, teamA?.name.orEmpty(), score.a, score.b, teamB?.name.orEmpty())
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = select)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.RadioButton
                selected = isSelected
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("${score.a} : ${score.b}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (score.isDraw) {
            Icon(Icons.Outlined.Cloud, null, Modifier.size(26.dp), tint = Theme.secondaryText)
        } else {
            TeamAvatar(winner, 26.dp)
        }
        Text(
            if (score.isDraw) stringResource(R.string.interrupted_one_point_each) else stringResource(R.string.team_wins, winner?.name ?: "–"),
            Modifier.weight(1f),
            color = if (score.isDraw) Theme.secondaryText else Color.White,
            maxLines = 2,
        )
        Icon(
            if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            null,
            tint = if (isSelected) Theme.qualifier else Theme.tertiaryText,
        )
    }
}

/**
 * Fixing the date (and optionally the time) of a game. A clash with another planned game is a
 * warning, not an error -- like on the website, the team can save anyway.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleGameScreen(gameId: Int) {
    val edited = rememberEditedGame(gameId) ?: return
    val game = edited.game
    val model = LocalAppModel.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val res = res()
    val start = game.startDate()

    val dateState = rememberDatePickerState(initialSelectedDateMillis = (start?.toLocalDate() ?: LocalDate.now(SwissTime.zone)).toEpochMillis())
    var hasTime by rememberSaveable { mutableStateOf(game.playedDate == null || game.gameTime != null) }
    var time by rememberSaveable { mutableStateOf(start?.takeIf { game.gameTime != null }?.toLocalTime() ?: DEFAULT_TIME) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ApiException?>(null) }
    var conflicts by remember { mutableStateOf<List<Game>>(emptyList()) }

    fun save(force: Boolean) {
        val date = dateState.selectedDateMillis?.toLocalDate() ?: return
        isSaving = true
        scope.launch {
            try {
                model.schedule(game, date, if (hasTime) time else null, force)
                navigator.back()
            } catch (e: ApiException) {
                if (e.code == "schedule_conflict") conflicts = e.conflicts else error = e
            } finally {
                isSaving = false
            }
        }
    }

    DetailScaffold(
        stringResource(if (game.playedDate == null) R.string.set_date else R.string.change_date),
        actions = { SaveAction(isSaving, enabled = dateState.selectedDateMillis != null) { save(force = false) } },
    ) {
        InfoCard {
            DatePicker(
                dateState,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Transparent,
                    selectedDayContainerColor = Theme.qualifier,
                    selectedDayContentColor = Color.Black,
                    todayDateBorderColor = Theme.qualifier,
                    todayContentColor = Theme.qualifier,
                ),
            )
            HorizontalDivider(Modifier.padding(start = 16.dp), color = Theme.separator)
            ToggleRow(stringResource(R.string.time_of_day), hasTime) { hasTime = it }
            if (hasTime) {
                HorizontalDivider(Modifier.padding(start = 16.dp), color = Theme.separator)
                QuarterHourTimeRow(time) { time = it }
            }
        }
        Note(stringResource(R.string.no_time_note))
    }

    error?.let { ErrorDialog(it) { error = null } }
    if (conflicts.isNotEmpty()) {
        val teams = model.details[game.seasonId]?.teamsById.orEmpty()
        val lines = conflicts.joinToString("\n") { other ->
            val a = other.teamAId?.let { teams[it]?.name } ?: "–"
            val b = other.teamBId?.let { teams[it]?.name } ?: "–"
            "• ${other.gameTime?.take(5) ?: res.getString(R.string.all_day_lower)}: $a – $b"
        }
        AlertDialog(
            onDismissRequest = { conflicts = emptyList() },
            title = { Text(stringResource(R.string.schedule_conflict_title)) },
            text = { Text(stringResource(R.string.schedule_conflict_message) + "\n" + lines) },
            confirmButton = {
                TextButton(onClick = {
                    conflicts = emptyList()
                    save(force = true)
                }) { Text(stringResource(R.string.save_anyway)) }
            },
            dismissButton = {
                TextButton(onClick = { conflicts = emptyList() }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

// MARK: Shared pieces

/** Without a time yet, the time picker suggests the usual evening slot. */
private val DEFAULT_TIME: LocalTime = LocalTime.of(18, 0)

private fun Game.startDate() = playedDate?.let { SwissTime.dateTime(it, gameTime) }

/** Material's date picker works in UTC midnight milliseconds. */
private fun LocalDate.toEpochMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toLocalDate(): LocalDate = java.time.Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun SaveAction(isSaving: Boolean, enabled: Boolean, save: () -> Unit) {
    if (isSaving) {
        CircularProgressIndicator(Modifier.padding(horizontal = 16.dp).size(24.dp), color = Color.White, strokeWidth = 2.dp)
    } else {
        IconButton(onClick = save, enabled = enabled) {
            Icon(Icons.Filled.Check, stringResource(R.string.save))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.padding(horizontal = 16.dp), color = Theme.secondaryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        InfoCard { content() }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, Modifier.padding(horizontal = 16.dp), color = Theme.secondaryText, fontSize = 13.sp)
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { change(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), color = Color.White, fontSize = 16.sp)
        Switch(
            checked,
            onCheckedChange = change,
            colors = SwitchDefaults.colors(checkedTrackColor = Theme.qualifier, checkedThumbColor = Color.Black),
        )
    }
}

/** The date as a row; tapping it opens the calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRow(date: LocalDate, change: (LocalDate) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val res = res()
    ValueRow(stringResource(R.string.date), SwissTime.dateTime(date.toString())!!.format(res, "EEEMMMdy")) { picking = true }
    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochMillis())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { change(it.toLocalDate()) }
                    picking = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(state, showModeToggle = false)
        }
    }
}

/** Hours and quarter hours only, like on the website and in the iOS app: games start at :00, :15, :30 or :45. */
@Composable
private fun QuarterHourTimeRow(time: LocalTime, change: (LocalTime) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.time_of_day), Modifier.weight(1f), color = Color.White, fontSize = 16.sp)
        Choice("%02d".format(time.hour), (0..23).map { "%02d".format(it) }) { change(time.withHour(it.toInt())) }
        Text(":", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Choice("%02d".format(time.minute - time.minute % 15), listOf("00", "15", "30", "45")) { change(time.withMinute(it.toInt())) }
    }
}

@Composable
private fun Choice(value: String, options: List<String>, choose: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Opens at the chosen value (a few rows above it showing), not at midnight.
    val scroll = rememberScrollState()
    val rowHeight = with(LocalDensity.current) { 48.dp.toPx() }
    LaunchedEffect(expanded) {
        if (expanded) scroll.scrollTo(((options.indexOf(value) - 3).coerceAtLeast(0) * rowHeight).toInt())
    }
    Box {
        Text(
            value,
            Modifier.width(52.dp).clickable { expanded = true }.padding(vertical = 10.dp),
            color = Theme.qualifier,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        DropdownMenu(expanded, onDismissRequest = { expanded = false }, scrollState = scroll) {
            for (option in options) {
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    expanded = false
                    choose(option)
                })
            }
        }
    }
}

@Composable
private fun ValueRow(title: String, value: String, open: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = open).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Color.White, fontSize = 16.sp)
        Text(value, color = Theme.qualifier, fontSize = 16.sp)
    }
}

@Composable
private fun ErrorDialog(error: ApiException, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        text = { Text(error.message(res())) },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.ok)) } },
    )
}
