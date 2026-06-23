package com.lokaleza.amatyma.social.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The design uses Inter. TODO: swap AppFont -> bundled Inter (res/font) or the
// Google Fonts provider to match the mockup exactly. System sans is the
// fallback for now so the foundation compiles with no extra setup.
private val AppFont = FontFamily.SansSerif

val AmaTypography = Typography(
    titleLarge  = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold,     fontSize = 20.sp, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold,     fontSize = 16.sp, letterSpacing = (-0.3).sp),
    bodyLarge   = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal,   fontSize = 15.sp),
    bodyMedium  = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal,   fontSize = 14.sp),
    labelLarge  = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelSmall  = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
)
