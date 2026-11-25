package com.github.soundpod.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.customart.CustomPhotoBackground

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = LocalAppearance.current.colorPalette.mainBackground,
    elevation: androidx.compose.ui.unit.Dp = 1.dp,
    backgroundPhoto: String? = null,
    backgroundDim: Int = 55,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = shape,
        color = color,
        shadowElevation = elevation
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (backgroundPhoto != null) {
                // Front layer (the grey card) photo with a dim scrim so the
                // text on top stays readable.
                CustomPhotoBackground(
                    fileName = backgroundPhoto,
                    modifier = Modifier.fillMaxSize()
                )
                val scrimAlpha = backgroundDim.coerceIn(0, 100) / 100f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to color.copy(alpha = scrimAlpha),
                                1f to color.copy(alpha = scrimAlpha)
                            )
                        )
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    }
}
