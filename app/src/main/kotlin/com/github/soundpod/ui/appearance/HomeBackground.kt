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
 * Step 1 (v2) of the user's personalization plan: a photo background for the
 * main (home) screen, in two layers:
 *
 *  - [HOME_BACKGROUND_PHOTO_KEY] ("back layer", the app's dark background
 *    around the content card), and
 *  - [HOME_FRONT_PHOTO_KEY] ("front layer", the content card itself).
 *
 * Each layer has its own dim preference (0..100, default 55) that blends the
 * app background color over the photo so text stays readable.
 */
const val HOME_BACKGROUND_PHOTO_KEY = "home_background_photo_file"
const val HOME_BACKGROUND_DIM_KEY = "home_background_dim"
const val HOME_FRONT_PHOTO_KEY = "home_front_photo_file"
const val HOME_FRONT_DIM_KEY = "home_front_dim"

/**
 * List detail screens (playlists, albums, artists): their own back/front
 * photo layers, independent from the home screen ones.
 */
const val LIST_BACKGROUND_PHOTO_KEY = "list_background_photo_file"
const val LIST_BACKGROUND_DIM_KEY = "list_background_dim"
const val LIST_FRONT_PHOTO_KEY = "list_front_photo_file"
const val LIST_FRONT_DIM_KEY = "list_front_dim"

private const val HOME_BACKGROUND_ITEM_KEY = "home_background"
private const val HOME_FRONT_ITEM_KEY = "home_front"
private const val LIST_BACKGROUND_ITEM_KEY = "list_background"
private const val LIST_FRONT_ITEM_KEY = "list_front"

/** Wallpaper chooser state for one layer. */
class HomePhotoPicker(
    val photo: String,
    val pick: () -> Unit,
    val remove: () -> Unit,
    val dim: Int,
    val setDim: (Int) -> Unit
)

/**
 * Full-screen photo background behind the home screen content (back layer).
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
 * Photo shown inside the front content card of the home screen.
 * Returns the photo file name, or null when the layer is unset.
 */
@Composable
fun rememberHomeFrontPhoto(): String? {
    val customPhotoFile by rememberPreference(HOME_FRONT_PHOTO_KEY, "")
    val context = LocalContext.current
    val file = customPhotoFile.takeIf { it.isNotBlank() }?.let {
        CustomArtworkStore.fileFor(context, it)
    }
    return if (file != null && file.exists()) customPhotoFile else null
}

/**
 * The back-layer photo file name, or null when unset (or file missing).
 * Also used to decide whether the home Scaffold should be transparent
 * so the photo is actually visible behind it.
 */
@Composable
fun rememberHomeBackgroundPhotoFile(): String? {
    val customPhotoFile by rememberPreference(HOME_BACKGROUND_PHOTO_KEY, "")
    val context = LocalContext.current
    val file = customPhotoFile.takeIf { it.isNotBlank() }?.let {
        CustomArtworkStore.fileFor(context, it)
    }
    return if (file != null && file.exists()) customPhotoFile else null
}

/** Dim used over the front photo layer. */
@Composable
fun rememberHomeFrontDim(): Int {
    val dimPercent by rememberPreference(HOME_FRONT_DIM_KEY, 55)
    return dimPercent.coerceIn(0, 100)
}

/**
 * Full-screen photo background behind list detail screens (back layer).
 */
