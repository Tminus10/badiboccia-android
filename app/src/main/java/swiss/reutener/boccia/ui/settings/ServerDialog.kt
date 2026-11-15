package swiss.reutener.boccia.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.net.ServerEnvironment

/** Debug builds only: which website the app talks to. */
@Composable
fun ServerDialog(onDismiss: () -> Unit) {
    val model = LocalAppModel.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.server)) },
        text = {
            Column {
                for (server in ServerEnvironment.selectable) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(server == model.server, role = Role.RadioButton) {
                                model.switchServer(server)
                                onDismiss()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = server == model.server, onClick = null)
                        Text(stringResource(server.title), Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}
