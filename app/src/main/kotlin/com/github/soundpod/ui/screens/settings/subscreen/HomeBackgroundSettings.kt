package com.github.soundpod.ui.screens.settings.subscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.soundpod.R
import com.github.soundpod.ui.appearance.HOME_BACKGROUND_DIM_KEY
import com.github.soundpod.ui.appearance.HOME_BACKGROUND_PHOTO_KEY
import com.github.soundpod.ui.appearance.removeHomeBackgroundPhoto
import com.github.soundpod.ui.appearance.rememberHomeBackgroundPicker
import com.github.soundpod.ui.screens.settings.SettingsGroup
import com.github.soundpod.ui.screens.settings.SettingsColumn
import com.github.soundpod.ui.common.IconSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.rememberPreference

/**
 * Settings screen to customize the home (main menu) photo background:
 * pick the photo, remove it, or adjust how dimmed it is.
 */
@Composable
fun HomeBackgroundSettingsContent() {
    val context = LocalContext.current
    val prefs = context.preferences
    val pickPhoto = rememberHomeBackgroundPicker()

    var customPhotoFile by rememberPreference(HOME_BACKGROUND_PHOTO_KEY, "")
    var dimPercent by rememberPreference(HOME_BACKGROUND_DIM_KEY, 55)

    Column {
        SettingsGroup(
            title = stringResource(id = R.string.home_background)
        ) {
            SettingsColumn(
                icon = IconSource.Vector(Icons.Default.Photo),
                title = stringResource(id = R.string.home_background_photo),
                description = stringResource(id = R.string.home_background_photo_description),
                onClick = pickPhoto
            )

            if (customPhotoFile.isNotBlank()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TextButton(onClick = {
                        removeHomeBackgroundPhoto(context)
                        customPhotoFile = ""
                    }) {
                        Text(text = stringResource(id = R.string.remove_photo))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(
            title = stringResource(id = R.string.background_dim)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = stringResource(id = R.string.background_dim_description))
                Slider(
                    value = dimPercent.toFloat(),
                    onValueChange = { dimPercent = it.toInt() },
                    onValueChangeFinished = {
                        prefs.edit { putInt(HOME_BACKGROUND_DIM_KEY, dimPercent) }
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
