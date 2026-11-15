package swiss.reutener.boccia.ui.team

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.DemoTeamsRoute
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.app.LocalNavigator
import swiss.reutener.boccia.app.SettingsRoute
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.Team
import swiss.reutener.boccia.model.allGames
import swiss.reutener.boccia.model.chronological
import swiss.reutener.boccia.model.hasBothTeams
import swiss.reutener.boccia.model.involves
import swiss.reutener.boccia.model.isComplete
import swiss.reutener.boccia.model.opponent
import swiss.reutener.boccia.model.playersSubtitle
import swiss.reutener.boccia.model.teamsById
import swiss.reutener.boccia.net.ApiException
import swiss.reutener.boccia.ui.components.DetailScaffold
import swiss.reutener.boccia.ui.components.SeasonLoadingState
import swiss.reutener.boccia.ui.components.TeamAvatar
import swiss.reutener.boccia.ui.components.TeamLabel
import swiss.reutener.boccia.ui.message
import swiss.reutener.boccia.ui.res
import swiss.reutener.boccia.ui.theme.EmptyStateText
import swiss.reutener.boccia.ui.theme.Theme
import swiss.reutener.boccia.ui.theme.ThemedScreen
import swiss.reutener.boccia.ui.theme.card

/**
 * Logged out: pick your team and enter its PIN (same PIN as on the website).
 * Logged in: your team's open games with quick access to result entry, then its played games.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTeamScreen(contentPadding: PaddingValues) {
    val model = LocalAppModel.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val session = model.session
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (session == null) R.string.log_in else R.string.tab_my_team),
                Modifier.weight(1f).semantics { heading() },
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { navigator.open(SettingsRoute) }) {
                Icon(Icons.Outlined.Settings, stringResource(R.string.settings), tint = Color.White)
            }
        }
        PullToRefreshBox(
            isRefreshing = model.isLoading,
            onRefresh = {
                scope.launch {
                    model.refresh().join()
                    model.validateSession()
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (session == null) teamPicker() else loggedIn(session.team)
            }
        }
    }
}

// MARK: Logged out

private fun LazyListScope.teamPicker() {
    item(key = "intro") {
        Intro(Icons.Filled.Key, Theme.qualifier, stringResource(R.string.login_intro))
    }
    item(key = "teams") {
        val model = LocalAppModel.current
        val detail = model.currentDetail
        when {
            detail != null -> TeamGroupSections(detail)
            model.seasons.isEmpty() || model.currentSeason != null -> SeasonLoadingState()
            else -> EmptyStateText(stringResource(R.string.no_season_login))
        }
    }
    item(key = "demo") {
        val model = LocalAppModel.current
        val navigator = LocalNavigator.current
        if (model.demo != null) {
            Column(Modifier.widthIn(max = 700.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth().card().clickable { navigator.open(DemoTeamsRoute) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Theme.demo)
                    Text(stringResource(R.string.try_demo), Modifier.weight(1f), color = Color.White, fontSize = 16.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.tertiaryText)
                }
                Footnote(stringResource(R.string.demo_footer))
            }
        }
    }
}

@Composable
private fun Intro(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String) {
    Column(Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(40.dp), tint = tint)
        Text(text, color = Theme.secondaryText, fontSize = 16.sp)
    }
}

@Composable
private fun Footnote(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(horizontal = 16.dp), color = Theme.secondaryText, fontSize = 13.sp)
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        Modifier.padding(start = 16.dp, top = 12.dp).semantics { heading() },
        color = Theme.secondaryText,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

/** A season's teams by group, each opening the PIN entry. */
@Composable
private fun TeamGroupSections(detail: SeasonDetail) {
    var loginTeam by remember { mutableStateOf<Team?>(null) }
    Column(Modifier.widthIn(max = 700.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (group in detail.groups) {
            SectionHeader(stringResource(R.string.group_name, group.name))
            Column(Modifier.fillMaxWidth().card()) {
                detail.teams.filter { it.groupId == group.id }.sortedBy { it.name.lowercase() }.forEachIndexed { index, team ->
                    if (index > 0) HorizontalDivider(Modifier.padding(start = 58.dp), color = Theme.separator)
                    Row(
                        Modifier.fillMaxWidth().clickable { loginTeam = team }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TeamLabel(team, Modifier.weight(1f), avatarSize = 32.dp)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.tertiaryText)
                    }
                }
            }
        }
    }
    loginTeam?.let { PinEntryDialog(it) { loginTeam = null } }
}

