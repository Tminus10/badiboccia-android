package swiss.reutener.boccia.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.net.ServerEnvironment
import swiss.reutener.boccia.ui.components.DetailScaffold
import swiss.reutener.boccia.ui.components.InfoCard
import swiss.reutener.boccia.ui.components.InfoRow
import swiss.reutener.boccia.ui.res
import swiss.reutener.boccia.ui.theme.Theme
import swiss.reutener.boccia.ui.timeText
import java.time.ZoneId

/** Server (debug builds only), links to the website, and the app version. */
@Composable
fun SettingsScreen() {
    val model = LocalAppModel.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    // The website opens in the app's language.
    val language = res().configuration.locales[0].language.takeIf { it == "en" } ?: "de"

    DetailScaffold(stringResource(R.string.settings)) {
        if (ServerEnvironment.selectable.size > 1) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCard {
                    ServerEnvironment.selectable.forEachIndexed { index, server ->
                        if (index > 0) HorizontalDivider(Modifier.padding(start = 56.dp), color = Theme.separator)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(server == model.server, role = Role.RadioButton) { model.switchServer(server) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = server == model.server,
                                onClick = null,
                                Modifier.padding(12.dp),
                                colors = RadioButtonDefaults.colors(selectedColor = Theme.qualifier),
                            )
                            Text(stringResource(server.title), color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
                Note(stringResource(R.string.dev_only_note))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoCard {
                Link(Icons.Outlined.Public, stringResource(R.string.open_website)) { uriHandler.openUri(model.server.websitePage(language = language)) }
                HorizontalDivider(Modifier.padding(start = 56.dp), color = Theme.separator)
                Link(Icons.AutoMirrored.Outlined.MenuBook, stringResource(R.string.rules)) { uriHandler.openUri(model.server.websitePage("regeln", language)) }
                HorizontalDivider(Modifier.padding(start = 56.dp), color = Theme.separator)
                Link(Icons.Outlined.EventAvailable, stringResource(R.string.subscribe_calendar)) {
                    uriHandler.openUri(model.server.websitePage("calendar", language))
                }
            }
            Note(stringResource(R.string.admins_note))
        }

        InfoCard {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            InfoRow(stringResource(R.string.version), "${info.versionName} (${info.longVersionCode})", first = true)
            model.lastUpdated?.let {
                InfoRow(stringResource(R.string.last_updated), it.atZone(ZoneId.systemDefault()).timeText())
            }
        }
    }
}

@Composable
private fun Link(icon: ImageVector, title: String, open: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = open).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = Theme.qualifier)
        Text(title, color = Theme.qualifier, fontSize = 16.sp)
    }
}

@Composable
private fun Note(text: String) {
    Text(text, Modifier.padding(horizontal = 16.dp), color = Theme.secondaryText, fontSize = 13.sp)
}
