package com.github.soundpod.customart

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.utils.thumbnail
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Builds a Coil request that plays animated images (GIF / WebP animado).
 * Hardware bitmaps are disabled for animated images: they only show the
 * first frame. Static photos keep hardware acceleration.
 */
fun animatedImageRequest(context: android.content.Context, model: Any?, sizePx: Int? = null): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(model)
        .allowHardware(true)
    if (sizePx != null && sizePx > 0) {
        builder.size(sizePx)
    }
    return builder.build()
}

fun isAnimatedImageFile(file: File?): Boolean {
    if (file == null || !file.exists()) return false
    return file.extension.lowercase() in setOf("gif", "webp")
}

/**
 * The effective artwork model for [itemKey]: the user's photo when one is set,
 * otherwise [fallbackUrl] (the original YouTube Music thumbnail).
 *
 * This is the single chokepoint every screen should use, so a custom cover
 * shows up everywhere: player, lists, album/artist headers.
 *
 * Local files are passed to Coil as a plain [java.io.File] path — Coil handles
 * them natively, no conversion to Uri needed.
 */
@Composable
fun effectiveArtwork(
    itemKey: String?,
    fallbackUrl: String?,
    sizePx: Int = 1024
): Any? {
    val context = LocalContext.current
    val customFileName by rememberCustomArtworkFileName(itemKey)
    if (itemKey != null && customFileName != null) {
        val file = CustomArtworkStore.fileFor(context, customFileName!!)
        if (file.exists()) return file
    }
    return fallbackUrl?.thumbnail(sizePx)
}

@Composable
fun rememberCustomArtworkFileName(itemKey: String?): androidx.compose.runtime.State<String?> {
    return if (itemKey == null) {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) }
    } else {
        db.customArtworkFileName(itemKey)
            .collectAsState(initial = null, context = Dispatchers.IO)
    }
}

/** Whether [itemKey] currently has a custom cover (for showing "Reset cover"). */
@Composable
fun hasCustomArtwork(itemKey: String?): Boolean {
    val fileName by rememberCustomArtworkFileName(itemKey)
    return fileName != null
}

/**
 * Drop-in replacement for the ubiquitous `AsyncImage(model = url.thumbnail(...))`
 * thumbnail blocks: shows the custom photo when set, the fallback otherwise,
 * with the same app-icon placeholder behind it.
 */
@Composable
fun CustomAwareAsyncImage(
    itemKey: String?,
    fallbackUrl: String?,
    modifier: Modifier = Modifier,
    sizePx: Int = 512,
    contentScale: ContentScale = ContentScale.Crop,
    showPlaceholderIcon: Boolean = true,
    iconSize: Dp = 0.dp
) {
    val context = LocalContext.current
    val model = effectiveArtwork(itemKey, fallbackUrl, sizePx)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (showPlaceholderIcon) {
            Icon(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier.size(if (iconSize > 0.dp) iconSize else 32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        AsyncImage(
            model = animatedImageRequest(context, model, sizePx),
            contentDescription = null,
            contentScale = contentScale,
            fallback = painterResource(id = R.drawable.app_icon),
            error = painterResource(id = R.drawable.app_icon),
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium)
        )
    }
}

/** Full-bleed background image (player "My photo" background). */
@Composable
fun CustomPhotoBackground(fileName: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val file = fileName?.let { CustomArtworkStore.fileFor(context, it) }
    if (file != null && file.exists()) {
        AsyncImage(
            model = animatedImageRequest(context, file),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    }
}
