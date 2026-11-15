package swiss.reutener.boccia.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.app.LocalNavigator
import swiss.reutener.boccia.model.CalendarEvent
import swiss.reutener.boccia.model.Game
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.SwissTime
import swiss.reutener.boccia.model.Team
import swiss.reutener.boccia.model.allGames
import swiss.reutener.boccia.model.hasBothTeams
import swiss.reutener.boccia.model.hasScore
import swiss.reutener.boccia.model.involves
import swiss.reutener.boccia.model.isRunning
import swiss.reutener.boccia.model.shownSetsA
import swiss.reutener.boccia.model.shownSetsB
import swiss.reutener.boccia.model.teamsById
import swiss.reutener.boccia.ui.components.ArchivedSeasonNote
import swiss.reutener.boccia.ui.components.SeasonLoadingState
import swiss.reutener.boccia.ui.components.SeasonPicker
import swiss.reutener.boccia.ui.components.TeamLabel
import swiss.reutener.boccia.ui.longDate
import swiss.reutener.boccia.ui.res
import swiss.reutener.boccia.ui.theme.EmptyStateText
import swiss.reutener.boccia.ui.theme.Theme
import swiss.reutener.boccia.ui.title

/**
 * Every game of the season by day (plus the website's calendar entries such as the group draw),
 * latest first: the games that have no date yet, upcoming days, today, then past days, and within
 * a day the latest game on top.
 * It opens at today (or the next game day).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(contentPadding: PaddingValues) {
    val model = LocalAppModel.current
    var onlyMine by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            SeasonPicker()
            // Logged-in teams can narrow the schedule down to their own games.
            if (model.session != null) {
                IconToggleButton(onlyMine, onCheckedChange = { onlyMine = it }, Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                    Icon(
                        if (onlyMine) Icons.Filled.Person else Icons.Outlined.Person,
                        stringResource(R.string.only_my_games),
                        tint = if (onlyMine) Theme.qualifier else Color.White,
                    )
                }
            }
        }
        val detail = model.selectedDetail
        PullToRefreshBox(
            isRefreshing = model.isLoading && detail != null,
            onRefresh = { model.refresh() },
            modifier = Modifier.weight(1f),
        ) {
            if (detail == null) {
                SeasonLoadingState()
            } else {
                ScheduleList(detail, contentPadding, onlyMine = onlyMine && model.session != null)
            }
        }
    }
}

/** A row of a day: a game or a calendar entry. */
private sealed interface Entry {
    val key: String
    val time: String?

    data class GameEntry(val game: Game) : Entry {
        override val key get() = "game-${game.id}"
        override val time get() = game.gameTime
    }

    data class EventEntry(val event: CalendarEvent) : Entry {
        override val key get() = "event-${event.id}"
        override val time get() = event.time
    }
}

/** Latest first, like the days; without a time (all day) at the end of the day. */
private val latestFirst: Comparator<Entry> = compareByDescending<Entry> { it.time.orEmpty() }
    .thenBy { if (it is Entry.GameEntry) 0 else 1 }
    .thenByDescending { (it as? Entry.GameEntry)?.game?.id ?: (it as Entry.EventEntry).event.id }

private class Day(val day: String, val entries: List<Entry>)

private fun scheduleDays(detail: SeasonDetail, games: List<Game>, onlyMine: Boolean): List<Day> {
    val events = if (onlyMine) emptyList() else detail.events
    val entries = games.filter { it.playedDate != null }.map { Entry.GameEntry(it) } + events.map { Entry.EventEntry(it) }
    return entries
        .groupBy { if (it is Entry.GameEntry) it.game.playedDate!! else (it as Entry.EventEntry).event.date }
        .toSortedMap(reverseOrder())
        .map { (day, dayEntries) -> Day(day, dayEntries.sortedWith(latestFirst)) }
}

