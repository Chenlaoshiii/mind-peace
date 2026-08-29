package com.mindpeace.app.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mindpeace.app.R
import com.mindpeace.app.data.VisualStyle

@OptIn(ExperimentalTextApi::class)
private fun newsreader(weight: FontWeight, optical: Float): Font = Font(
    resId = R.font.newsreader,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("opsz", optical),
    ),
)

val NewsreaderFamily = FontFamily(
    newsreader(FontWeight.Normal, 14f),
    newsreader(FontWeight.Medium, 16f),
    newsreader(FontWeight.SemiBold, 24f),
    newsreader(FontWeight.Bold, 36f),
)

private val Sans = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 48.sp),
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

private fun TextStyle.serif(): TextStyle = copy(fontFamily = NewsreaderFamily)

private val OrangeTypography = Typography(
    displayLarge = Typography.displayLarge.serif(),
    displayMedium = Typography.displayMedium.serif(),
    displaySmall = Typography.displaySmall.serif(),
    headlineLarge = Typography.headlineLarge.serif(),
    headlineMedium = Typography.headlineMedium.serif(),
    headlineSmall = Typography.headlineSmall.serif(),
    titleLarge = Typography.titleLarge.serif(),
    titleMedium = Typography.titleMedium.serif(),
    titleSmall = Typography.titleSmall.serif(),
    bodyLarge = Typography.bodyLarge.serif(),
    bodyMedium = Typography.bodyMedium.serif(),
    bodySmall = Typography.bodySmall.serif(),
    labelLarge = Typography.labelLarge.serif(),
    labelMedium = Typography.labelMedium.serif(),
    labelSmall = Typography.labelSmall.serif(),
)

fun typographyFor(style: VisualStyle): Typography {
    return if (style == VisualStyle.ORANGE) OrangeTypography else Typography
}
