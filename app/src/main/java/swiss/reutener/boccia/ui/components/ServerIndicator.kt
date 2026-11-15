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
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.AppModel
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.ui.theme.Theme

// Marks a session that isn't the real league -- a test server (DEV or local) or a logged-in demo
// team -- so nobody enters a result in the wrong place: a line along the top of every screen
// (`ServerIndicatorLine`) and a tag next to the title (`ServerBadge`). Nothing is shown for a real
// team on the live site.

/** The test server wins over the demo: which website you're on matters more. */
private val AppModel.indicatorColor: Color?
    get() = when {
        server.badge != null -> Theme.testServer
        isDemoSession -> Theme.demo
        else -> null
    }

@Composable
fun ServerBadge(modifier: Modifier = Modifier) {
    val model = LocalAppModel.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        model.server.badge?.let { Tag(it, Theme.testServer, stringResource(model.server.title)) }
        if (model.isDemoSession) Tag("DEMO", Theme.demo, stringResource(R.string.demo_mode))
    }
}

@Composable
private fun Tag(text: String, color: Color, description: String) {
    Text(
        text,
        Modifier
            .background(color, CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { contentDescription = description },
        color = Color.Black,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
    )
}

/** A solid line with a glow fading down over the status bar: a bare thin line was easy to miss. */
@Composable
fun BoxScope.ServerIndicatorLine() {
    val color = LocalAppModel.current.indicatorColor ?: return
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(6.dp).background(color))
        Box(Modifier.fillMaxWidth().height(44.dp).background(Brush.verticalGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent))))
    }
}
