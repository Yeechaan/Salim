package com.chanbro.salim.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.chanbro.salim.domain.model.Invite
import com.chanbro.salim.ui.common.CodeInputField
import com.chanbro.salim.ui.common.SalimType
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

// ---------------------------------------------------------------------------
// 9-3. 코드 입력 + 9-4. 확인 시트 (wireframe/connect.md)
// ---------------------------------------------------------------------------

@Composable
fun CodeInputScreen(
    onClose: () -> Unit,
    onConnected: () -> Unit,
    modifier: Modifier = Modifier,
    prefillCode: String? = null,
    viewModel: CodeInputViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(prefillCode) { viewModel.prefill(prefillCode) }
    LaunchedEffect(state.done) { if (state.done) onConnected() }

    CodeInputContent(
        state = state,
        modifier = modifier,
        onClose = onClose,
        onCodeChange = viewModel::onCodeChange,
        onSubmit = viewModel::submit,
        onScan = {
            // GMS 코드 스캐너 — 카메라 권한 없이 시스템이 스캐너 UI를 띄운다.
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            GmsBarcodeScanning.getClient(context, options).startScan()
                .addOnSuccessListener { viewModel.onScanned(it.rawValue) }
                .addOnFailureListener { viewModel.onScanFailed() }
                // 사용자가 스스로 닫은 것에는 오류를 띄우지 않는다 (로그인 화면과 같은 규칙).
                .addOnCanceledListener { }
        },
    )

    state.pending?.let { invite ->
        ConfirmSheet(
            invite = invite,
            connecting = state.connecting,
            onConfirm = viewModel::confirm,
            onDismiss = viewModel::dismissConfirm,
        )
    }
}

@Composable
private fun CodeInputContent(
    state: CodeInputUiState,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onScan: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ConnectTopBar(stringResource(R.string.code_input_title), onNavigate = onClose, closeIcon = true)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.code_input_body),
                style = SalimType.bodyMd,
                color = SalimTokens.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            CodeInputField(
                value = state.code,
                onValueChange = onCodeChange,
                isError = state.error != null,
                enabled = !state.connecting,
                modifier = Modifier.fillMaxWidth(),
            )
            // 오류 문구가 없을 때도 자리를 지켜 레이아웃이 밀리지 않게 한다. (connect.md 9-3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                state.error?.let {
                    Text(
                        stringResource(it.messageRes),
                        style = SalimType.bodySm,
                        color = SalimTokens.Warning,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            TextButton(onClick = onScan, enabled = !state.connecting) {
                Text(
                    stringResource(R.string.code_input_scan),
                    style = SalimType.bodyMd,
                    color = SalimTokens.Accent,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SalimTokens.Accent,
                    contentColor = Color.White,
                    disabledContainerColor = SalimTokens.ProgressTrack,
                    disabledContentColor = SalimTokens.TextMuted,
                ),
            ) {
                if (state.checking) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.height(22.dp))
                } else {
                    Text(
                        stringResource(R.string.code_input_submit),
                        style = SalimType.bodyLg.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

/** 9-4. 연결은 되돌리기 어려운 동작이라 상대와 데이터 범위를 한 번 확인시킨다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmSheet(
    invite: Invite,
    connecting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        // 연결 진행 중에는 시트를 닫지 않는다.
        onDismissRequest = { if (!connecting) onDismiss() },
        sheetState = sheetState,
        containerColor = SalimTokens.CardSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.connect_confirm_title, invite.inviterNameOrDefault),
                style = SalimType.titleLg,
                color = SalimTokens.TextPrimary,
            )
            Text(
                stringResource(R.string.connect_confirm_body),
                style = SalimType.bodyMd,
                color = SalimTokens.TextMuted,
            )
            Button(
                onClick = onConfirm,
                enabled = !connecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SalimTokens.Accent,
                    contentColor = Color.White,
                    disabledContainerColor = SalimTokens.ProgressTrack,
                ),
            ) {
                if (connecting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.height(22.dp))
                } else {
                    Text(
                        stringResource(R.string.code_input_submit),
                        style = SalimType.bodyLg.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
            TextButton(
                onClick = onDismiss,
                enabled = !connecting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.common_cancel), style = SalimType.bodyMd, color = SalimTokens.TextMuted)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CodeInputScreenPreview() {
    SalimTheme {
        CodeInputContent(
            state = CodeInputUiState(code = "7K2M", error = ConnectError.NOT_FOUND),
            modifier = Modifier.background(SalimTokens.Background),
            onClose = {},
            onCodeChange = {},
            onSubmit = {},
            onScan = {},
        )
    }
}
