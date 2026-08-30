package com.chanbro.salim.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

// ---------------------------------------------------------------------------
// 상대방 연결 공용 컴포넌트 (design.md 2번 "코드 입력 필드" / "QR 카드")
// ---------------------------------------------------------------------------

/**
 * 6칸 분할 코드 입력. (wireframe/connect.md 9-3)
 *
 * 칸을 실제 입력 필드로 만들지 않고, 투명한 필드 하나 위에 칸을 그린다 —
 * 칸마다 포커스를 옮기는 방식은 붙여넣기와 백스페이스에서 자주 어긋난다.
 */
@Composable
fun CodeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(length * 2)) },
        modifier = modifier.focusRequester(focusRequester),
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
        ),
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth()) {
                // 실제 입력은 화면에서 지우고 칸만 보여 준다.
                Box(modifier = Modifier.alpha(0f)) { innerTextField() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(length) { index ->
                        CodeCell(
                            char = value.getOrNull(index),
                            isError = isError,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun CodeCell(char: Char?, isError: Boolean, modifier: Modifier = Modifier) {
    val filled = char != null
    val borderColor = when {
        isError -> SalimTokens.Warning
        filled -> SalimTokens.Accent
        else -> SalimTokens.Divider
    }
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SalimTokens.CardSurface)
            .border(width = if (filled || isError) 1.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char?.toString() ?: "",
            style = SalimType.titleLg,
            color = if (isError) SalimTokens.Warning else SalimTokens.TextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * QR 카드. (wireframe/connect.md 9-2)
 *
 * QR은 흑백을 유지한다 — 파스텔 톤을 입히면 대비가 떨어져 인식률이 나빠진다.
 * 코드 원문을 아래 병기해 스캔이 안 될 때 손입력으로 넘어갈 수 있게 한다.
 */
@Composable
fun QrCard(content: String, code: String, modifier: Modifier = Modifier) {
    val bitmap = remember(content) { qrBitmap(content) }
    SalimCard(modifier = modifier, cornerRadius = 24.dp, contentPadding = 20.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "초대 QR 코드",
                    modifier = Modifier.size(190.dp),
                    // 모듈 경계가 뭉개지지 않도록 보간을 끈다.
                    filterQuality = FilterQuality.None,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SalimTokens.ProgressTrack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("QR을 만들지 못했어요", style = SalimType.bodySm, color = SalimTokens.TextMuted)
                }
            }
            Text(
                text = code,
                style = SalimType.bodyLg.copy(fontWeight = FontWeight.Bold, letterSpacing = 3.sp),
                color = SalimTokens.TextMuted,
            )
        }
    }
}

/** ZXing으로 QR 비트맵 생성. 실패하면 null — 코드 원문이 있으므로 화면은 계속 쓸 수 있다. */
private fun qrBitmap(content: String, sizePx: Int = 512): Bitmap? = runCatching {
    val hints = mapOf(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    createBitmap(width, height).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}.getOrNull()

@Preview(showBackground = true, backgroundColor = 0xFFFBF3EA)
@Composable
private fun CodeInputPreview() {
    SalimTheme {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CodeInputField(value = "7K2", onValueChange = {})
            CodeInputField(value = "7K2M9Q", onValueChange = {}, isError = true)
        }
    }
}
