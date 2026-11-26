package com.github.soundpod.ui.appearance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import com.github.core.ui.LocalAppearance
import com.github.core.visuals.ColorClusters
import com.github.core.visuals.BlurredBackground
import com.github.core.visuals.ThemedLottieBackground
import com.github.core.visuals.adaptToTheme
import com.github.core.visuals.extractColorClusters
import com.github.soundpod.utils.rememberPreference

const val PLAYER_BACKGROUND_STYLE_KEY = "player_background_style"
const val PLAYER_BACKGROUND_PHOTO_KEY = "player_background_photo_file"
const val PLAYER_BACKGROUND_DIM_KEY = "player_background_dim"

object BackgroundStyles {
    const val OFF = -1
    const val CROSS_FADE = 0
    const val ABSTRACT_1 = 1
    const val ABSTRACT_2 = 2
    const val ABSTRACT_3 = 3
    const val ABSTRACT_4 = 4
    const val BLURRED = 5
    const val MY_PHOTO = 6
}

@Composable
fun PlayerBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    expandProgress: Float = 0f,
    mediaId: String? = null,
    content: @Composable () -> Unit
) {
    val currentStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.CROSS_FADE)
    // Per-song background photo: "playerbg:<mediaId>" in CustomArtwork.
    // Falls back to the legacy global photo only while the song has none.
    val legacyPhotoFile by rememberPreference(PLAYER_BACKGROUND_PHOTO_KEY, "")
    val songPhotoFile by com.github.soundpod.customart.rememberCustomArtworkFileName(
        mediaId?.takeIf { it.isNotBlank() }?.let { "playerbg:$it" }
    )
    val customPhotoFile = songPhotoFile ?: legacyPhotoFile
    val customPhotoDim by rememberPreference(PLAYER_BACKGROUND_DIM_KEY, 55)
    val (colorPalette) = LocalAppearance.current
    val context = LocalContext.current

    val fallbackColor = colorPalette.accent

    var clusters by remember { 
        mutableStateOf(ColorClusters(fallbackColor, fallbackColor, fallbackColor, fallbackColor, fallbackColor, fallbackColor))
    }
    
    val photoSource: String? = remember(customPhotoFile) {
        customPhotoFile.takeIf { it.isNotBlank() }?.let {
            java.io.File(context.filesDir, "custom_artwork/$it").takeIf { f -> f.exists() }?.absolutePath
        }
    }
    LaunchedEffect(thumbnailUrl, colorPalette, photoSource) {
        clusters = extractColorClusters(
            context,
            (photoSource ?: thumbnailUrl),
            fallbackColor
        )
    }

    val isDark = colorPalette.isDark
    val baseBackground = colorPalette.background0
    val targetBackgroundColorRaw = remember(clusters, isDark, currentStyle) {
        when {
            currentStyle == BackgroundStyles.OFF -> baseBackground
            // "My photo": the photo itself fills the screen, so only a slight
            // tint goes under it (it used to cover the photo entirely).
            currentStyle == BackgroundStyles.MY_PHOTO -> baseBackground
            else -> {
                val adapted = clusters.surface.adaptToTheme(isDark)
                adapted.copy(alpha = if (isDark) 0.35f else 0.25f).compositeOver(baseBackground)
            }
        }
    }
    val targetBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColorRaw,
        animationSpec = tween(1200),
        label = "targetBackgroundColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(targetBackgroundColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().alpha(expandProgress)) {
            when (currentStyle) {
                BackgroundStyles.OFF -> {
                    // Already handled by targetBackgroundColor
                }
                BackgroundStyles.CROSS_FADE -> {
                    val staticColor by animateColorAsState(
                        targetValue = clusters.c1,
                        animationSpec = tween(1000),
                        label = "staticColor"
                    )
                    Box(Modifier.fillMaxSize().background(baseBackground))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to staticColor.copy(alpha = if (isDark) 0.45f else 0.35f),
                                    0.5f to staticColor.copy(alpha = if (isDark) 0.45f else 0.35f),
                                    1f to Color.Transparent
                                )
                            )
                    )
                }
                BackgroundStyles.BLURRED -> {
                    BlurredBackground(
                        thumbnailUrl = thumbnailUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                BackgroundStyles.MY_PHOTO -> {
                    // Your photo (or GIF) fills the whole player screen when
                    // the song is open, so the text stays readable.
                    com.github.soundpod.customart.CustomPhotoBackground(
                        fileName = customPhotoFile.takeIf { it.isNotBlank() },
                        modifier = Modifier.fillMaxSize()
                    )
                    val photoScrim = customPhotoDim.coerceIn(0, 100) / 100f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    0f to baseBackground.copy(alpha = photoScrim),
                                    0.4f to baseBackground.copy(alpha = photoScrim * 0.7f),
                                    1f to baseBackground.copy(alpha = photoScrim)
                                )
                            )
                    )
                }
                else -> {
                    ThemedLottieBackground(
                        animationNumber = currentStyle,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        content()
    }
}
