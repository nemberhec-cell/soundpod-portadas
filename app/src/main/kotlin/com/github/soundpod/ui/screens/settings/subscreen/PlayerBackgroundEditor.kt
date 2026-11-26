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
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.customart.CustomArtworkStore
import com.github.soundpod.customart.hasCustomArtwork
import com.github.soundpod.customart.rememberCustomArtworkFileName
import com.github.soundpod.customart.removeCustomArtwork
import com.github.soundpod.customart.toastCoverFailed
import com.github.soundpod.db
import com.github.soundpod.query
import com.github.soundpod.customart.CustomArtwork
import com.github.soundpod.ui.appearance.BackgroundStyles
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_STYLE_KEY
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_DIM_KEY
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.screens.settings.SettingsColumn
import com.github.soundpod.ui.screens.settings.SettingsGroup
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Per-song player editor (inside the player):
 *  - background photo/GIF **only for the current song** (playerbg:<mediaId>)
 *  - seek-dot photo/GIF **only for the current song** (seekdot:<mediaId>)
 * Each song is independent: changing song shows its own photos, or none.
 */
@Composable
fun PlayerBackgroundEditor() {
    val context = LocalContext.current
    val prefs = context.preferences
    val binder = LocalPlayerServiceBinder.current
    val currentMediaId = binder?.player?.currentMediaItem?.mediaId
        ?: return
    val bgKey = "playerbg:$currentMediaId"
    val dotKey = "seekdot:$currentMediaId"

    val bgFileName by rememberCustomArtworkFileName(bgKey)
    val dotFileName by rememberCustomArtworkFileName(dotKey)
    var dim by rememberPreference(PLAYER_BACKGROUND_DIM_KEY, 55)
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val fileName = withContext(Dispatchers.IO) {
                CustomArtworkStore.copyPickedImage(context, uri, bgKey)
            }
            if (fileName == null) { toastCoverFailed(context); return@launch }
            query {
                val previous = db.getCustomArtwork(bgKey)
                db.upsert(CustomArtwork(itemKey = bgKey, fileName = fileName))
                if (previous != null && previous.fileName != fileName) {
                    CustomArtworkStore.delete(context, previous.fileName)
                }
            }
            prefs.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.MY_PHOTO) }
        }
    }

    val seekPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val fileName = withContext(Dispatchers.IO) {
                CustomArtworkStore.copyPickedImage(context, uri, dotKey)
            }
            if (fileName == null) { toastCoverFailed(context); return@launch }
            query {
                val previous = db.getCustomArtwork(dotKey)
                db.upsert(CustomArtwork(itemKey = dotKey, fileName = fileName))
                if (previous != null && previous.fileName != fileName) {
                    CustomArtworkStore.delete(context, previous.fileName)
                }
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
            if (!bgFileName.isNullOrBlank()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TextButton(onClick = {
                        removeCustomArtwork(context, bgKey)
                        prefs.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.CROSS_FADE) }
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
            if (!dotFileName.isNullOrBlank()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TextButton(onClick = {
                        removeCustomArtwork(context, dotKey)
                    }) { Text(text = stringResource(id = R.string.remove_photo)) }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
