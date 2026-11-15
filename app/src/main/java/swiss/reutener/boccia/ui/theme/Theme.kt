package swiss.reutener.boccia.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The Sports-app-like look of the iOS app: a deep blue gradient behind translucent dark cards. Dark only. */
object Theme {
    val backgroundTop = Color(0.17f, 0.24f, 0.62f)
    val backgroundBottom = Color(0.03f, 0.07f, 0.24f)
    val card = Color(0.03f, 0.07f, 0.27f).copy(alpha = 0.62f)
    val cardHeader = Color.White.copy(alpha = 0.04f)
    val cardBorder = Color.White.copy(alpha = 0.06f)
    val separator = Color.White.copy(alpha = 0.09f)
    val secondaryText = Color.White.copy(alpha = 0.58f)
    val tertiaryText = Color.White.copy(alpha = 0.38f)

    /** Marks teams that qualify for the knockout phase; also the app's accent color. */
    val qualifier = Color(1.0f, 0.85f, 0.4f)
    val testServer = Color(0xFFFF9500)
    val demo = Color(0.38f, 0.86f, 0.66f)
    val won = Color(0xFF34C759)
    val lost = Color(0xFFFF3B30)
    val cornerRadius = 22.dp

    val backgroundBrush = Brush.verticalGradient(listOf(backgroundTop, backgroundBottom))
}

@Composable
fun BocciaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Theme.qualifier,
            onPrimary = Color.Black,
            secondaryContainer = Color.White.copy(alpha = 0.14f),
            onSecondaryContainer = Color.White,
            background = Theme.backgroundBottom,
            surface = Theme.backgroundBottom,
            surfaceContainer = Color(0xFF16205A),
            surfaceContainerHigh = Color(0xFF1C2766),
            onSurface = Color.White,
            onSurfaceVariant = Theme.secondaryText,
            outlineVariant = Theme.separator,
        ),
        content = content,
    )
}

/** The gradient behind every screen, edge to edge (also behind the status and navigation bars). */
@Composable
fun ThemedScreen(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Theme.backgroundBrush), content = content)
}

/** The rounded dark card the Sports app puts every section in. */
fun Modifier.card(cornerRadius: Dp = Theme.cornerRadius): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return clip(shape).background(Theme.card).border(1.dp, Theme.cardBorder, shape)
}

/** Small section heading inside cards ("Gruppe A", "PKT"). */
@Composable
fun CardHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = Theme.secondaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

/** Centered placeholder for "nothing here yet". */
@Composable
fun EmptyStateText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
        color = Theme.secondaryText,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
}

/** "#96e48b" -> Color; gray for anything unparsable. */
fun colorFromHex(hex: String?): Color {
    val digits = hex?.trimStart('#') ?: return Color.Gray
    val value = digits.takeIf { it.length == 6 }?.toLongOrNull(16) ?: return Color.Gray
    return Color(0xFF000000 or value)
}
