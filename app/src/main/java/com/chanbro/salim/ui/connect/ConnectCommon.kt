package com.chanbro.salim.ui.connect

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.SalimType

/** 연결 흐름 공통 상단 바. 되돌아가기(9-1)와 닫기(9-2·9-3) 두 모양을 쓴다. */
@Composable
internal fun ConnectTopBar(title: String, onNavigate: () -> Unit, closeIcon: Boolean = false) {
    Surface(color = SalimTokens.Background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onNavigate) {
                Icon(
                    if (closeIcon) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (closeIcon) "닫기" else "뒤로",
                    tint = SalimTokens.TextPrimary,
                )
            }
            Text(title, style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
        }
    }
}

/** 두 사람을 나타내는 원형 아이콘. 연결 상태 카드·완료 화면 공용. */
@Composable
internal fun CoupleAvatar(size: Dp = 56.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(SalimTokens.AccentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.People,
            contentDescription = null,
            tint = SalimTokens.Accent,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** 실패 사유 → 문구. (wireframe/connect.md 상태 분기 종합) */
@get:StringRes
internal val ConnectError.messageRes: Int
    get() = when (this) {
        ConnectError.NOT_FOUND -> R.string.connect_error_not_found
        ConnectError.EXPIRED -> R.string.connect_error_expired
        ConnectError.OWN_CODE -> R.string.connect_error_own_code
        ConnectError.ALREADY_CONNECTED -> R.string.connect_error_already_connected
        ConnectError.PARTNER_CONNECTED -> R.string.connect_error_partner_connected
        ConnectError.QR_INVALID -> R.string.connect_error_qr
        ConnectError.NETWORK -> R.string.connect_error_network
    }

/** QR/공유 링크 페이로드. 소유 도메인이 없어 커스텀 스킴을 쓴다 (AndroidManifest와 짝). */
internal fun inviteDeepLink(code: String): String = "salim://invite/$code"
