package com.github.soundpod.ui.appearance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.github.core.ui.LocalAppearance
import com.github.soundpod.customart.CustomArtworkStore
import com.github.soundpod.customart.CustomPhotoBackground
import com.github.soundpod.customart.toastCoverFailed
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Step 1 of the user's personalization plan: a photo background for the
 * main (home) screen, chosen from the phone gallery.
 *
 * The chosen photo is stored with [HOME_BACKGROUND_PHOTO_KEY]; the
 * [HOME_BACKGROUND_DIM_KEY] preference (0..100, default 55) sets how much
 * the app's own background color is blended over the photo so the text
 * and cards stay readable.
 */
const val HOME_BACKGROUND_PHOTO_KEY = "home_background_photo_file"
const val HOME_BACKGROUND_DIM_KEY = "home_background_dim"

private const val HOME_BACKGROUND_ITEM_KEY = "home_background"

/**
 * Full-screen photo background behind the home screen content.
 * Does nothing when no photo has been chosen (or the file is gone).
 */
@Composable
fun HomePhotoBackground() {
    val customPhotoFile by rememberPreference(HOME_BACKGROUND_PHOTO_KEY, "")
    if (customPhotoFile.isBlank()) return
    val dimPercent by rememberPreference(HOME_BACKGROUND_DIM_KEY, 55)
    val baseBackground = LocalAppearance.current.colorPalette.background0
    val scrimAlpha = dimPercent.coerceIn(0, 100) / 100f

    CustomPhotoBackground(
        fileName = customPhotoFile,
        modifier = Modifier.fillMaxSize()
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to baseBackground.copy(alpha = scrimAlpha),
                    0.35f to baseBackground.copy(alpha = scrimAlpha * 0.8f),
                    1f to baseBackground.copy(alpha = scrimAlpha)
                )
            )
    )
}

/**
 * Opens the system photo picker and saves the chosen image as the home
 * background, replacing (and deleting) the previous one.
 * Call the returned lambda from a click handler.
 */
@Composable
fun rememberHomeBackgroundPicker(): () -> Unit {
    val context = LocalContext.current
    val prefs = context.preferences
    val scope = rememberCoroutineScope()
    var currentFile by rememberPreference(HOME_BACKGROUND_PHOTO_KEY, "")

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val fileName = withContext(Dispatchers.IO) {
                CustomArtworkStore.copyPickedImage(context, uri, HOME_BACKGROUND_ITEM_KEY)
            }
            if (fileName == null) {
                toastCoverFailed(context)
                return@launch
            }
            val previous = currentFile
            currentFile = fileName
            if (previous.isNotBlank() && previous != fileName) {
                withContext(Dispatchers.IO) { CustomArtworkStore.delete(context, previous) }
            }
        }
    }

    return { launcher.launch("image/*") }
}

/** Removes the home background photo. */
fun removeHomeBackgroundPhoto(context: android.content.Context) {
    val prefs = context.preferences
    val previous = prefs.getString(HOME_BACKGROUND_PHOTO_KEY, "") ?: ""
    prefs.edit { putString(HOME_BACKGROUND_PHOTO_KEY, "") }
    if (previous.isNotBlank()) {
        CustomArtworkStore.delete(context, previous)
    }
}
