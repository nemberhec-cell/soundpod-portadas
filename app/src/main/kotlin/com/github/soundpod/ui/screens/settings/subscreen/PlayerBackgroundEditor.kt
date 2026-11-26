package com.github.soundpod.ui.screens.settings.subscreen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.soundpod.R
import com.github.soundpod.customart.CustomArtworkStore
import com.github.soundpod.customart.toastCoverFailed
import com.github.soundpod.ui.appearance.BackgroundStyles
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_PHOTO_KEY
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_STYLE_KEY
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_DIM_KEY
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.screens.player.seekbar.SEEK_BAR_PHOTO_KEY
import com.github.soundpod.ui.screens.settings.SettingsColumn
import com.github.soundpod.ui.screens.settings.SettingsGroup
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Player background editor that lives INSIDE the player: its own photo
 * (back) and the seek-bar dot image. Nothing here touches the main menu
 * or the lists.
 */
@Composable
fun PlayerBackgroundEditor() {
    val context = LocalContext.current
    val prefs = context.preferences
    var customPhotoFile by rememberPreference(PLAYER_BACKGROUND_PHOTO_KEY, "")
    var seekPhotoFile by rememberPreference(SEEK_BAR_PHOTO_KEY, "")
    var dim by rememberPreference(PLAYER_BACKGROUND_DIM_KEY, 55)
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val fileName = withContext(Dispatchers.IO) {
                CustomArtworkStore.copyPickedImage(context, uri, "player_background")
            }
            if (fileName == null) { toastCoverFailed(context); return@launch }
            val previous = customPhotoFile
            customPhotoFile = fileName
            prefs.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.MY_PHOTO) }
            if (previous.isNotBlank() && previous != fileName) {
                withContext(Dispatchers.IO) { CustomArtworkStore.delete(context, previous) }
            }
        }
    }

    val seekPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val fileName = withContext(Dispatchers.IO) {
                CustomArtworkStore.copyPickedImage(context, uri, "seek_bar_dot")
            }
            if (fileName == null) { toastCoverFailed(context); return@launch }
            val previous = seekPhotoFile
            seekPhotoFile = fileName
            if (previous.isNotBlank() && previous != fileName) {
                withContext(Dispatchers.IO) { CustomArtworkStore.delete(context, previous) }
            }
        }
    }

    Column {
        SettingsGroup(
            title = stringResource(id = R.string.player_background)
        ) {
            SettingsColumn(
                icon = IconSource.Vector(Icons.Default.Photo),
                title = stringResource(id = R.string.my_photo),
                description = stringResource(id = R.string.my_photo_description),
                onClick = { photoPicker.launch("image/*") }
            )
            if (customPhotoFile.isNotBlank()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TextButton(onClick = {
                        val previous = customPhotoFile
                        customPhotoFile = ""
                        prefs.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.CROSS_FADE) }
                        scope.launch { withContext(Dispatchers.IO) { CustomArtworkStore.delete(context, previous) } }
                    }) { Text(text = stringResource(id = R.string.remove_photo)) }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text(text = stringResource(id = R.string.background_dim_description))
                    Slider(
                        value = dim.toFloat(),
                        onValueChange = {
                            dim = it.toInt()
                            prefs.edit { putInt(PLAYER_BACKGROUND_DIM_KEY, it.toInt()) }
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(
            title = stringResource(id = R.string.seek_bar_dot_image)
        ) {
            SettingsColumn(
                icon = IconSource.Vector(Icons.Default.Photo),
                title = stringResource(id = R.string.seek_bar_dot_image_photo),
                description = stringResource(id = R.string.seek_bar_dot_image_description),
                onClick = { seekPicker.launch("image/*") }
            )
            if (seekPhotoFile.isNotBlank()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TextButton(onClick = {
                        val previous = seekPhotoFile
                        seekPhotoFile = ""
                        scope.launch { withContext(Dispatchers.IO) { CustomArtworkStore.delete(context, previous) } }
                    }) { Text(text = stringResource(id = R.string.remove_photo)) }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
