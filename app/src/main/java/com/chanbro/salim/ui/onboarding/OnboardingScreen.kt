package com.chanbro.salim.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import kotlinx.coroutines.launch

/**
 * 온보딩 2장. (docs/wireframe/onboarding.md 1-1, 1-2)
 * 마지막 슬라이드의 "시작하기"에서 완료를 기록하고 로그인으로 넘어간다.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Outlined.Favorite,
            titleRes = R.string.onboarding_1_title,
            bodyRes = R.string.onboarding_1_body,
            buttonRes = R.string.onboarding_next,
        ),
        OnboardingPage(
            icon = Icons.Outlined.Link,
            titleRes = R.string.onboarding_2_title,
            bodyRes = R.string.onboarding_2_body,
            buttonRes = R.string.onboarding_start,
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SalimTokens.Background)
            .statusBarsPadding(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        PageIndicator(pageCount = pages.size, selected = pagerState.currentPage)
        Spacer(Modifier.height(8.dp))

        val isLast = pagerState.currentPage == pages.lastIndex
        SaveButton(
            enabled = true,
            label = stringResource(pages[pagerState.currentPage].buttonRes),
            onClick = {
                if (isLast) onFinish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
        )
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
    val buttonRes: Int,
)

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 일러스트 자리 — 최종 시안 나오면 이미지로 교체 (design.md 아이콘 규칙: 라인 아이콘)
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(SalimTokens.AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = SalimTokens.Accent,
                modifier = Modifier.size(52.dp),
            )
        }
        Spacer(Modifier.height(40.dp))
        Text(
            text = stringResource(page.titleRes),
            style = SalimType.headlineSm,
            color = SalimTokens.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(page.bodyRes),
            style = SalimType.bodyMd,
            color = SalimTokens.TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicator(pageCount: Int, selected: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (active) 9.dp else 7.dp)
                    .clip(CircleShape)
                    .background(if (active) SalimTokens.Accent else SalimTokens.ProgressTrack),
            )
        }
    }
}
