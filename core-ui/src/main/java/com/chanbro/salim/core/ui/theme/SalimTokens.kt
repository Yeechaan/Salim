package com.chanbro.salim.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "같이살림" 디자인 토큰 (소프트 파스텔 테마).
 * design.md 1번(컬러) 기준. 시안과 어긋나면 시안이 우선이며 이 값을 맞춘다.
 */
object SalimTokens {
    // 표면 / 배경
    val Background = Color(0xFFFBF3EA)   // 웜 크림 앱 배경
    val CardSurface = Color(0xFFFFFFFF)  // 카드 배경 (순백)
    val CardBorder = Color(0xFFF0E6DA)   // 구분선/카드 내부 라인

    // 텍스트
    val TextPrimary = Color(0xFF4B403A)  // 본문 (웜 차콜, Ink)
    val TextMuted = Color(0xFFA2948A)    // 보조/메타 텍스트

    // 강조 (Primary = Coral)
    val Accent = Color(0xFFE8896B)       // 선택 탭, 링크(전체보기), D-day 배지, 강조 수치
    val AccentSoft = Color(0xFFFCE4D8)   // Coral 옅은 버전: 아이콘 칩/프로필 배경

    // 진행률 / 구분선
    val ProgressTrack = Color(0xFFF1E7DB)
    val ProgressFillStart = Color(0xFFF3A98E) // Peach (그라데이션 시작)
    val ProgressFillEnd = Color(0xFFE8896B)   // Coral (그라데이션 끝)
    val Divider = Color(0xFFF0E6DA)

    // 파스텔 보조 세트
    val Peach = Color(0xFFF3A98E)
    val Sage = Color(0xFF9FBFA6)
    val Lavender = Color(0xFFB7AEE0)
    val Mint = Color(0xFF93C7B4)

    // 카테고리 도넛 세그먼트 (design.md: 식비=Peach, 문화/여가=Lavender, 교통=Mint)
    val CatFood = Peach       // 식비
    val CatCulture = Lavender // 문화/여가
    val CatTransport = Mint   // 교통
}
