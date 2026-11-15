package swiss.reutener.boccia.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swiss.reutener.boccia.BuildConfig
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.app.LocalNavigator
import swiss.reutener.boccia.model.Podium
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.Team
import swiss.reutener.boccia.model.hasBothTeams
import swiss.reutener.boccia.model.isRunning
import swiss.reutener.boccia.model.teamsById
import swiss.reutener.boccia.ui.components.ArchivedSeasonNote
import swiss.reutener.boccia.ui.components.SeasonLoadingState
import swiss.reutener.boccia.ui.components.SeasonPicker
import swiss.reutener.boccia.ui.components.ServerBadge
import swiss.reutener.boccia.ui.components.TeamAvatar
import swiss.reutener.boccia.ui.settings.ServerDialog
import swiss.reutener.boccia.ui.theme.Theme
import swiss.reutener.boccia.ui.theme.card

/**
 * The home screen, modeled on the Sports app's tournament page: title and season capsule, a
 * scoreboard card (recent / today / upcoming), then one standings card per group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(contentPadding: PaddingValues) {
    val model = LocalAppModel.current
    PullToRefreshBox(
        isRefreshing = model.isLoading && model.selectedDetail != null,
        onRefresh = { model.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val wide = maxWidth >= 600.dp
            Column(
                Modifier
                    .widthIn(max = 1100.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Header()
                ArchivedSeasonNote()
                val detail = model.selectedDetail
                if (detail != null) {
                    detail.podium?.let { PodiumCard(it, detail) }
                    ScoreboardCard(detail)
                    Standings(detail, columns = if (wide) 2 else 1)
                } else {
                    SeasonLoadingState()
                }
            }
        }
    }
}

@Composable
private fun Header() {
    var showsServers by remember { mutableStateOf(false) }
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Hive, null, Modifier.size(36.dp), tint = Theme.qualifier)
            Text(
                stringResource(R.string.app_name),
                Modifier.semantics { heading() },
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
            )
            ServerBadge(Modifier.padding(start = 4.dp))
            Spacer(Modifier.weight(1f))
            // Debug builds only: switch between the live site, DEV and the local dev server.
            if (BuildConfig.DEBUG) {
                IconButton(onClick = { showsServers = true }) {
                    Icon(Icons.Outlined.Settings, stringResource(R.string.settings), tint = Color.White)
                }
            }
        }
        SeasonPicker()
    }
    if (showsServers) ServerDialog(onDismiss = { showsServers = false })
}

@Composable
private fun Standings(detail: SeasonDetail, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (row in detail.groups.chunked(columns)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (group in row) {
                    StandingsCard(group, detail, Modifier.weight(1f))
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Shown once the final is decided: the season's top three. */
@Composable
private fun PodiumCard(podium: Podium, detail: SeasonDetail) {
    val teams = detail.teamsById
    Column(
        Modifier.fillMaxWidth().card().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.final_standings), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Place(2, podium.runnerUpId?.let { teams[it] }, "🥈", 64.dp, detail, Modifier.weight(1f))
            Place(1, podium.championId?.let { teams[it] }, "🥇", 84.dp, detail, Modifier.weight(1f))
            Place(3, podium.thirdId?.let { teams[it] }, "🥉", 48.dp, detail, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Place(rank: Int, team: Team?, medal: String, height: Dp, detail: SeasonDetail, modifier: Modifier) {
    val navigator = LocalNavigator.current
    val third = detail.bracket.third
    val action: (() -> Unit)? = when {
        team != null -> ({ navigator.openTeam(team.id, detail.season.id) })
        // No third place to link to, but the game itself can still be opened.
        rank == 3 && third != null && third.hasBothTeams -> ({ navigator.openGame(third.id) })
        else -> null
    }
    Column(
        modifier.then(if (action != null) Modifier.clickable(onClick = action) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TeamAvatar(team, if (rank == 1) 56.dp else 44.dp)
        Text(
            team?.name ?: stringResource(if (detail.season.isRunning) R.string.open else R.string.result_unknown),
            Modifier.heightIn(min = 32.dp),
            color = if (team == null) Theme.secondaryText else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = height)
                .background(Color.White.copy(alpha = if (rank == 1) 0.14f else 0.08f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(medal, fontSize = 34.sp)
        }
    }
}
