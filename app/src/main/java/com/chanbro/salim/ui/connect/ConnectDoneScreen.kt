package com.chanbro.salim.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import com.chanbro.salim.ui.common.SalimType

// ---------------------------------------------------------------------------
// 9-5. 연결 완료 (wireframe/connect.md)
// 발급자(9-2)와 수락자(9-4) 양쪽이 이 화면에 도착한다.
// ---------------------------------------------------------------------------

@Composable
fun ConnectDoneScreen(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectViewModel = hiltViewModel(),
) {
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val partnerName = (connection as? Connection.Connected)?.partner?.nameOrDefault ?: "상대방"
    ConnectDoneContent(partnerName = partnerName, modifier = modifier, onHome = onHome)
}

@Composable
private fun ConnectDoneContent(
    partnerName: String,
    modifier: Modifier = Modifier,
    onHome: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CoupleAvatar(size = 88.dp)
                Text(
                    stringResource(R.string.connect_done_title, partnerName),
                    style = SalimType.headlineSm,
                    color = SalimTokens.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.connect_done_body),
                    style = SalimType.bodyMd,
                    color = SalimTokens.TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Button(
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SalimTokens.Accent,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    stringResource(R.string.connect_done_home),
                    style = SalimType.bodyLg.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ConnectDoneScreenPreview() {
    SalimTheme {
        ConnectDoneContent(
            partnerName = "예찬",
            modifier = Modifier.background(SalimTokens.Background),
            onHome = {},
        )
    }
}