@Composable
fun ListPhotoBackground() {
    val customPhotoFile by rememberPreference(LIST_BACKGROUND_PHOTO_KEY, "")
    if (customPhotoFile.isBlank()) return
    val dimPercent by rememberPreference(LIST_BACKGROUND_DIM_KEY, 55)
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
 * Per-list background storage: every list (playlist, album, artist) keeps
 * its own back + front photos, keyed by its id. Two lists that share a
 * name are still different ids, so their backgrounds never collide.
 */
private fun listBackKey(listKey: String) = "$listKey:back_photo"
private fun listBackDimKey(listKey: String) = "$listKey:back_dim"
private fun listFrontKey(listKey: String) = "$listKey:front_photo"
private fun listFrontDimKey(listKey: String) = "$listKey:front_dim"

/** The back-layer photo file name for [listKey], or null when unset (or file missing). */
@Composable
fun rememberListBackgroundPhotoFileFor(listKey: String?): String? {
    if (listKey == null) return null
    val customPhotoFile by rememberPreference(listBackKey(listKey), "")
    val context = LocalContext.current
    val file = customPhotoFile.takeIf { it.isNotBlank() }?.let {
        CustomArtworkStore.fileFor(context, it)
    }
    return if (file != null && file.exists()) customPhotoFile else null
}

/** The front-layer photo file name for [listKey], or null when unset (or file missing). */
@Composable
fun rememberListFrontPhotoFor(listKey: String?): String? {
    if (listKey == null) return null
    val customPhotoFile by rememberPreference(listFrontKey(listKey), "")
    val context = LocalContext.current
    val file = customPhotoFile.takeIf { it.isNotBlank() }?.let {
        CustomArtworkStore.fileFor(context, it)
    }
    return if (file != null && file.exists()) customPhotoFile else null
}

/** Dim used over the front photo layer of [listKey]. */
@Composable
fun rememberListFrontDimFor(listKey: String?): Int {
    if (listKey == null) return 55
    val dimPercent by rememberPreference(listFrontDimKey(listKey), 55)
    return dimPercent.coerceIn(0, 100)
}

/** Picker + dim controls for the back layer of the list identified by [listKey]. */
@Composable
fun rememberListBackgroundPickerFor(listKey: String?): HomePhotoPicker? {
    if (listKey == null) return null
    val pick = rememberPhotoPickerFor(listBackKey(listKey), listKey + "_back")
    val photo by rememberPreference(listBackKey(listKey), "")
    var dim by rememberPreference(listBackDimKey(listKey), 55)
    val context = LocalContext.current
    return HomePhotoPicker(
        photo = photo,
        pick = pick,
        remove = { removePhotoFor(context, listBackKey(listKey)) },
        dim = dim,
        setDim = {
            dim = it
            context.preferences.edit { putInt(listBackDimKey(listKey), it) }
        }
    )
}

/** Picker + dim controls for the front layer of the list identified by [listKey]. */
@Composable
fun rememberListFrontPickerFor(listKey: String?): HomePhotoPicker? {
    if (listKey == null) return null
    val pick = rememberPhotoPickerFor(listFrontKey(listKey), listKey + "_front")
    val photo by rememberPreference(listFrontKey(listKey), "")
    var dim by rememberPreference(listFrontDimKey(listKey), 55)
    val context = LocalContext.current
    return HomePhotoPicker(
        photo = photo,
        pick = pick,
        remove = { removePhotoFor(context, listFrontKey(listKey)) },
        dim = dim,
        setDim = {
            dim = it
            context.preferences.edit { putInt(listFrontDimKey(listKey), it) }
        }
    )
}

/**
 * Full-screen back-layer background for the list identified by [listKey]
 * (its own photo, independent from the menu and from other lists).
 */
@Composable
fun ListPhotoBackgroundFor(listKey: String?) {
    val customPhotoFile = rememberListBackgroundPhotoFileFor(listKey)
    if (customPhotoFile.isNullOrBlank()) return
    val dimPercent by rememberPreference(listBackDimKey(listKey.toString()), 55)
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

/** The list back-layer photo file name, or null when unset (or file missing). */
@Composable
fun rememberListBackgroundPhotoFile(): String? {
    val customPhotoFile by rememberPreference(LIST_BACKGROUND_PHOTO_KEY, "")
    val context = LocalContext.current
    val file = customPhotoFile.takeIf { it.isNotBlank() }?.let {
        CustomArtworkStore.fileFor(context, it)
    }
    return if (file != null && file.exists()) customPhotoFile else null
}

/** The list front-layer photo file name, or null when unset (or file missing). */
@Composable
fun rememberListFrontPhoto(): String? {
    val customPhotoFile by rememberPreference(LIST_FRONT_PHOTO_KEY, "")
    val context = LocalContext.current
    val file = customPhotoFile.takeIf { it.isNotBlank() }?.let {
        CustomArtworkStore.fileFor(context, it)
    }
    return if (file != null && file.exists()) customPhotoFile else null
}

/** Dim used over the list front photo layer. */
@Composable
fun rememberListFrontDim(): Int {
    val dimPercent by rememberPreference(LIST_FRONT_DIM_KEY, 55)
    return dimPercent.coerceIn(0, 100)
}

/** Picker + dim controls for the list back layer. */
@Composable
fun rememberListBackgroundPicker(): HomePhotoPicker {
    val pick = rememberPhotoPickerFor(LIST_BACKGROUND_PHOTO_KEY, LIST_BACKGROUND_ITEM_KEY)
    val photo by rememberPreference(LIST_BACKGROUND_PHOTO_KEY, "")
    var dim by rememberPreference(LIST_BACKGROUND_DIM_KEY, 55)
    val context = LocalContext.current
    return HomePhotoPicker(
        photo = photo,
        pick = pick,
        remove = { removePhotoFor(context, LIST_BACKGROUND_PHOTO_KEY) },
        dim = dim,
        setDim = {
            dim = it
            context.preferences.edit { putInt(LIST_BACKGROUND_DIM_KEY, it) }
        }
    )
}

/** Picker + dim controls for the list front layer. */
@Composable
fun rememberListFrontPicker(): HomePhotoPicker {
    val pick = rememberPhotoPickerFor(LIST_FRONT_PHOTO_KEY, LIST_FRONT_ITEM_KEY)
    val photo by rememberPreference(LIST_FRONT_PHOTO_KEY, "")
    var dim by rememberPreference(LIST_FRONT_DIM_KEY, 55)
    val context = LocalContext.current
    return HomePhotoPicker(
        photo = photo,
        pick = pick,
        remove = { removePhotoFor(context, LIST_FRONT_PHOTO_KEY) },
        dim = dim,
        setDim = {
            dim = it
            context.preferences.edit { putInt(LIST_FRONT_DIM_KEY, it) }
        }
    )
}

/**
 * Opens the system photo picker and saves the chosen image under the given
 * preference key, replacing (and deleting) the previous one.
 * Call the returned lambda from a click handler.
 */
@Composable
private fun rememberPhotoPickerFor(prefKey: String, itemKey: String): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentFile by rememberPreference(prefKey, "")

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val fileName = withContext(Dispatchers.IO) {
                CustomArtworkStore.copyPickedImage(context, uri, itemKey)
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

    return { launcher.launch(arrayOf("image/*", "image/gif", "image/webp")) }
}

/** Removes the photo stored under [prefKey]. */
fun removePhotoFor(context: android.content.Context, prefKey: String) {
    val prefs = context.preferences
    val previous = prefs.getString(prefKey, "") ?: ""
    prefs.edit { putString(prefKey, "") }
    if (previous.isNotBlank()) {
        CustomArtworkStore.delete(context, previous)
    }
}

/** Picker + dim controls for the back (main menu) layer. */
@Composable
fun rememberHomeBackgroundPicker(): HomePhotoPicker {
    val pick = rememberPhotoPickerFor(HOME_BACKGROUND_PHOTO_KEY, HOME_BACKGROUND_ITEM_KEY)
    val photo by rememberPreference(HOME_BACKGROUND_PHOTO_KEY, "")
    var dim by rememberPreference(HOME_BACKGROUND_DIM_KEY, 55)
    val context = LocalContext.current
    return HomePhotoPicker(
        photo = photo,
        pick = pick,
        remove = {
            removePhotoFor(context, HOME_BACKGROUND_PHOTO_KEY)
        },
        dim = dim,
        setDim = {
            dim = it
            context.preferences.edit { putInt(HOME_BACKGROUND_DIM_KEY, it) }
        }
    )
}

/** Picker + dim controls for the front (content card) layer. */
@Composable
fun rememberHomeFrontPicker(): HomePhotoPicker {
    val pick = rememberPhotoPickerFor(HOME_FRONT_PHOTO_KEY, HOME_FRONT_ITEM_KEY)
    val photo by rememberPreference(HOME_FRONT_PHOTO_KEY, "")
    var dim by rememberPreference(HOME_FRONT_DIM_KEY, 55)
    val context = LocalContext.current
    return HomePhotoPicker(
        photo = photo,
        pick = pick,
        remove = {
            removePhotoFor(context, HOME_FRONT_PHOTO_KEY)
        },
        dim = dim,
        setDim = {
            dim = it
            context.preferences.edit { putInt(HOME_FRONT_DIM_KEY, it) }
        }
    )
}
