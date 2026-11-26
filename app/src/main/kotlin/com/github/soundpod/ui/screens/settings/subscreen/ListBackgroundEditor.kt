package com.github.soundpod.ui.screens.settings.subscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.soundpod.R
import com.github.soundpod.ui.appearance.HomePhotoPicker
import com.github.soundpod.ui.appearance.rememberListBackgroundPickerFor
import com.github.soundpod.ui.appearance.rememberListFrontPickerFor
import com.github.soundpod.ui.screens.settings.SettingsGroup

/**
 * Background editor that lives INSIDE each list: its back and front layers
 * are stored under that list's own id, so every list keeps its own photos
 * and its own dim levels — independent from the menu and from other lists.
 */
@Composable
fun ListBackgroundEditor(listKey: String?) {
    val backPicker: HomePhotoPicker? = rememberListBackgroundPickerFor(listKey)
    val frontPicker: HomePhotoPicker? = rememberListFrontPickerFor(listKey)

    Column {
        SettingsGroup(
            title = stringResource(id = R.string.list_background)
        ) {
            backPicker?.let { picker ->
                LayerSettings(
                    title = stringResource(id = R.string.list_back_layer),
                    description = stringResource(id = R.string.list_back_layer_description),
                    picker = picker
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(
            title = stringResource(id = R.string.list_front_layer)
        ) {
            frontPicker?.let { picker ->
                LayerSettings(
                    title = stringResource(id = R.string.list_front_layer_photo),
                    description = stringResource(id = R.string.list_front_layer_description),
                    picker = picker
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
