package swiss.reutener.boccia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.ui.message
import swiss.reutener.boccia.ui.res
import swiss.reutener.boccia.ui.theme.Theme

/**
 * The capsule with the tournament name, like "FIFA World Cup 2026" in the Sports app; opens a menu
 * of every season, so past seasons (the website's archive) are one tap away.
 */
@Composable
fun SeasonPicker(modifier: Modifier = Modifier) {
    val model = LocalAppModel.current
    var expanded by remember { mutableStateOf(false) }
    val label = model.selectedSeason?.label ?: stringResource(R.string.season)
    val chooseSeason = stringResource(R.string.choose_season)
    Box(modifier) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(Theme.backgroundTop.copy(alpha = 0.75f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable(role = Role.DropdownList) { expanded = true }
                .semantics {
                    contentDescription = chooseSeason
                    stateDescription = label
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.EmojiEvents, null, Modifier.size(20.dp), tint = Theme.qualifier)
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(20.dp), tint = Theme.secondaryText)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            for (season in model.seasons) {
                DropdownMenuItem(
                    text = {
                        Text(season.label, fontWeight = if (season.id == model.selectedSeasonId) FontWeight.Bold else FontWeight.Normal)
                    },
                    leadingIcon = when {
                        season.isDemo -> ({ Icon(Icons.Filled.AutoAwesome, null) })
                        season.isCurrent -> ({ Icon(Icons.Filled.Star, null) })
                        else -> null
                    },
                    onClick = {
                        expanded = false
                        model.selectSeason(season.id)
                    },
                )
            }
        }
    }
}

/**
 * Banner for seasons that aren't running: archived ones are read-only, like on the website, and
 * upcoming ones haven't started yet. The demo season is neither.
 */
@Composable
fun ArchivedSeasonNote(modifier: Modifier = Modifier) {
    val season = LocalAppModel.current.selectedSeason ?: return
    if (season.isCurrent || season.isDemo) return
    val (icon, text) = if (season.isUpcoming) {
        Icons.Outlined.Event to R.string.upcoming_note
    } else {
        Icons.Outlined.Archive to R.string.archived_note
    }
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = Theme.secondaryText)
        Text(stringResource(text), color = Theme.secondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Error or loading state for a screen whose season data isn't there yet. */
@Composable
fun SeasonLoadingState(modifier: Modifier = Modifier) {
    val model = LocalAppModel.current
    Column(
        modifier.fillMaxWidth().heightIn(min = 300.dp).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val error = model.lastError
        if (error != null) {
            Icon(Icons.Outlined.CloudOff, null, Modifier.size(40.dp), tint = Theme.secondaryText)
            Text(error.message(res()), color = Theme.secondaryText, textAlign = TextAlign.Center)
            FilledTonalButton(onClick = { model.refresh() }) {
                Text(stringResource(R.string.try_again))
            }
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
