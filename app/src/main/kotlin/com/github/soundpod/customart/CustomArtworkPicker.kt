package com.github.soundpod.customart

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.soundpod.db
import com.github.soundpod.query
import com.github.soundpod.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * In-app photo picker flow, 100% from inside the app:
 * 1. opens the system photo picker (no storage permission needed),
 * 2. copies the chosen photo into private storage (filesDir/custom_artwork),
 * 3. saves itemKey -> photo in the database.
 * [onDone] runs after saving so the UI can refresh.
 */
@Composable
fun rememberCustomArtworkPicker(
    onDone: () -> Unit = {},
    onError: () -> Unit = {}
): (itemKey: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingKey by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val itemKey = pendingKey
        pendingKey = null
        if (uri == null || itemKey == null) return@rememberLauncherForActivityResult
        scope.launch {
            val fileName = withContext(Dispatchers.IO) {
                CustomArtworkStore.copyPickedImage(context, uri, itemKey)
            }
            if (fileName == null) {
                onError()
                return@launch
            }
            query {
                val previous = db.getCustomArtwork(itemKey)
                db.upsert(CustomArtwork(itemKey = itemKey, fileName = fileName))
                if (previous != null && previous.fileName != fileName) {
                    CustomArtworkStore.delete(context, previous.fileName)
                }
            }
            onDone()
        }
    }

    return remember(launcher) {
        { itemKey: String ->
            pendingKey = itemKey
            launcher.launch("image/*")
        }
    }
}

/** Removes the custom artwork for [itemKey] and deletes the copied file. */
fun removeCustomArtwork(context: android.content.Context, itemKey: String, onDone: () -> Unit = {}) {
    query {
        val existing = db.getCustomArtwork(itemKey)
        db.deleteCustomArtwork(itemKey)
        if (existing != null) CustomArtworkStore.delete(context, existing.fileName)
    }
    onDone()
}

fun artworkFile(context: android.content.Context, fileName: String?): File? =
    fileName?.let { CustomArtworkStore.fileFor(context, it) }

fun artworkUriFor(context: android.content.Context, fileName: String?): Uri? =
    artworkFile(context, fileName)?.toUri()

fun toastCoverUpdated(context: android.content.Context) =
    context.toast(context.getString(com.github.soundpod.R.string.cover_updated))

fun toastCoverFailed(context: android.content.Context) =
    context.toast(context.getString(com.github.soundpod.R.string.cover_failed))
