package com.chanbro.salim.ui.connect

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.PartnerProfile
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// 9-1. 연결 관리 (wireframe/connect.md)
//
// 연결 해제는 30일 유예 삭제에 서버 작업이 필요해 1차 범위 밖이다 — 동작하는 것처럼
// 보이지 않도록 비활성으로만 노출한다. (PRD 9 "1차 구현 범위")
// ---------------------------------------------------------------------------

@Composable
fun ConnectScreen(
    onClose: () -> Unit,
    onCreateCode: () -> Unit,
    onEnterCode: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectViewModel = hiltViewModel(),
) {
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    ConnectContent(
        connection = connection,
        modifier = modifier,
        onClose = onClose,
        onCreateCode = onCreateCode,
        onEnterCode = onEnterCode,
    )
}

@Composable
private fun ConnectContent(
    connection: Connection,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onCreateCode: () -> Unit,
    onEnterCode: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ConnectTopBar(stringResource(R.string.connect_title), onNavigate = onClose)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (connection) {
                // 판정 전에는 아무것도 그리지 않는다 — 연결된 사용자에게 "연결해보세요"가
                // 한 번 깜빡이는 것을 막기 위해서다.
                Connection.Unknown -> Unit
                Connection.None -> UnconnectedBody(onCreateCode = onCreateCode, onEnterCode = onEnterCode)
                is Connection.Connected -> ConnectedBody(connection)
            }
        }
    }
}

@Composable
private fun UnconnectedBody(onCreateCode: () -> Unit, onEnterCode: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    CoupleAvatar(size = 72.dp)
    Spacer(Modifier.height(24.dp))
    Text(
        stringResource(R.string.connect_intro_title),
        style = SalimType.titleLg,
        color = SalimTokens.TextPrimary,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(R.string.connect_intro_body),
        style = SalimType.bodyMd,
        color = SalimTokens.TextMuted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(40.dp))
    Button(
        onClick = onCreateCode,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SalimTokens.Accent,
            contentColor = Color.White,
        ),
    ) {
        Text(
            stringResource(R.string.connect_create_code),
            style = SalimType.bodyLg.copy(fontWeight = FontWeight.Bold),
        )
    }
    Spacer(Modifier.height(4.dp))
    TextButton(onClick = onEnterCode) {
        Text(
            stringResource(R.string.connect_enter_code),
            style = SalimType.bodyMd,
            color = SalimTokens.TextMuted,
        )
    }
}

@Composable
private fun ConnectedBody(connection: Connection.Connected) {
    Spacer(Modifier.height(24.dp))
    SalimCard(cornerRadius = 24.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoupleAvatar(size = 52.dp)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    stringResource(R.string.connect_connected, connection.partner.nameOrDefault),
                    style = SalimType.bodyLg,
                    color = SalimTokens.TextPrimary,
                )
                Text(
                    stringResource(R.string.connect_connected_since, formatConnectedAt(connection.connectedAtMillis)),
                    style = SalimType.bodySm,
                    color = SalimTokens.TextMuted,
                )
            }
        }
    }
    Spacer(Modifier.height(28.dp))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            "${stringResource(R.string.connect_disconnect)} · ${stringResource(R.string.connect_preparing)}",
            style = SalimType.bodyMd,
            color = SalimTokens.TextMuted,
        )
    }
}

private fun formatConnectedAt(millis: Long): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN).format(Date(millis))

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ConnectScreenUnconnectedPreview() {
    SalimTheme {
        ConnectContent(
            connection = Connection.None,
            modifier = Modifier.background(SalimTokens.Background),
            onClose = {},
            onCreateCode = {},
            onEnterCode = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ConnectScreenConnectedPreview() {
    SalimTheme {
        ConnectContent(
            connection = Connection.Connected(
                coupleId = "a_b",
                partner = PartnerProfile("b", "예찬", null),
                connectedAtMillis = 1_741_000_000_000,
            ),
            modifier = Modifier.background(SalimTokens.Background),
            onClose = {},
            onCreateCode = {},
            onEnterCode = {},
        )
    }
}
