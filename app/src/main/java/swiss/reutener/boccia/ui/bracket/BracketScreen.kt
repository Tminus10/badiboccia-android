package swiss.reutener.boccia.ui.bracket

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.app.LocalNavigator
import swiss.reutener.boccia.model.Game
import swiss.reutener.boccia.model.Phase
import swiss.reutener.boccia.model.Placeholder
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.Team
import swiss.reutener.boccia.model.groupPhaseStarted
import swiss.reutener.boccia.model.shownSetsA
import swiss.reutener.boccia.model.shownSetsB
import swiss.reutener.boccia.model.teamsById
import swiss.reutener.boccia.ui.components.ArchivedSeasonNote
import swiss.reutener.boccia.ui.components.SeasonLoadingState
import swiss.reutener.boccia.ui.components.SeasonPicker
import swiss.reutener.boccia.ui.components.TeamAvatar
import swiss.reutener.boccia.ui.components.TeamLabel
import swiss.reutener.boccia.ui.dateLabel
import swiss.reutener.boccia.ui.res
import swiss.reutener.boccia.ui.theme.CardHeading
import swiss.reutener.boccia.ui.theme.Theme
import swiss.reutener.boccia.ui.theme.card
import swiss.reutener.boccia.ui.theme.colorFromHex
import swiss.reutener.boccia.ui.title
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The phase bar's columns: the group tables first, then each knockout round. */
enum class BracketPhase(val abbreviation: Int, val phase: Phase) {
    GROUPS(R.string.phase_abbr_groups, Phase.GROUP),
    QUARTER_FINALS(R.string.phase_abbr_qf, Phase.QF),
    SEMI_FINALS(R.string.phase_abbr_sf, Phase.SF),
    FINAL(R.string.phase_abbr_final, Phase.FINAL),
}

private const val HORIZONTAL_INSET = 16f

/** Below the bracket, so its end isn't glued to the tab bar. */
private const val BOTTOM_PADDING = 24f

/** Room for the champion badge above the final, including the gap to the card. */
private const val CHAMPION_HEIGHT = 126f

