package com.chanbro.salim.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

/** 같이살림 브라운 라이트 테마 (Stitch "홈 메인" 시안 기반). */
private val LightColorScheme = lightColorScheme(
    primary = SalimTokens.TextPrimary,
    onPrimary = Color.White,
    secondary = SalimTokens.Accent,
    onSecondary = Color.White,
    background = SalimTokens.Background,
    onBackground = SalimTokens.TextPrimary,
    surface = SalimTokens.Background,
    onSurface = SalimTokens.TextPrimary,
    surfaceVariant = SalimTokens.CardSurface,
    onSurfaceVariant = SalimTokens.TextPrimary,
    outlineVariant = SalimTokens.Divider,
)

@Composable
fun SalimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 의도된 브라운 브랜드 컬러를 쓰기 위해 다이내믹 컬러는 기본 off
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
