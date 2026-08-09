package com.chanbro.salim.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "같이살림" 홈 화면 디자인 토큰 (미니멀 브라운 테마).
 * Stitch "홈 메인" 시안에서 추출한 값들.
 */
object SalimTokens {
    // 표면 / 배경
    val Background = Color(0xFFFFFFFF)   // 앱 배경
    val CardSurface = Color(0xFFF9F9F9)  // 카드 배경
    val CardBorder = Color(0x0D000000)   // black/5 테두리

    // 텍스트 / 강조
    val TextPrimary = Color(0xFF53352B)  // 기본 텍스트 (딥 브라운)
    val Accent = Color(0xFF8A5A44)       // 링크/강조 (전체보기 등)

    // 진행률 / 구분선
    val ProgressTrack = Color(0xFFE6DEC4)
    val ProgressFill = Color(0xFF53352B)
    val Divider = Color(0xFFE6DEC4)

    // 카테고리 도넛 세그먼트
    val CatFood = Color(0xFF7A5C53)      // 식비
    val CatCulture = Color(0xFFA98467)   // 문화/여가
    val CatTransport = Color(0xFFDDBEA9) // 교통
}