/**
 * The tournament tree, modeled on the Sports app's knockout view (like the iOS app's BracketView):
 * a phase bar on top (groups, quarter-finals, semi-finals, final) whose frame marks the visible
 * columns, and columns that snap horizontally -- group tables first, then each knockout round.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BracketScreen(contentPadding: PaddingValues) {
    val model = LocalAppModel.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val phases = BracketPhase.entries
    // Card rows grow with the system font size, like @ScaledMetric on iOS.
    val headerHeight = 28f * density.fontScale
    val rowHeight = 38f * density.fontScale

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val visible = BracketLayout.visibleColumns(maxWidth.value, phases.size)
        // Never negative: the first layout pass can report a zero width.
        val columnWidth = max((maxWidth.value - 2 * HORIZONTAL_INSET) / max(visible, 1), 0f)
        val layout = BracketLayout(columnWidth, headerHeight, rowHeight)
        val maxFocus = phases.size - visible
        val columnPx = with(density) { columnWidth.dp.toPx() }

        val horizontal = rememberScrollState()
        val scrolledColumns = if (columnPx > 0) horizontal.value / columnPx else 0f
        val focus = scrolledColumns.roundToInt().coerceIn(0, maxFocus)
        val snapping = remember(columnPx, maxFocus) { ColumnSnapping(horizontal, columnPx, maxFocus) }

        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.EmojiEvents, null, Modifier.size(38.dp), tint = Theme.qualifier)
                SeasonPicker()
                ArchivedSeasonNote()
            }
            PhaseBar(phases, scrolledColumns, visible, Modifier.padding(horizontal = HORIZONTAL_INSET.dp, vertical = 12.dp)) { index ->
                scope.launch { horizontal.animateScrollTo((min(index, maxFocus) * columnPx).roundToInt()) }
            }

            val detail = model.selectedDetail
            if (detail == null) {
                SeasonLoadingState()
                return@Column
            }
            PullToRefreshBox(
                isRefreshing = model.isLoading,
                onRefresh = { model.refresh() },
                modifier = Modifier.weight(1f),
            ) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val tabBar = contentPadding.calculateBottomPadding().value
                    // The bracket's height on screen, between the phase bar and the tab bar.
                    val visibleHeight = maxHeight.value - tabBar
                    val focusRound = max(focus - 1, 0)
                    // Knockout rounds are 0 (quarter-finals) to 2 (final); the phase bar counts the groups first.
                    val visibleRounds = (focus - 1) until (focus + visible - 1)
                    val placement = knockoutPlacement(
                        detail, layout, focusRound, visibleRounds, visibleHeight,
                        centerBlock = visible >= 3 || phases[focus] == BracketPhase.SEMI_FINALS,
                    )
                    // The rounds glide into their new packing when the visible columns change.
                    val animatedFocusRound by animateFloatAsState(focusRound.toFloat(), label = "focusRound")
                    val topInset by animateFloatAsState(placement.topInset, label = "topInset")

                    val row = @Composable { rowModifier: Modifier ->
                        Row(
                            rowModifier
                                .horizontalScroll(horizontal, enabled = maxFocus > 0, flingBehavior = rememberSnapFlingBehavior(snapping))
                                .padding(horizontal = HORIZONTAL_INSET.dp),
                        ) {
                            for (phase in phases) {
                                Box(Modifier.width(columnWidth.dp)) {
                                    when (phase) {
                                        BracketPhase.GROUPS -> {
                                            // No connector lines leave the group column, so it only needs a small gap.
                                            val groups = @Composable { GroupStageColumn(detail, max(columnWidth - 10, 0f)) }
                                            if (placement.isPinned) {
                                                // The knockout rounds stay put; the group tables scroll on their own,
                                                // down to the edge of the display behind the tab bar.
                                                Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                                                    groups()
                                                    Spacer(Modifier.height((tabBar + BOTTOM_PADDING).dp))
                                                }
                                            } else {
                                                groups()
                                            }
                                        }
                                        BracketPhase.QUARTER_FINALS ->
                                            KnockoutColumn(0, detail.bracket.qf, detail, layout, animatedFocusRound, topInset)
                                        BracketPhase.SEMI_FINALS ->
                                            KnockoutColumn(1, detail.bracket.sf, detail, layout, animatedFocusRound, topInset)
                                        BracketPhase.FINAL -> KnockoutColumn(
                                            2, listOfNotNull(detail.bracket.final), detail, layout, animatedFocusRound, topInset,
                                            thirdPlace = detail.bracket.third,
                                            // Pinned: down at the bottom of the screen, like on the website.
                                            thirdPlaceBottom = if (placement.isPinned) visibleHeight - BOTTOM_PADDING - placement.topInset else placement.blockHeight,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (placement.isPinned) {
                        // Exactly one screen high: nothing moves vertically but the group column.
                        row(Modifier.fillMaxHeight())
                    } else {
                        // Too tall: everything scrolls together.
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            row(Modifier)
                            Spacer(Modifier.height((tabBar + BOTTOM_PADDING).dp))
                        }
                    }
                }
            }
        }
    }
}

/** Snaps the horizontal scroll to whole columns, in the direction of a fling. */
private class ColumnSnapping(
    private val state: androidx.compose.foundation.ScrollState,
    private val columnPx: Float,
    private val maxFocus: Int,
) : SnapLayoutInfoProvider {
    override fun calculateSnapOffset(velocity: Float): Float {
        if (columnPx <= 0f) return 0f
        val current = state.value / columnPx
        val target = when {
            abs(velocity) < 400f -> current.roundToInt()
            velocity > 0 -> ceil(current - 0.05f).toInt()
            else -> floor(current + 0.05f).toInt()
        }.coerceIn(0, maxFocus)
        return target * columnPx - state.value
    }
}

/** Where the knockout rounds sit vertically. All of them move down together, so the connector lines still meet. */
private data class KnockoutPlacement(
    val topInset: Float,
    /** Height of the leftmost visible knockout round (quarter- or semi-finals), the block the later rounds hang off. */
    val blockHeight: Float,
    /** The knockout rounds fit on screen: they stay put in the vertical middle and only the group column scrolls. */
    val isPinned: Boolean,
)

