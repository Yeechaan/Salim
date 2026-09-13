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

    // 경고 (design.md: 예산 100% 초과, 입력 오류 — 테라코타 계열)
    val Warning = Color(0xFFC1614A)

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

    // 카테고리 팔레트 (design.md "카테고리 색"). 카테고리마다 한 색 — 고정 항목끼리는 계열이 겹치지 않는다.
    // 파스텔 톤을 유지하되 위 파스텔 세트보다 한 단계 진하게 잡아, 옅은 배경(24%) 위에서도 카테고리끼리 갈리게 한다.
    val CatPeach = Color(0xFFF09A7E)
    val CatSage = Color(0xFF86BC96)
    val CatRose = Color(0xFFE791B0)
    val CatLavender = Color(0xFFA89FE0)
    val CatSky = Color(0xFF7FB2E3)
    val CatMint = Color(0xFF72BFB2)
    val CatButter = Color(0xFFE6C274)
    val CatLilac = Color(0xFFC99BDB)
    val CatClay = Color(0xFFC79B7C)
    val CatApricot = Color(0xFFF0B27A)
    val CatWarmGray = Color(0xFFADA195)
    val CatOlive = Color(0xFFB3C27A)

    /** 카테고리 칩·아이콘 배지의 옅은 배경 농도. */
    const val CategoryTintAlpha = 0.24f

    /** 선택된 카테고리 칩의 배경 농도. 같은 카테고리색 테두리와 함께 쓴다. */
    const val CategorySelectedAlpha = 0.40f
}
