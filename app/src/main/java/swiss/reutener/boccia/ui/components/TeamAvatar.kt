package swiss.reutener.boccia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import swiss.reutener.boccia.model.Placeholder
import swiss.reutener.boccia.model.Team
import swiss.reutener.boccia.model.initials
import swiss.reutener.boccia.ui.label
import swiss.reutener.boccia.ui.res
import swiss.reutener.boccia.ui.theme.Theme
import swiss.reutener.boccia.ui.theme.colorFromHex

/**
 * A team's badge -- its photo, or its pastel color with initials -- shaped as a squircle like an
 * app icon, with a border in the team's color. A null team (slot not decided yet) shows an empty
 * dimmed squircle.
 */
@Composable
fun TeamAvatar(team: Team?, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    // The app-icon corner ratio; the website's .avatar matches it.
    val shape = RoundedCornerShape(size * 0.225f)
    if (team == null) {
        Box(modifier.size(size).clip(shape).background(Color.White.copy(alpha = 0.08f)).border(0.5.dp, Color.White.copy(alpha = 0.15f), shape))
        return
    }
    val color = colorFromHex(team.colorHex)
    Box(modifier.size(size).clip(shape).background(color), contentAlignment = Alignment.Center) {
        Text(
            team.initials,
            color = Color.Black.copy(alpha = 0.72f),
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        if (team.photoUrl != null) {
            AsyncImage(
                model = team.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
        Box(Modifier.size(size).border(max(1.5.dp, size / 32), color, shape))
    }
}

/** Avatar plus name, or the placeholder text ("Sieger Gruppe A") for an undecided slot. */
@Composable
fun TeamLabel(
    team: Team?,
    modifier: Modifier = Modifier,
    placeholder: Placeholder? = null,
    avatarSize: Dp = 24.dp,
    emphasized: Boolean = false,
    dimmed: Boolean = false,
    fontSize: TextUnit = 16.sp,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        TeamAvatar(team, avatarSize)
        Text(
            team?.name ?: placeholder?.label(res()) ?: "–",
            color = if (team == null || dimmed) Theme.secondaryText else Color.White,
            fontSize = fontSize,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
