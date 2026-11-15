package swiss.reutener.boccia.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.app.LocalNavigator
import swiss.reutener.boccia.model.Game
import swiss.reutener.boccia.model.Phase
import swiss.reutener.boccia.model.Team
import swiss.reutener.boccia.model.TeamDetail
import swiss.reutener.boccia.model.chronological
import swiss.reutener.boccia.model.hasScore
import swiss.reutener.boccia.model.isComplete
import swiss.reutener.boccia.model.isDraw
import swiss.reutener.boccia.model.opponent
import swiss.reutener.boccia.model.playersSubtitle
import swiss.reutener.boccia.net.ApiException
import swiss.reutener.boccia.ui.components.DetailScaffold
import swiss.reutener.boccia.ui.components.InfoCard
import swiss.reutener.boccia.ui.components.InfoRow
import swiss.reutener.boccia.ui.components.TeamAvatar
import swiss.reutener.boccia.ui.dateLabel
import swiss.reutener.boccia.ui.message
import swiss.reutener.boccia.ui.res
import swiss.reutener.boccia.ui.theme.Theme
import swiss.reutener.boccia.ui.theme.card
import swiss.reutener.boccia.ui.title

/** Latest round first, like the bracket read from the final back to the group phase. */
private val phaseOrder = listOf(Phase.FINAL, Phase.THIRD, Phase.SF, Phase.QF, Phase.GROUP)

/**
 * A team's page: who plays in it, its record, and its games in one season (switchable to any
 * season it took part in, like the website's team page).
 */
@Composable
fun TeamDetailScreen(teamId: Int, initialSeasonId: Int?) {
    val model = LocalAppModel.current
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<TeamDetail?>(null) }
    var error by remember { mutableStateOf<ApiException?>(null) }
    var seasonId by rememberSaveable { mutableIntStateOf(-1) }

    suspend fun load() {
        try {
            val loaded = model.teamDetail(teamId)
            detail = loaded
            error = null
            if (seasonId < 0) {
                val enrolled = loaded.enrollments.map { it.seasonId }
                seasonId = initialSeasonId?.takeIf { it in enrolled } ?: loaded.enrollments.firstOrNull()?.seasonId ?: -1
            }
        } catch (e: ApiException) {
            error = e
        }
    }
    LaunchedEffect(teamId) { load() }

    DetailScaffold(detail?.team?.name.orEmpty()) {
        val loaded = detail
        val failure = error
        when {
            loaded != null -> Content(loaded, seasonId, selectSeason = { seasonId = it }, reload = { load() })
            failure != null -> Column(
                Modifier.fillMaxWidth().padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.CloudOff, null, Modifier.size(48.dp), tint = Theme.secondaryText)
                Text(stringResource(R.string.team_load_failed), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Text(failure.message(res()), color = Theme.secondaryText, textAlign = TextAlign.Center)
                FilledTonalButton(onClick = { scope.launch { load() } }) { Text(stringResource(R.string.try_again)) }
            }
            else -> Box(Modifier.fillMaxWidth().padding(top = 120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun Content(detail: TeamDetail, seasonId: Int, selectSeason: (Int) -> Unit, reload: suspend () -> Unit) {
    val res = res()
    val team = detail.team
    val enrollment = detail.enrollments.firstOrNull { it.seasonId == seasonId }
    val games = detail.games.filter { it.seasonId == seasonId }
    val teams = (detail.opponents + team).associateBy { it.id }
    var showsPhoto by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TeamAvatar(
            team,
            96.dp,
            if (team.photoUrl != null) Modifier.clickable(onClickLabel = stringResource(R.string.enlarge_photo)) { showsPhoto = true } else Modifier,
        )
        Text(team.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        team.playersSubtitle?.let { Text(it, color = Theme.secondaryText) }
        if (LocalAppModel.current.session?.team?.id == team.id) {
            TeamPhotoPicker(team, onUploaded = reload)
        }
        Record(games, team.id, Modifier.padding(top = 6.dp))
    }

    if (detail.enrollments.size > 1) {
        SeasonMenu(detail, seasonId, selectSeason)
    } else if (enrollment != null) {
        InfoCard { InfoRow(stringResource(R.string.season), enrollment.seasonLabel, first = true) }
    }

    if (games.isEmpty()) {
        InfoCard {
            Text(stringResource(R.string.no_games_this_season), Modifier.padding(16.dp), color = Theme.secondaryText)
        }
    }
    for (phase in phaseOrder) {
        val phaseGames = games.filter { it.phase == phase }.sortedWith(chronological.reversed())
        if (phaseGames.isEmpty()) continue
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (phase == Phase.GROUP && enrollment != null) {
                    stringResource(R.string.phase_with_group, phase.title(res), enrollment.groupName)
                } else {
                    phase.title(res)
                },
                Modifier.padding(start = 16.dp).semantics { heading() },
                color = Theme.secondaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            InfoCard {
                phaseGames.forEachIndexed { index, game ->
                    if (index > 0) HorizontalDivider(Modifier.padding(start = 60.dp), color = Theme.separator)
                    TeamGameRow(game, team.id, game.opponent(team.id)?.let { teams[it] })
                }
            }
        }
    }

    if (showsPhoto) PhotoDialog(team) { showsPhoto = false }
}

@Composable
private fun Record(games: List<Game>, teamId: Int, modifier: Modifier) {
    val played = games.filter { it.isComplete }
    val won = played.count { it.winnerTeamId == teamId }
    val tied = played.count { it.isDraw }
    val lost = played.size - won - tied
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Stat(won, stringResource(R.string.wins))
        if (tied > 0) Stat(tied, stringResource(R.string.draws))
        Stat(lost, stringResource(R.string.losses))
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Theme.secondaryText, fontSize = 12.sp)
    }
}

@Composable
private fun SeasonMenu(detail: TeamDetail, seasonId: Int, selectSeason: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = detail.enrollments.firstOrNull { it.seasonId == seasonId }?.seasonLabel.orEmpty()
    Box {
        Row(
            Modifier.fillMaxWidth().card().clickable { expanded = true }.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.season), Modifier.weight(1f), color = Color.White, fontSize = 16.sp)
            Text(label, color = Theme.qualifier, fontSize = 16.sp)
            Icon(Icons.Filled.UnfoldMore, null, Modifier.size(18.dp), tint = Theme.qualifier)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            for (enrollment in detail.enrollments) {
                DropdownMenuItem(
                    text = { Text(enrollment.seasonLabel) },
                    onClick = {
                        expanded = false
                        selectSeason(enrollment.seasonId)
                    },
                )
            }
        }
    }
}

