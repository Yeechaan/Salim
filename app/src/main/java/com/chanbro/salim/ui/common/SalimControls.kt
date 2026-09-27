package com.chanbro.salim.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTokens

// ---------------------------------------------------------------------------
// 세그먼트 (design.md "세그먼트") — 같은 화면 안의 내용 전환. 쓰임: 결혼 준비 체크리스트/예산/업체
// Material3 SegmentedButton은 테두리형이라 톤이 맞지 않아 직접 그린다.
// ---------------------------------------------------------------------------

@Composable
fun SalimSegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(SalimTokens.ProgressTrack)
            .padding(3.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // Coral을 쓰지 않는다 — 아래 칩·체크박스·FAB가 이미 Coral이라 강조가 겹친다.
                    .then(
                        if (selected) {
                            Modifier.shadow(2.dp, shape, clip = false, spotColor = Color(0x33785A46))
                        } else {
                            Modifier
                        },
                    )
                    .clip(shape)
                    .background(if (selected) SalimTokens.CardSurface else Color.Transparent)
                    .clickable { onSelect(index) }
                    .semantics {
                        role = Role.Tab
                        this.selected = selected
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = SalimType.labelMd.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                    color = if (selected) SalimTokens.TextPrimary else SalimTokens.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 진행률 바 (design.md "진행률 바") — 기본 / 두 겹(지출 완료 + 지출 예정) / 얇은 변형
// ---------------------------------------------------------------------------

/**
 * @param ratio 채울 비율(0~1 밖이면 자른다).
 * @param plannedRatio 이어서 옅은 Coral로 채울 비율 — 웨딩 예산의 지출 예정분. 합이 1을 넘으면 넘는 만큼 잘린다.
 * @param over 경고색. 앞 구간(지출 완료분)에만 적용한다.
 */
@Composable
fun SalimProgressBar(
    ratio: Float,
    modifier: Modifier = Modifier,
    plannedRatio: Float = 0f,
    over: Boolean = false,
    height: Dp = 10.dp,
) {
    val filled = ratio.coerceIn(0f, 1f)
    val planned = plannedRatio.coerceIn(0f, 1f - filled)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(SalimTokens.ProgressTrack),
    ) {
        // 예정분을 먼저 (완료+예정) 길이로 깔고 완료분을 위에 얹는다 — 두 구간 사이에 틈이 생기지 않는다.
        if (planned > 0f) {
            Box(
                modifier = Modifier
                    .width(maxWidth * (filled + planned))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(SalimTokens.AccentSoft),
            )
        }
        if (filled > 0f) {
            Box(
                modifier = Modifier
                    .width(maxWidth * filled)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        if (over) {
                            SolidColor(SalimTokens.Warning)
                        } else {
                            Brush.horizontalGradient(listOf(SalimTokens.ProgressFillStart, SalimTokens.ProgressFillEnd))
                        },
                    ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 원형 체크 (design.md 리스트 아이템 - 할 일). 할 일·결혼 준비 체크리스트 공용
// ---------------------------------------------------------------------------

/** 줄 탭(수정)과 따로 눌리는 체크 버튼. */
@Composable
fun CheckCircleButton(checked: Boolean, size: Dp, onClick: () -> Unit) {
    val label = stringResource(if (checked) R.string.todo_uncheck else R.string.todo_check)
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = label }) {
        CheckCircle(checked = checked, size = size)
    }
}

/** 완료 시 Coral 채움 + 흰 체크, 미완료는 옅은 테두리 원. */
@Composable
fun CheckCircle(checked: Boolean, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (checked) {
                    Modifier.background(SalimTokens.Accent)
                } else {
                    Modifier.border(2.dp, SalimTokens.TextMuted.copy(alpha = 0.6f), CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.66f))
        }
    }
}

// ---------------------------------------------------------------------------
// 입력 화면 공용 — 지출 입력 / 웨딩 지출 기록 / 업체 추가
// ---------------------------------------------------------------------------

/** 하위 화면 상단: 좌측 뒤로(또는 닫기), 가운데 제목, 우측 액션 하나. */
@Composable
fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = SalimTokens.TextPrimary,
            )
        }
        Text(
            title,
            style = SalimType.titleLg,
            color = SalimTokens.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 56.dp),
        )
        if (action != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) { action() }
        }
    }
}

/** 큰 금액 입력. 숫자는 "원" 앞에 오른쪽 정렬, "원" 뒤에 여백. (expense.md 4-2) */
@Composable
fun AmountInput(digits: String, onDigitsChange: (String) -> Unit) {
    val amountStyle = SalimType.display.copy(fontSize = 40.sp, lineHeight = 48.sp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 입력칸이 남는 폭을 쓰고 숫자는 "원" 앞에 오른쪽 정렬한다. BasicTextField는 기본 최소 폭(10글자)이 있어
        // 폭을 정해 주지 않으면 이 줄을 거의 다 차지하고, "원"이 화면 끝에 붙거나 좁은 화면에서 잘린다.
        BasicTextField(
            value = digits,
            onValueChange = onDigitsChange,
            modifier = Modifier.weight(1f),
            textStyle = amountStyle.copy(color = SalimTokens.TextPrimary, textAlign = TextAlign.End),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(SalimTokens.Accent),
            visualTransformation = ThousandsTransformation,
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (digits.isEmpty()) {
                        Text("0", style = amountStyle, color = SalimTokens.TextMuted)
                    }
                    inner()
                }
            },
        )
        // "원" 뒤 여백 — 아래 카드 안쪽 값(날짜·시간)의 오른쪽 끝과 맞춘다. (expense.md 4-2)
        Text(
            "원",
            style = amountStyle,
            color = SalimTokens.TextPrimary,
            modifier = Modifier.padding(end = 20.dp),
        )
    }
}

/** 카드 안 라벨 + 한 줄(또는 여러 줄) 텍스트 입력. 메모·업체명·연락처 등. */
@Composable
fun TextInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(
        modifier = modifier.padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = SalimType.bodyMd, color = SalimTokens.TextMuted)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = SalimType.bodyLg.copy(color = SalimTokens.TextPrimary),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(SalimTokens.Accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(hint, style = SalimType.bodyLg, color = SalimTokens.TextMuted)
                }
                inner()
            },
        )
    }
}
