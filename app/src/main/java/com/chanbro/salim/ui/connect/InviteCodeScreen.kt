package com.chanbro.salim.ui.connect

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.QrCard
import com.chanbro.salim.ui.common.SalimType
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// 9-2. 내 초대 코드 (wireframe/connect.md)
// ---------------------------------------------------------------------------

@Composable
fun InviteCodeScreen(
    onClose: () -> Unit,
    onConnected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteCodeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    // 상대가 수락하는 순간 이 화면이 직접 감지한다 (서버 푸시 없이).
    LaunchedEffect(connected) { if (connected) onConnected() }

    InviteCodeContent(
        state = state,
        modifier = modifier,
        onClose = onClose,
        onCopy = { code ->
            coroutineScope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("초대 코드", code)))
                Toast.makeText(context, R.string.invite_copied, Toast.LENGTH_SHORT).show()
            }
        },
        onShare = { code ->
            val text = context.getString(R.string.invite_share_text, code, inviteDeepLink(code))
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.invite_share_subject))
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.invite_share)))
        },
        onRegenerate = viewModel::regenerate,
    )
}

@Composable
private fun InviteCodeContent(
    state: InviteCodeUiState,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onRegenerate: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ConnectTopBar(stringResource(R.string.invite_title), onNavigate = onClose, closeIcon = true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            when {
                state.loading -> LoadingBlock()
                state.failed -> FailedBlock(onRetry = onRegenerate)
                state.expired -> ExpiredBlock(onRegenerate = onRegenerate)
                state.code != null -> ActiveCodeBlock(
                    code = state.code,
                    remainingSeconds = state.remainingSeconds,
                    onCopy = onCopy,
                    onShare = onShare,
                    onRegenerate = onRegenerate,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = SalimTokens.Accent)
    }
}

@Composable
private fun FailedBlock(onRetry: () -> Unit) {
    Spacer(Modifier.height(60.dp))
    Text(
        stringResource(R.string.invite_failed),
        style = SalimType.bodyLg,
        color = SalimTokens.TextPrimary,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onRetry,
        colors = ButtonDefaults.buttonColors(containerColor = SalimTokens.Accent, contentColor = Color.White),
    ) {
        Text(stringResource(R.string.invite_retry), style = SalimType.bodyMd)
    }
}

/**
 * 만료 화면. 이미 공유한 코드가 죽었다는 사실이 분명히 보이도록 코드를 지우고 안내로 바꾼다 —
 * 같은 자리에 새 코드가 조용히 들어차면 둘이 서로 다른 코드를 보게 된다.
 */
@Composable
private fun ExpiredBlock(onRegenerate: () -> Unit) {
    Spacer(Modifier.height(60.dp))
    Text(
        stringResource(R.string.invite_expired),
        style = SalimType.titleLg,
        color = SalimTokens.TextPrimary,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onRegenerate,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SalimTokens.Accent, contentColor = Color.White),
    ) {
        Text(
            stringResource(R.string.invite_regenerate),
            style = SalimType.bodyLg.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun ActiveCodeBlock(
    code: String,
    remainingSeconds: Int,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onRegenerate: () -> Unit,
) {
    // 탭하면 복사된다는 것을 알 수 있도록 코드 자체를 넉넉한 탭 영역으로 둔다.
    Text(
        // 끊어 쓰지 않는다 — 옮겨 적는 코드라 공백이 보이면 코드의 일부로 읽힌다.
        // 읽기 편하게 하는 건 자간이 맡는다.
        text = code,
        style = SalimType.headlineMd.copy(letterSpacing = 8.sp),
        color = SalimTokens.TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCopy(code) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
    Spacer(Modifier.height(4.dp))
    Text(
        // 1분 미만이면 분을 떼고 초만 — "0분 12초"보다 "12초"가 읽기 쉽다.
        text = if (remainingSeconds < 60) {
            stringResource(R.string.invite_remaining_seconds, remainingSeconds)
        } else {
            stringResource(R.string.invite_remaining, remainingSeconds / 60, remainingSeconds % 60)
        },
        style = SalimType.bodySm,
        color = SalimTokens.TextMuted,
    )
    Spacer(Modifier.height(20.dp))
    QrCard(content = inviteDeepLink(code), code = code)
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = { onShare(code) },
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SalimTokens.Accent, contentColor = Color.White),
    ) {
        Text(stringResource(R.string.invite_share), style = SalimType.bodyLg.copy(fontWeight = FontWeight.Bold))
    }
    TextButton(onClick = onRegenerate) {
        Text(stringResource(R.string.invite_regenerate), style = SalimType.bodyMd, color = SalimTokens.TextMuted)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun InviteCodeScreenPreview() {
    SalimTheme {
        InviteCodeContent(
            state = InviteCodeUiState(loading = false, code = "7K2M9Q", remainingSeconds = 1_712),
            modifier = Modifier.background(SalimTokens.Background),
            onClose = {},
            onCopy = {},
            onShare = {},
            onRegenerate = {},
        )
    }
}