/**
 * One of the team's games, from its point of view: opponent, round, date, and the result (W/L).
 * Also used for the logged-in team's games under "Mein Team".
 */
@Composable
fun TeamGameRow(game: Game, teamId: Int, opponent: Team?) {
    val navigator = LocalNavigator.current
    val model = LocalAppModel.current
    val scope = rememberCoroutineScope()
    val res = res()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                // The game page looks the game up in loaded season data; make sure that season is there.
                scope.launch {
                    if (model.details[game.seasonId] == null) runCatching { model.loadSeason(game.seasonId) }
                    navigator.openGame(game.id)
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TeamAvatar(opponent, 32.dp)
        Column(Modifier.weight(1f)) {
            Text(
                opponent?.let { stringResource(R.string.versus, it.name) } ?: stringResource(R.string.opponent_unknown),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(game.title(res), game.dateLabel(res)).joinToString(" · "),
                color = Theme.secondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (game.isComplete) {
            ResultBadge(game, teamId)
        } else if (model.canEdit(game)) {
            Icon(Icons.Outlined.EditNote, stringResource(R.string.enter_result), tint = Theme.qualifier)
        }
    }
}

@Composable
private fun ResultBadge(game: Game, teamId: Int) {
    val mine = (if (game.teamAId == teamId) game.setsA else game.setsB) ?: 0
    val theirs = (if (game.teamAId == teamId) game.setsB else game.setsA) ?: 0
    val (letter, color) = when {
        mine > theirs -> R.string.badge_won to Theme.won
        mine < theirs -> R.string.badge_lost to Theme.lost
        else -> R.string.badge_draw to Color.Gray
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (game.hasScore) Text("$mine:$theirs", color = Color.White, fontWeight = FontWeight.SemiBold)
        Box(Modifier.size(22.dp).background(color.copy(alpha = 0.85f), CircleShape), contentAlignment = Alignment.Center) {
            Text(stringResource(letter), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** The team photo, large over a dimmed page, like the website's photo view. Tap anywhere to close. */
@Composable
private fun PhotoDialog(team: Team, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = team.photoUrl,
                contentDescription = team.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)),
            )
        }
    }
}
