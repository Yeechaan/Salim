package com.chanbro.salim.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** 같이살림 소프트 파스텔 라이트 테마 (design.md 1번 기준). */
private val LightColorScheme = lightColorScheme(
    primary = SalimTokens.Accent,        // Coral
    onPrimary = Color.White,
    primaryContainer = SalimTokens.AccentSoft,
    onPrimaryContainer = SalimTokens.Accent,
    secondary = SalimTokens.Sage,
    onSecondary = Color.White,
    background = SalimTokens.Background,
    onBackground = SalimTokens.TextPrimary,
    surface = SalimTokens.CardSurface,
    onSurface = SalimTokens.TextPrimary,
    surfaceVariant = SalimTokens.AccentSoft,
    onSurfaceVariant = SalimTokens.TextMuted,
    outlineVariant = SalimTokens.Divider,
)

@Composable
fun SalimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 의도된 파스텔 브랜드 컬러를 쓰기 위해 다이내믹 컬러는 기본 off
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        // 다크 테마 시안이 아직 없으므로 파스텔 라이트 스킴을 항상 사용한다.
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