@Composable
private fun ScheduleList(detail: SeasonDetail, contentPadding: PaddingValues, onlyMine: Boolean) {
    val res = res()
    val teamId = LocalAppModel.current.session?.team?.id
    val games = detail.allGames.filter { it.hasBothTeams && (!onlyMine || teamId == null || it.involves(teamId)) }
    val days = scheduleDays(detail, games, onlyMine)
    val undated = games.filter { it.playedDate == null }
    val listState = rememberLazyListState()

    // Start at today (or the next game day) instead of the latest day; a finished season starts at the top.
    var scrolledToToday by rememberSaveable(detail.season.id) { mutableStateOf(false) }
    LaunchedEffect(detail.season.id) {
        if (scrolledToToday) return@LaunchedEffect
        scrolledToToday = true
        val target = days.lastOrNull { it.day >= SwissTime.today } ?: return@LaunchedEffect
        listState.scrollToItem(headerIndex(target.day, days, undated))
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "note") { ArchivedSeasonNote() }
        if (days.isEmpty() && undated.isEmpty()) {
            item(key = "empty") { EmptyStateText(stringResource(R.string.no_games_yet)) }
        }
        if (undated.isNotEmpty()) {
            sectionHeader("undated") { SectionTitle(stringResource(R.string.not_yet_scheduled), Theme.secondaryText) }
            cardRows(undated.map { game -> "game-${game.id}" to @Composable { ScheduleRow(game, detail) } })
            if (detail.season.isRunning) {
                item(key = "undated-footer") {
                    Text(
                        stringResource(R.string.schedule_footer),
                        Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Theme.secondaryText,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        for (day in days) {
            val isToday = day.day == SwissTime.today
            sectionHeader("day-${day.day}") {
                val text = SwissTime.dateTime(day.day)?.longDate(res) ?: day.day
                SectionTitle(
                    if (isToday) stringResource(R.string.today_date, text) else text,
                    if (isToday) Theme.qualifier else Theme.secondaryText,
                )
            }
            cardRows(
                day.entries.map { entry ->
                    entry.key to @Composable {
                        when (entry) {
                            is Entry.GameEntry -> ScheduleRow(entry.game, detail)
                            is Entry.EventEntry -> EventRow(entry.event)
                        }
                    }
                },
            )
        }
    }
}

/** Index of a day's header in the list above, to scroll there: note, (undated header, rows, footer), then each day's header and rows. */
private fun headerIndex(day: String, days: List<Day>, undated: List<Game>): Int {
    var index = 1
    if (days.isEmpty() && undated.isEmpty()) index += 1
    if (undated.isNotEmpty()) index += 1 + undated.size + 1
    for (d in days) {
        if (d.day == day) return index
        index += 1 + d.entries.size
    }
    return 0
}

private fun LazyListScope.sectionHeader(key: String, content: @Composable () -> Unit) {
    item(key = key) {
        Box(Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(text, Modifier.semantics { heading() }, color = color, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
}

/** Rows grouped in one card, like an iOS inset-grouped list section: rounded ends, hairlines between. */
private fun LazyListScope.cardRows(rows: List<Pair<String, @Composable () -> Unit>>) {
    rows.forEachIndexed { index, (key, content) ->
        item(key = key) {
            val radius = Theme.cornerRadius
            val shape = RoundedCornerShape(
                topStart = if (index == 0) radius else 0.dp,
                topEnd = if (index == 0) radius else 0.dp,
                bottomStart = if (index == rows.lastIndex) radius else 0.dp,
                bottomEnd = if (index == rows.lastIndex) radius else 0.dp,
            )
            Column(Modifier.widthIn(max = 700.dp).fillMaxWidth().clip(shape).background(Theme.card)) {
                if (index > 0) HorizontalDivider(Modifier.padding(start = 98.dp, end = 16.dp), color = Theme.separator)
                content()
            }
        }
    }
}

/** Compact game row: time on the left, both teams stacked, sets on the right. */
@Composable
private fun ScheduleRow(game: Game, detail: SeasonDetail) {
    val navigator = LocalNavigator.current
    val teams = detail.teamsById
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { navigator.openGame(game.id) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.width(74.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            game.gameTime?.let { Text(it.take(5), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            Text(game.title(res()), color = Theme.secondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TeamLine(game, game.teamAId?.let { teams[it] }, game.shownSetsA)
            TeamLine(game, game.teamBId?.let { teams[it] }, game.shownSetsB)
        }
        if (LocalAppModel.current.canEdit(game) && !game.hasScore) {
            Icon(Icons.Outlined.EditNote, stringResource(R.string.enter_result), tint = Theme.qualifier)
        }
    }
}

@Composable
private fun TeamLine(game: Game, team: Team?, sets: Int?) {
    val isWinner = team != null && game.winnerTeamId == team.id
    val isLoser = team != null && game.winnerTeamId != null && !isWinner
    Row(verticalAlignment = Alignment.CenterVertically) {
        TeamLabel(team, Modifier.weight(1f), avatarSize = 22.dp, emphasized = isWinner, dimmed = isLoser)
        if (sets != null) {
            Text("$sets", color = if (isLoser) Theme.secondaryText else Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** A calendar entry from the website (group draw, victory party, ...). */
@Composable
private fun EventRow(event: CalendarEvent) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(event.time?.take(5).orEmpty(), Modifier.width(74.dp), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Icon(Icons.AutoMirrored.Outlined.EventNote, null, Modifier.size(22.dp), tint = Theme.qualifier)
        Column {
            Text(event.title, color = Color.White, fontWeight = FontWeight.Medium)
            event.location?.takeIf { it.isNotEmpty() }?.let { Text(it, color = Theme.secondaryText, fontSize = 12.sp) }
        }
    }
}