/** "Demo ausprobieren": the demo season's made-up teams, all with the same public PIN. */
@Composable
fun DemoTeamsScreen() {
    val model = LocalAppModel.current
    val navigator = LocalNavigator.current
    var error by remember { mutableStateOf<ApiException?>(null) }
    LaunchedEffect(Unit) {
        try {
            model.loadDemoSeason()
        } catch (e: ApiException) {
            error = e
        }
    }
    // After a demo login, My Team shows the logged-in demo team underneath.
    LaunchedEffect(model.isDemoSession) {
        if (model.isDemoSession) navigator.back()
    }
    DetailScaffold(stringResource(R.string.demo)) {
        Intro(Icons.Filled.AutoAwesome, Theme.demo, stringResource(R.string.demo_pin_hint, model.demo?.pin.orEmpty()))
        val detail = model.demo?.let { model.details[it.seasonId] }
        val failure = error
        when {
            detail != null -> TeamGroupSections(detail)
            failure != null -> EmptyStateText(failure.message(res()))
            else -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

/** Passcode-style PIN pad over the whole screen; logs in as soon as all four digits are in. */
@Composable
private fun PinEntryDialog(team: Team, onDismiss: () -> Unit) {
    val model = LocalAppModel.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val length = 4
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<ApiException?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val res = res()

    fun type(digit: String) {
        if (isChecking || pin.length >= length) return
        error = null
        pin += digit
        if (pin.length < length) return
        isChecking = true
        scope.launch {
            try {
                model.login(team.id, pin)
                onDismiss()
            } catch (e: ApiException) {
                error = e
                pin = ""
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                shake.animateTo(0f, keyframes {
                    durationMillis = 400
                    for (i in 1..7) (if (i % 2 == 0) -10f else 10f) at i * 50
                })
            } finally {
                isChecking = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        ThemedScreen {
            Column(
                Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, stringResource(R.string.close), tint = Color.White)
                    }
                }
                Spacer(Modifier.weight(1f))
                TeamAvatar(team, 72.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(team.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.enter_pin), color = Theme.secondaryText)
                }
                val progress = stringResource(R.string.pin_progress, pin.length, length)
                Row(
                    Modifier.offset(x = shake.value.dp).semantics { contentDescription = progress },
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    repeat(length) { index ->
                        Box(
                            Modifier
                                .size(16.dp)
                                .border(1.5.dp, Color.White, CircleShape)
                                .background(if (index < pin.length) Color.White else Color.Transparent, CircleShape),
                        )
                    }
                }
                Text(
                    error?.message(res) ?: " ",
                    Modifier.heightIn(min = 36.dp),
                    color = Color(0xFFFF8A80),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Keypad(onDigit = ::type, onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) })
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Keypad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                for (key in row) {
                    val modifier = Modifier.size(76.dp)
                    when (key) {
                        "" -> Spacer(modifier)
                        "⌫" -> Box(modifier.clip(CircleShape).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Outlined.Backspace, stringResource(R.string.delete), tint = Color.White)
                        }
                        else -> Box(
                            modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).clickable { onDigit(key) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(key, color = Color.White, fontSize = 32.sp)
                        }
                    }
                }
            }
        }
    }
}

// MARK: Logged in

private fun LazyListScope.loggedIn(team: Team) {
    item(key = "team") {
        val model = LocalAppModel.current
        val navigator = LocalNavigator.current
        Row(
            Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .card()
                .clickable { navigator.openTeam(team.id, model.currentSeason?.id) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TeamAvatar(team, 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(team.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                team.playersSubtitle?.let { Text(it, color = Theme.secondaryText) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = Theme.qualifier)
                    Text(stringResource(R.string.logged_in), color = Theme.qualifier, fontSize = 12.sp)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.tertiaryText)
        }
    }
    item(key = "games") {
        val model = LocalAppModel.current
        val detail = model.currentDetail
        if (detail != null) {
            MyGames(team, detail)
        } else if (model.currentSeason == null && model.seasons.isNotEmpty()) {
            EmptyStateText(stringResource(R.string.error_no_current_season))
        }
    }
    item(key = "logout") {
        val model = LocalAppModel.current
        Box(
            Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(top = 16.dp).card().clickable { model.logout() }.padding(16.dp),
        ) {
            Text(stringResource(R.string.log_out), color = Color(0xFFFF6961), fontSize = 16.sp)
        }
    }
}

@Composable
private fun MyGames(team: Team, detail: SeasonDetail) {
    val teams = detail.teamsById
    val mine = detail.allGames.filter { it.involves(team.id) && it.hasBothTeams }
    val open = mine.filter { !it.isComplete }.sortedWith(chronological)
    val played = mine.filter { it.isComplete }.sortedWith(chronological.reversed())
    Column(Modifier.widthIn(max = 700.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(R.string.open_games))
        Column(Modifier.fillMaxWidth().card()) {
            if (open.isEmpty()) {
                Text(stringResource(R.string.no_open_games), Modifier.padding(16.dp), color = Theme.secondaryText)
            }
            open.forEachIndexed { index, game ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = 60.dp), color = Theme.separator)
                TeamGameRow(game, team.id, game.opponent(team.id)?.let { teams[it] })
            }
        }
        if (open.isNotEmpty()) Footnote(stringResource(R.string.tap_game_hint))
        if (played.isNotEmpty()) {
            SectionHeader(stringResource(R.string.played))
            Column(Modifier.fillMaxWidth().card()) {
                played.forEachIndexed { index, game ->
                    if (index > 0) HorizontalDivider(Modifier.padding(start = 60.dp), color = Theme.separator)
                    TeamGameRow(game, team.id, game.opponent(team.id)?.let { teams[it] })
                }
            }
        }
    }
}
