package swiss.reutener.boccia.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalNavigator
import swiss.reutener.boccia.model.Game
import swiss.reutener.boccia.model.Placeholder
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.Team
import swiss.reutener.boccia.model.allGames
import swiss.reutener.boccia.model.bracketGames
import swiss.reutener.boccia.model.chronological
import swiss.reutener.boccia.model.hasBothTeams
import swiss.reutener.boccia.model.isComplete
import swiss.reutener.boccia.model.isEstimated
import swiss.reutener.boccia.model.shownSetsA
import swiss.reutener.boccia.model.shownSetsB
import swiss.reutener.boccia.model.startDate
import swiss.reutener.boccia.model.SwissTime
import swiss.reutener.boccia.model.teamsById
import swiss.reutener.boccia.ui.components.TeamAvatar
import swiss.reutener.boccia.ui.dateLabel
import swiss.reutener.boccia.ui.format
import swiss.reutener.boccia.ui.label
import swiss.reutener.boccia.ui.res
import swiss.reutener.boccia.ui.theme.EmptyStateText
import swiss.reutener.boccia.ui.theme.Theme
import swiss.reutener.boccia.ui.theme.card
import swiss.reutener.boccia.ui.timeText
import swiss.reutener.boccia.ui.title

/**
 * The Sports app's "Yesterday / Today / Upcoming" card. Boccia games are spread over the summer,
 * so "recent" is the latest day that has results and "upcoming" the next scheduled games.
 */
enum class Segment(val title: Int, val empty: Int) {
    RECENT(R.string.segment_recent, R.string.no_results_yet),
    TODAY(R.string.segment_today, R.string.no_games_today),
    UPCOMING(R.string.segment_upcoming, R.string.no_scheduled_games),
}

@Composable
fun ScoreboardCard(detail: SeasonDetail) {
    var segment by rememberSaveable { mutableStateOf<Segment?>(null) }
    val selected = segment ?: defaultSegment(detail)
    Column(Modifier.fillMaxWidth().card()) {
        Row {
            for (item in Segment.entries) {
                Text(
                    stringResource(item.title),
                    Modifier
                        .weight(1f)
                        .selectable(item == selected, role = Role.Tab) { segment = item }
                        .padding(vertical = 14.dp),
                    color = if (item == selected) Color.White else Theme.secondaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        HorizontalDivider(color = Theme.separator)
        val games = games(detail, selected)
        if (games.isEmpty()) {
            EmptyStateText(stringResource(selected.empty))
        } else {
            games.forEachIndexed { index, game ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Theme.separator)
                ScoreRow(game, detail, showDate = selected != Segment.TODAY)
            }
        }
    }
}

/** Today if there are games today, else what's coming up, else the latest results. */
private fun defaultSegment(detail: SeasonDetail): Segment = when {
    games(detail, Segment.TODAY).isNotEmpty() -> Segment.TODAY
    games(detail, Segment.UPCOMING).isNotEmpty() -> Segment.UPCOMING
    else -> Segment.RECENT
}

internal fun games(detail: SeasonDetail, segment: Segment): List<Game> {
    val today = SwissTime.today
    val candidates = detail.allGames.filter { it.hasBothTeams && it.playedDate != null }
    return when (segment) {
        Segment.TODAY -> candidates.filter { it.playedDate == today }.sortedWith(chronological)
        Segment.UPCOMING -> candidates.filter { it.playedDate!! > today && !it.isComplete }.sortedWith(chronological).take(4)
        Segment.RECENT -> {
            val played = candidates.filter { it.playedDate!! < today && it.isComplete }
            val lastDay = played.maxOfOrNull { it.playedDate!! }
                // Results without a date (older seasons) still deserve a place: show the latest bracket games.
                ?: return detail.bracketGames.filter { it.isComplete }.reversed().take(4)
            played.filter { it.playedDate == lastDay }.sortedWith(chronological)
        }
    }
}

/**
 * One game in big scoreboard style: avatar and name on each side, the sets as large numbers, and
 * what/when in the middle -- like the Sports app's live score rows.
 */
@Composable
private fun ScoreRow(game: Game, detail: SeasonDetail, showDate: Boolean) {
    val navigator = LocalNavigator.current
    val res = res()
    val teams = detail.teamsById
    val teamA = game.teamAId?.let { teams[it] }
    val teamB = game.teamBId?.let { teams[it] }
    val description = accessibilityText(game, teamA, teamB, res)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { navigator.openGame(game.id) }
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Side(teamA, game.placeholderA, Modifier.weight(1f))
        Score(game, game.shownSetsA, isWinner = game.winnerTeamId != null && game.winnerTeamId == teamA?.id)
        Column(Modifier.widthIn(min = 84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(game.title(res), color = Theme.tertiaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                statusText(game, showDate, res),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
        Score(game, game.shownSetsB, isWinner = game.winnerTeamId != null && game.winnerTeamId == teamB?.id)
        Side(teamB, game.placeholderB, Modifier.weight(1f))
    }
}

private fun statusText(game: Game, showDate: Boolean, res: android.content.res.Resources): String {
    val start = game.startDate
    val time = start?.timeText()
    val day = start?.format(res, "MMMd")
    if (game.isComplete) {
        return if (showDate) day ?: res.getString(R.string.finished) else res.getString(R.string.finished)
    }
    if (showDate) return listOfNotNull(day, if (game.gameTime != null) time else null).joinToString("\n")
    return if (game.gameTime != null) time.orEmpty() else res.getString(R.string.all_day)
}

@Composable
private fun Side(team: Team?, placeholder: Placeholder?, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TeamAvatar(team, 44.dp)
        Text(
            team?.name ?: placeholder?.label(res()) ?: "–",
            color = Theme.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun Score(game: Game, sets: Int?, isWinner: Boolean) {
    if (game.isEstimated && isWinner) {
        // No score to show, but the winner is known.
        Icon(Icons.Filled.WorkspacePremium, null, Modifier.size(28.dp), tint = Theme.qualifier)
        return
    }
    Text(
        sets?.toString() ?: "–",
        Modifier.widthIn(min = 28.dp),
        color = when {
            sets == null -> Theme.tertiaryText
            game.winnerTeamId == null || isWinner -> Color.White
            else -> Theme.secondaryText
        },
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

private fun accessibilityText(game: Game, teamA: Team?, teamB: Team?, res: android.content.res.Resources): String {
    val a = teamA?.name ?: game.placeholderA?.label(res) ?: "–"
    val b = teamB?.name ?: game.placeholderB?.label(res) ?: "–"
    val parts = mutableListOf(game.title(res), "$a – $b")
    val setsA = game.setsA
    val setsB = game.setsB
    if (game.isEstimated) {
        listOfNotNull(teamA, teamB).firstOrNull { it.id == game.winnerTeamId }?.let { parts += it.name }
        parts += res.getString(R.string.winner_known)
    } else if (setsA != null && setsB != null) {
        parts += res.getString(R.string.result_description, setsA, setsB)
    }
    game.dateLabel(res)?.let { parts += it }
    return parts.joinToString(", ")
}
