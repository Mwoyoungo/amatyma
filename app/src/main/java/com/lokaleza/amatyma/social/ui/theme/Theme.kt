package com.lokaleza.amatyma.social.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmaColorScheme = darkColorScheme(
    primary      = AmaCrimson,
    onPrimary    = Color.White,
    secondary    = AmaPurple,
    background   = AmaBlack,
    onBackground = AmaTextPrimary,
    surface      = AmaBlack,
    onSurface    = AmaTextPrimary,
)

/** Theme wrapper for the new social/video surface. Chat keeps its own (Views) theme. */
@Composable
fun AmatymaSocialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AmaColorScheme,
        typography  = AmaTypography,
        content     = content,
    )
}
