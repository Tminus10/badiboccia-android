package swiss.reutener.boccia.ui.team

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import swiss.reutener.boccia.R
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.model.Team
import swiss.reutener.boccia.net.ApiException
import swiss.reutener.boccia.ui.message
import swiss.reutener.boccia.ui.res
import swiss.reutener.boccia.ui.theme.Theme

/**
 * "Foto hinzufügen / ändern" for the logged-in team, like on the website's team page: picks a
 * photo with the system photo picker (no storage permission needed) and uploads it as a square JPEG.
 */
@Composable
fun TeamPhotoPicker(team: Team, modifier: Modifier = Modifier, onUploaded: suspend () -> Unit = {}) {
    val model = LocalAppModel.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ApiException?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isUploading = true
        scope.launch {
            try {
                model.uploadPhoto(uri)
                onUploaded()
            } catch (e: ApiException) {
                error = e
            } finally {
                isUploading = false
            }
        }
    }

    Row(
        modifier
            .clickable(enabled = !isUploading) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isUploading) {
            CircularProgressIndicator(Modifier.size(16.dp), color = Theme.qualifier, strokeWidth = 2.dp)
        } else {
            Icon(Icons.Filled.PhotoCamera, null, Modifier.size(18.dp), tint = Theme.qualifier)
        }
        Text(
            stringResource(
                when {
                    isUploading -> R.string.uploading_photo
                    team.photoUrl != null -> R.string.change_photo
                    else -> R.string.add_photo
                },
            ),
            color = Theme.qualifier,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }

    error?.let {
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(stringResource(R.string.photo_not_saved)) },
            text = { Text(it.message(res())) },
            confirmButton = { TextButton(onClick = { error = null }) { Text(stringResource(R.string.ok)) } },
        )
    }
}
