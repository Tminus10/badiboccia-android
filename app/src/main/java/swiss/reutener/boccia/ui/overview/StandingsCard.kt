package swiss.reutener.boccia.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalNavigator
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.TeamGroup
import swiss.reutener.boccia.model.isComplete
import swiss.reutener.boccia.model.teamsById
import swiss.reutener.boccia.ui.components.TeamAvatar
import swiss.reutener.boccia.ui.theme.EmptyStateText
import swiss.reutener.boccia.ui.theme.Theme
import swiss.reutener.boccia.ui.theme.card

/**
 * A group's table as in the Sports app: team, games played, won, drawn, lost, set difference, points.
 * The teams that go through to the quarter-finals are marked once the group phase has started.
 */
@Composable
fun StandingsCard(group: TeamGroup, detail: SeasonDetail, modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val teams = detail.teamsById
    val showQualifiers = group.games.any { it.isComplete }
    val showDraws = group.standings.any { it.tied > 0 }
    Column(modifier.fillMaxWidth().card(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.group_name, group.name),
            Modifier.padding(top = 16.dp, bottom = 10.dp).semantics { heading() },
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (group.standings.isEmpty()) {
            EmptyStateText(stringResource(R.string.no_teams_yet))
            return@Column
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            Row(Modifier.padding(bottom = 6.dp).clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.column_team), Modifier.weight(1f), color = Theme.secondaryText, fontSize = 15.sp)
                Stat(stringResource(R.string.abbr_played), Theme.secondaryText)
                Stat(stringResource(R.string.abbr_won), Theme.secondaryText)
                if (showDraws) Stat(stringResource(R.string.abbr_drawn), Theme.secondaryText)
                Stat(stringResource(R.string.abbr_lost), Theme.secondaryText)
                Stat(stringResource(R.string.abbr_set_difference), Theme.secondaryText, width = 30.dp)
                Stat(stringResource(R.string.abbr_points), Theme.secondaryText, width = 34.dp)
            }
            for (row in group.standings) {
                val team = teams[row.teamId]
                val qualifies = showQualifiers && row.rank <= detail.qualifiersPerGroup
                var description = stringResource(R.string.standing_description, row.rank, team?.name.orEmpty(), row.points, row.won, row.lost)
                if (qualifies) description += ", " + stringResource(R.string.qualified)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { navigator.openTeam(row.teamId, detail.season.id) }
                        .clearAndSetSemantics { contentDescription = description }
                        .padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(3.dp).height(22.dp).background(if (qualifies) Theme.qualifier else Color.Transparent, CircleShape))
                        TeamAvatar(team, 24.dp)
                        Text(team?.name ?: "–", color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Stat("${row.played}")
                    Stat("${row.won}")
                    if (showDraws) Stat("${row.tied}")
                    Stat("${row.lost}")
                    Stat(if (row.setDifference > 0) "+${row.setDifference}" else "${row.setDifference}", width = 30.dp)
                    Stat("${row.points}", width = 34.dp, weight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RowScope.Stat(
    text: String,
    color: Color = Color.White,
    width: Dp = 26.dp,
    weight: FontWeight = FontWeight.Normal,
    size: TextUnit = 16.sp,
) {
    Text(text, Modifier.width(width), color = color, fontSize = size, fontWeight = weight, textAlign = TextAlign.End, maxLines = 1)
}
