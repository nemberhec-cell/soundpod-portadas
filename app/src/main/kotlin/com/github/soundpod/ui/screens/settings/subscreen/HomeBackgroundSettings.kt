package com.github.soundpod.ui.screens.settings.subscreen

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.soundpod.R
import com.github.soundpod.ui.appearance.HomePhotoPicker
import com.github.soundpod.ui.appearance.rememberHomeBackgroundPicker
import com.github.soundpod.ui.appearance.rememberHomeFrontPicker
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.screens.settings.SettingsColumn
import com.github.soundpod.ui.screens.settings.SettingsGroup

/**
 * Settings screen to customize the home (main menu) photo backgrounds:
 * a back layer (the dark background around everything) and a front layer
 * (the grey content card). Each layer can pick its own photo, remove it,
 * or adjust how dimmed it is.
 */
@Composable
fun HomeBackgroundSettingsContent() {
    val backPicker = rememberHomeBackgroundPicker()
    val frontPicker = rememberHomeFrontPicker()

    Column {
        SettingsGroup(
            title = stringResource(id = R.string.home_background)
        ) {
            LayerSettings(
                title = stringResource(id = R.string.home_back_layer),
                description = stringResource(id = R.string.home_back_layer_description),
                picker = backPicker
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(
            title = stringResource(id = R.string.home_front_layer)
        ) {
            LayerSettings(
                title = stringResource(id = R.string.home_front_layer_photo),
                description = stringResource(id = R.string.home_front_layer_description),
                picker = frontPicker
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun LayerSettings(
    title: String,
    description: String,
    picker: HomePhotoPicker
) {
    SettingsColumn(
        icon = IconSource.Vector(Icons.Default.Photo),
        title = title,
        description = description,
        onClick = picker.pick
    )

    if (picker.photo.isNotBlank()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            TextButton(onClick = picker.remove) {
                Text(text = stringResource(id = R.string.remove_photo))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(text = stringResource(id = R.string.background_dim_description))
            Slider(
                value = picker.dim.toFloat(),
                onValueChange = { picker.setDim(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