private fun knockoutPlacement(
    detail: SeasonDetail,
    layout: BracketLayout,
    focusRound: Int,
    visibleRounds: IntRange,
    visibleHeight: Float,
    centerBlock: Boolean,
): KnockoutPlacement {
    val focus = focusRound.toFloat()
    val availableHeight = visibleHeight - BOTTOM_PADDING
    val championInset = if (detail.podium?.championId != null) layout.insetAboveFinal(CHAMPION_HEIGHT, focus) else 0f
    val blockGames = if (focusRound == 0) detail.bracket.qf.size else detail.bracket.sf.size
    val blockHeight = layout.height(focusRound, blockGames, focus)
    // Only the rounds in view count: on phones, the final (with the third-place game) is off to the
    // right next to the quarter-finals, and the quarter-finals are off to the left next to the final.
    val heights = listOf(
        if (0 in visibleRounds) knockoutColumnHeight(0, detail.bracket.qf.size, layout, focus) else 0f,
        if (1 in visibleRounds) knockoutColumnHeight(1, detail.bracket.sf.size, layout, focus) else 0f,
        if (2 in visibleRounds) {
            knockoutColumnHeight(2, if (detail.bracket.final == null) 0 else 1, layout, focus, detail.bracket.third != null, blockHeight)
        } else {
            0f
        },
    )
    val contentHeight = heights.max()
    // Centered between the phase bar and the tab bar.
    val centeredInset = (visibleHeight - blockHeight) / 2

    if (visibleHeight > 0 && championInset + contentHeight <= availableHeight) {
        // Centered on the leftmost round, but never pushing the third-place game out of view.
        val inset = max(championInset, min(centeredInset, availableHeight - contentHeight))
        return KnockoutPlacement(inset, blockHeight, isPinned = true)
    }
    // Too tall: everything scrolls together. The block is still centered where there's room
    // (tablets, and phones with only the semi-finals and the final in view).
    return KnockoutPlacement(max(championInset, if (centerBlock) centeredInset else 0f), blockHeight, isPinned = false)
}

// MARK: Knockout column

/** Space between the final and the third-place game, so the champion and the final read as one block. */
private const val THIRD_PLACE_GAP = 32f

/** The third-place game's label and card. */
private fun thirdPlaceHeight(layout: BracketLayout) = 32f + layout.cardHeight

/** At `bottom` when there's room, else just below the final. */
private fun thirdPlaceTop(finalBottom: Float, bottom: Float, layout: BracketLayout) =
    max(finalBottom + THIRD_PLACE_GAP, bottom - thirdPlaceHeight(layout))

/** A knockout column's height without the top inset; used to tell whether the knockout rounds fit on screen. */
private fun knockoutColumnHeight(
    round: Int,
    gameCount: Int,
    layout: BracketLayout,
    focusRound: Float,
    hasThirdPlace: Boolean = false,
    thirdPlaceBottom: Float = 0f,
): Float {
    val finalBottom = layout.height(round, gameCount, focusRound)
    if (!hasThirdPlace) return finalBottom
    return thirdPlaceTop(finalBottom, thirdPlaceBottom, layout) + thirdPlaceHeight(layout)
}

