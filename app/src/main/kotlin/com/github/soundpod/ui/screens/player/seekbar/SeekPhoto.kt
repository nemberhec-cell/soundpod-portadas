package com.github.soundpod.ui.screens.player.seekbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.github.soundpod.customart.CustomArtworkStore
import com.github.soundpod.customart.rememberCustomArtworkFileName

/** Per-song seek-dot image key in CustomArtwork table. */
fun seekDotKey(mediaId: String?) = mediaId?.takeIf { it.isNotBlank() }?.let { "seekdot:$it" }

/** Returns this song's photo/GIF file, or null when this song has none. */
@Composable
fun rememberSeekPhoto(mediaId: String?): java.io.File? {
    val context = LocalContext.current
    val fileName by rememberCustomArtworkFileName(seekDotKey(mediaId))
    return fileName?.let { CustomArtworkStore.fileFor(context, it) }?.takeIf { it.exists() }
}

/** Your photo or GIF as the dot for THIS song, centered at [center]. */
@Composable
fun SeekPhotoDot(
    center: Offset,
    mediaId: String?,
    dotSize: androidx.compose.ui.unit.Dp = 28.dp
): Boolean {
    val context = LocalContext.current
    val seekPhoto = rememberSeekPhoto(mediaId)
    if (seekPhoto == null) return false
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (center.x - dotSize.toPx() / 2f).toInt(),
                    y = (center.y - dotSize.toPx() / 2f).toInt()
                )
            }
            .size(dotSize)
            .clip(androidx.compose.foundation.shape.CircleShape)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(seekPhoto)
                .allowHardware(true)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .size(56)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(dotSize)
        )
    }
    return true
}
