package swiss.reutener.boccia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.ui.theme.Theme

// Marks a session that isn't the real league -- a test server (DEV or local) -- so nobody enters a
// result in the wrong place: a line along the top of the screen (`ServerIndicatorLine`) and a tag
// next to the title (`ServerBadge`). Nothing is shown on the live site.

/** The test-server tag next to the title. */
@Composable
fun ServerBadge(modifier: Modifier = Modifier) {
    val server = LocalAppModel.current.server
    val badge = server.badge ?: return
    val description = stringResource(server.title)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            badge,
            Modifier
                .background(Theme.testServer, CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .semantics { contentDescription = description },
            color = Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

/** A solid line with a glow fading down over the status bar: a bare thin line was easy to miss. */
@Composable
fun BoxScope.ServerIndicatorLine() {
    if (LocalAppModel.current.server.badge == null) return
    val color = Theme.testServer
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(6.dp).background(color))
        Box(Modifier.fillMaxWidth().height(44.dp).background(Brush.verticalGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent))))
    }
}
