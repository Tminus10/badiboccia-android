package swiss.reutener.boccia.ui.game

import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.app.LocalNavigator
import swiss.reutener.boccia.app.ResultEntryRoute
import swiss.reutener.boccia.app.ScheduleGameRoute
import swiss.reutener.boccia.model.Game
import swiss.reutener.boccia.model.Placeholder
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.Team
import swiss.reutener.boccia.model.game
import swiss.reutener.boccia.model.hasBothTeams
import swiss.reutener.boccia.model.hasScore
import swiss.reutener.boccia.model.isComplete
import swiss.reutener.boccia.model.isDraw
import swiss.reutener.boccia.model.isEstimated
import swiss.reutener.boccia.model.isRunning
import swiss.reutener.boccia.model.playersSubtitle
import swiss.reutener.boccia.model.shownSetsA
import swiss.reutener.boccia.model.shownSetsB
import swiss.reutener.boccia.model.startDate
import swiss.reutener.boccia.model.teamsById
import swiss.reutener.boccia.ui.components.DetailScaffold
import swiss.reutener.boccia.ui.components.InfoCard
import swiss.reutener.boccia.ui.components.InfoRow
import swiss.reutener.boccia.ui.components.TeamAvatar
import swiss.reutener.boccia.ui.label
import swiss.reutener.boccia.ui.longDate
import swiss.reutener.boccia.ui.res
import swiss.reutener.boccia.ui.theme.EmptyStateText
import swiss.reutener.boccia.ui.theme.Theme
import swiss.reutener.boccia.ui.theme.card
import swiss.reutener.boccia.ui.timeText
import swiss.reutener.boccia.ui.title

/** A single game: big scoreboard header and its details. */
@Composable
fun GameDetailScreen(gameId: Int) {
    val model = LocalAppModel.current
    val res = res()
    val detail = model.details.values.firstOrNull { it.game(gameId) != null }
    val game = detail?.game(gameId)
    DetailScaffold(game?.title(res).orEmpty()) {
        if (detail == null || game == null) {
            Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Outlined.HelpOutline, null, Modifier.size(48.dp), tint = Theme.secondaryText)
                EmptyStateText(stringResource(R.string.game_not_found))
            }
            return@DetailScaffold
        }
        val teams = detail.teamsById
        val note = missingResultText(game, detail, res)
        ScoreHeader(game, game.teamAId?.let { teams[it] }, game.teamBId?.let { teams[it] }, note, detail.season.id)
        InfoCard {
            InfoRow(stringResource(R.string.round), game.title(res), first = true)
            InfoRow(stringResource(R.string.season), detail.season.label)
            InfoRow(
                stringResource(R.string.date),
                game.startDate?.longDate(res) ?: stringResource(if (isPending(game, detail)) R.string.still_open else R.string.unknown),
            )
            game.gameTime?.let { InfoRow(stringResource(R.string.time), slotText(game, it)) }
            if (note != null) {
                InfoRow(stringResource(R.string.result), note, valueSize = 13)
            } else {
                InfoRow(stringResource(R.string.result), "${game.setsA ?: 0} : ${game.setsB ?: 0}")
                if (game.isDraw) {
                    Text(
                        stringResource(R.string.interrupted_note),
                        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        color = Theme.secondaryText,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        if (model.canEdit(game)) {
            EditSection(game)
        } else if (model.session == null && game.hasBothTeams && model.selectedSeason?.isCurrent == true && !game.hasScore) {
            InfoCard {
                Text(stringResource(R.string.login_hint_game), Modifier.padding(16.dp), color = Theme.secondaryText, fontSize = 13.sp)
            }
        }
    }
}

/** For the two teams playing it, in the current season: entering the result or fixing the date. */
@Composable
private fun EditSection(game: Game) {
    val navigator = LocalNavigator.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoCard {
            EditLink(Icons.Outlined.EditNote, stringResource(if (game.hasScore) R.string.correct_result else R.string.enter_result)) {
                navigator.open(ResultEntryRoute(game.id))
            }
            HorizontalDivider(Modifier.padding(start = 56.dp), color = Theme.separator)
            EditLink(Icons.Outlined.EditCalendar, stringResource(if (game.playedDate == null) R.string.set_date else R.string.change_date)) {
                navigator.open(ScheduleGameRoute(game.id))
            }
        }
        Text(stringResource(R.string.change_log_note), Modifier.padding(horizontal = 16.dp), color = Theme.secondaryText, fontSize = 13.sp)
    }
}

@Composable
private fun EditLink(icon: ImageVector, title: String, open: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = open).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = Theme.qualifier)
        Text(title, Modifier.weight(1f), color = Theme.qualifier, fontSize = 16.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.tertiaryText)
    }
}

/** Whether missing details can still come. In a finished season the game won't be played anymore, they just never reached us. */
private fun isPending(game: Game, detail: SeasonDetail): Boolean = detail.season.isRunning || !game.hasBothTeams

/** Why a game has no score to show; null if it has one. */
private fun missingResultText(game: Game, detail: SeasonDetail, res: Resources): String? = when {
    game.isEstimated -> res.getString(R.string.winner_known)
    game.isComplete -> null
    isPending(game, detail) -> res.getString(R.string.still_open)
    else -> res.getString(R.string.result_unknown)
}

/** "18:00 – 19:30", using the website's assumed slot length for the phase. */
private fun slotText(game: Game, start: String): String {
    val begin = game.startDate ?: return start
    return "${start.take(5)} – ${begin.plusMinutes(game.slotMinutes.toLong()).timeText()}"
}

/** The Sports app's score layout: avatars and names left and right, big sets in between. */
@Composable
private fun ScoreHeader(game: Game, teamA: Team?, teamB: Team?, note: String?, seasonId: Int) {
    val avatarSize = 64.dp
    Column(
        Modifier.fillMaxWidth().card().padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Avatars stay level when names differ in length, and the score stays centered on them.
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Side(game, teamA, game.placeholderA, seasonId, Modifier.weight(1f))
            Box(Modifier.height(avatarSize), contentAlignment = Alignment.Center) {
                val a = game.shownSetsA
                val b = game.shownSetsB
                Text(
                    if (a != null && b != null) "$a : $b" else "– : –",
                    color = if (game.hasScore) Color.White else Theme.tertiaryText,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Side(game, teamB, game.placeholderB, seasonId, Modifier.weight(1f))
        }
        if (note != null) {
            Text(note, color = Theme.secondaryText, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Side(game: Game, team: Team?, placeholder: Placeholder?, seasonId: Int, modifier: Modifier) {
    val navigator = LocalNavigator.current
    Column(
        modifier.then(if (team != null) Modifier.clickable { navigator.openTeam(team.id, seasonId) } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            TeamAvatar(team, 64.dp)
            if (team != null && team.id == game.winnerTeamId) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    stringResource(R.string.winner),
                    Modifier.size(18.dp).align(Alignment.TopEnd).offset(x = 6.dp, y = (-8).dp),
                    tint = Theme.qualifier,
                )
            }
        }
        Text(
            team?.name ?: placeholder?.label(res()) ?: "–",
            color = if (team == null) Theme.secondaryText else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        team?.playersSubtitle?.let {
            Text(it, color = Theme.secondaryText, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}