/** One knockout round, with the bracket lines leading to the next round. */
@Composable
private fun KnockoutColumn(
    round: Int,
    games: List<Game>,
    detail: SeasonDetail,
    layout: BracketLayout,
    focusRound: Float,
    topInset: Float,
    thirdPlace: Game? = null,
    /**
     * Where the third-place game's bottom edge goes when there's room below the final: the bottom of
     * the screen when the knockout rounds are pinned, else the bottom of the leftmost visible round.
     */
    thirdPlaceBottom: Float = 0f,
) {
    val height = knockoutColumnHeight(round, games.size, layout, focusRound, thirdPlace != null, thirdPlaceBottom)
    Box(Modifier.padding(top = topInset.dp).width(layout.columnWidth.dp).height(height.dp)) {
        if (games.isEmpty()) {
            Box(Modifier.size(layout.cardWidth.dp, layout.cardHeight.dp).card(14.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.not_set_yet), color = Theme.secondaryText, fontSize = 15.sp)
            }
        }
        if (round < 2) {
            // Faded out while this round is left of the visible ones.
            Connectors(round, games.size, layout, focusRound, Modifier.alpha((1f - (focusRound - round)).coerceIn(0f, 1f)))
        }
        games.forEachIndexed { index, game ->
            BracketGameCard(game, detail, layout, Modifier.offset(y = layout.topY(round, index, focusRound).dp))
        }
        val championId = detail.podium?.championId
        val champion = championId?.let { detail.teamsById[it] }
        if (round == 2 && champion != null) {
            Box(
                Modifier
                    .offset(y = (layout.topY(round, 0, focusRound) - CHAMPION_HEIGHT).dp)
                    .size(layout.cardWidth.dp, (CHAMPION_HEIGHT - 14).dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                ChampionBadge(champion, detail.season.id)
            }
        }
        if (thirdPlace != null) {
            val finalBottom = layout.height(round, games.size, focusRound)
            Column(Modifier.offset(y = thirdPlaceTop(finalBottom, thirdPlaceBottom, layout).dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // The card's header names the game itself until it has a date.
                Box(Modifier.height(24.dp), contentAlignment = Alignment.BottomStart) {
                    Text(
                        if (thirdPlace.dateLabel(res()) != null) Phase.THIRD.title(res()) else " ",
                        color = Theme.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                BracketGameCard(thirdPlace, detail, layout)
            }
        }
    }
}

/** "⊐" shapes joining each pair of games, plus the stub into the next round's game. */
@Composable
private fun Connectors(round: Int, gameCount: Int, layout: BracketLayout, focusRound: Float, modifier: Modifier) {
    val line = Color.White.copy(alpha = 0.28f)
    Canvas(modifier.fillMaxSize()) {
        val px = { value: Float -> value * density }
        val start = px(layout.cardWidth)
        val mid = px(layout.cardWidth + layout.connectorGap / 2)
        val end = px(layout.columnWidth)
        val stroke = 1.dp.toPx()
        for (pair in 0 until gameCount / 2) {
            val y1 = px(layout.centerY(round, 2 * pair, focusRound))
            val y2 = px(layout.centerY(round, 2 * pair + 1, focusRound))
            val yNext = px(layout.centerY(round + 1, pair, focusRound))
            drawLine(line, Offset(start, y1), Offset(mid, y1), stroke)
            drawLine(line, Offset(start, y2), Offset(mid, y2), stroke)
            drawLine(line, Offset(mid, minOf(y1, y2, yNext)), Offset(mid, maxOf(y1, y2, yNext)), stroke)
            drawLine(line, Offset(mid, yNext), Offset(end, yNext), stroke)
        }
    }
}

/** The trophy and the winning team, above the final. */
@Composable
private fun ChampionBadge(team: Team, seasonId: Int) {
    val navigator = LocalNavigator.current
    Column(
        Modifier.fillMaxWidth().clickable { navigator.openTeam(team.id, seasonId) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            TeamAvatar(team, 48.dp)
            Icon(
                Icons.Filled.EmojiEvents,
                null,
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 4.dp)
                    .size(22.dp)
                    .background(Theme.backgroundBottom, CircleShape)
                    .padding(3.dp),
                tint = Theme.qualifier,
            )
        }
        Text(team.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(stringResource(R.string.champion), color = Theme.qualifier, fontSize = 12.sp)
    }
}

/** A bracket game: date (or round name) on top, then both teams with their sets; the loser is dimmed. */
@Composable
private fun BracketGameCard(game: Game, detail: SeasonDetail, layout: BracketLayout, modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val res = res()
    val teams = detail.teamsById
    Column(
        modifier
            .size(layout.cardWidth.dp, layout.cardHeight.dp)
            .card(12.dp)
            .clickable(onClickLabel = stringResource(R.string.game_details_hint)) { navigator.openGame(game.id) },
    ) {
        Box(
            Modifier.fillMaxWidth().height(layout.headerHeight.dp).background(Theme.cardHeader).padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(game.dateLabel(res) ?: game.title(res), color = Theme.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CardRow(game, game.teamAId?.let { teams[it] }, game.shownSetsA, game.placeholderA, layout)
        HorizontalDivider(color = Theme.separator)
        CardRow(game, game.teamBId?.let { teams[it] }, game.shownSetsB, game.placeholderB, layout)
    }
}

@Composable
private fun CardRow(game: Game, team: Team?, sets: Int?, placeholder: Placeholder?, layout: BracketLayout) {
    val isWinner = team != null && game.winnerTeamId == team.id
    val isLoser = team != null && game.winnerTeamId != null && !isWinner
    Row(Modifier.fillMaxWidth().height(layout.rowHeight.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TeamLabel(team, Modifier.weight(1f), placeholder, avatarSize = 20.dp, emphasized = isWinner, dimmed = isLoser, fontSize = 13.sp)
        if (sets != null) {
            Text("$sets", Modifier.padding(start = 4.dp), color = if (isLoser) Theme.secondaryText else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// MARK: Group stage column

/** Compact group tables (rank, team, points) as the tree's first column. */
@Composable
private fun GroupStageColumn(detail: SeasonDetail, width: Float) {
    val navigator = LocalNavigator.current
    val teams = detail.teamsById
    val started = detail.groupPhaseStarted
    Column(Modifier.width(width.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (group in detail.groups) {
            Column(Modifier.fillMaxWidth().card(14.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(30.dp).background(Theme.cardHeader).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardHeading(stringResource(R.string.group_name, group.name), Modifier.weight(1f))
                    CardHeading(stringResource(R.string.abbr_points))
                }
                HorizontalDivider(color = Theme.separator)
                for (row in group.standings) {
                    val team = teams[row.teamId]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .clickable { navigator.openTeam(row.teamId, detail.season.id) }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            "${row.rank}",
                            Modifier.width(14.dp),
                            color = if (started && row.rank <= detail.qualifiersPerGroup) Theme.qualifier else Theme.secondaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Box(Modifier.size(8.dp).background(colorFromHex(team?.colorHex), CircleShape))
                        Text(team?.name ?: "–", Modifier.weight(1f), color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${row.points}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// MARK: Phase bar

/**
 * Sports-app phase selector: labels above simple glyphs, and a white frame around the phases
 * currently visible below, following the columns while they scroll. Tapping a phase scrolls there.
 */
@Composable
private fun PhaseBar(phases: List<BracketPhase>, scrolledColumns: Float, visibleCount: Int, modifier: Modifier, select: (Int) -> Unit) {
    val res = res()
    fun isVisible(index: Int) = index >= scrolledColumns.roundToInt() && index < scrolledColumns.roundToInt() + visibleCount
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            phases.forEachIndexed { index, phase ->
                Text(
                    stringResource(phase.abbreviation),
                    Modifier.weight(1f),
                    color = if (isVisible(index)) Color.White else Theme.tertiaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(56.dp)) {
            val itemWidth = maxWidth / phases.size
            val shape = RoundedCornerShape(14.dp)
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.07f), shape))
            Box(
                Modifier
                    .offset(x = itemWidth * scrolledColumns)
                    .width(itemWidth * visibleCount)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.1f), shape)
                    .border(3.dp, Color.White, shape)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, Modifier.size(18.dp), tint = Color.White)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp), tint = Color.White)
                }
            }
            Row(Modifier.fillMaxSize()) {
                phases.forEachIndexed { index, phase ->
                    val title = phase.phase.title(res)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { select(index) }
                            .semantics {
                                contentDescription = title
                                role = Role.Tab
                                selected = isVisible(index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        PhaseGlyph(phase, if (isVisible(index)) Color.White else Theme.tertiaryText)
                    }
                }
            }
        }
    }
}

/** The little pictograms of the phase bar: many lines for early rounds, fewer later, a trophy for the final. */
@Composable
private fun PhaseGlyph(phase: BracketPhase, color: Color) {
    when (phase) {
        BracketPhase.GROUPS -> Lines(4, 4f, 3f, color)
        BracketPhase.QUARTER_FINALS -> Lines(4, 1.5f, 5f, color)
        BracketPhase.SEMI_FINALS -> Lines(2, 1.5f, 8f, color)
        BracketPhase.FINAL -> Icon(Icons.Outlined.EmojiEvents, null, Modifier.size(24.dp), tint = color)
    }
}

@Composable
private fun Lines(count: Int, thickness: Float, spacing: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
        repeat(count) {
            Box(Modifier.size(30.dp, thickness.dp).background(color, CircleShape))
        }
    }
}
