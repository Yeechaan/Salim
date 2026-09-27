package com.chanbro.salim.ui.wedding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.domain.model.TodoAssignee
import com.chanbro.salim.domain.model.VendorStatus
import com.chanbro.salim.domain.model.WeddingItem
import com.chanbro.salim.domain.model.WeddingPeriod
import com.chanbro.salim.ui.common.CategoryChip
import com.chanbro.salim.ui.common.ChipFlowRow
import com.chanbro.salim.ui.common.SalimChip
import com.chanbro.salim.ui.common.SalimType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ---------------------------------------------------------------------------
// 결혼 준비 화면 공용 — 웨딩 항목 색·아이콘(design.md "웨딩 항목 색·아이콘"), 라벨, 서식
// ---------------------------------------------------------------------------

/** 카테고리 색 키를 빌려 쓴다 — 칩·배지를 가계부 카테고리와 같은 컴포넌트로 그리기 위해. */
fun WeddingItem.colorKey(): String = when (this) {
    WeddingItem.VENUE -> "rose"
    WeddingItem.SDM -> "lavender"
    WeddingItem.GIFTS -> "butter"
    WeddingItem.HOUSEHOLD -> "clay"
    WeddingItem.HONEYMOON -> "sky"
    WeddingItem.ETC -> "warmgray"
}

fun WeddingItem.icon(): ImageVector = when (this) {
    WeddingItem.VENUE -> Icons.Filled.Church
    WeddingItem.SDM -> Icons.Filled.Checkroom
    WeddingItem.GIFTS -> Icons.Filled.Diamond
    WeddingItem.HOUSEHOLD -> Icons.Filled.Chair
    WeddingItem.HONEYMOON -> Icons.Filled.Flight
    WeddingItem.ETC -> Icons.Filled.MoreHoriz
}

@StringRes
fun WeddingItem.labelRes(): Int = when (this) {
    WeddingItem.VENUE -> R.string.wedding_item_venue
    WeddingItem.SDM -> R.string.wedding_item_sdm
    WeddingItem.GIFTS -> R.string.wedding_item_gifts
    WeddingItem.HOUSEHOLD -> R.string.wedding_item_household
    WeddingItem.HONEYMOON -> R.string.wedding_item_honeymoon
    WeddingItem.ETC -> R.string.wedding_item_etc
}

@StringRes
fun WeddingPeriod.labelRes(): Int = when (this) {
    WeddingPeriod.M12_6 -> R.string.wedding_period_m12_6
    WeddingPeriod.M6_3 -> R.string.wedding_period_m6_3
    WeddingPeriod.M3_1 -> R.string.wedding_period_m3_1
    WeddingPeriod.M1_W1 -> R.string.wedding_period_m1_w1
    WeddingPeriod.W1 -> R.string.wedding_period_w1
}

@StringRes
fun VendorStatus.labelRes(): Int = when (this) {
    VendorStatus.CONSULTING -> R.string.wedding_vendor_status_consulting
    VendorStatus.CONTRACTED -> R.string.wedding_vendor_status_contracted
}

@Composable
fun assigneeLabel(assignee: TodoAssignee, names: SpenderNames): String = when (assignee) {
    TodoAssignee.TOGETHER -> stringResource(R.string.todo_assignee_together)
    TodoAssignee.ME -> names.mine
    TodoAssignee.PARTNER -> names.partner
}

/** 웨딩 항목 6개 칩. (wedding.md 12-5 / 12-8) */
@Composable
fun WeddingItemChips(selected: WeddingItem?, onSelect: (WeddingItem) -> Unit) {
    ChipFlowRow {
        WeddingItem.entries.forEach { item ->
            CategoryChip(
                icon = item.icon(),
                colorKey = item.colorKey(),
                label = stringResource(item.labelRes()),
                selected = item == selected,
                onClick = { onSelect(item) },
            )
        }
    }
}

/** 가계부 지출자와 같은 칩 줄. 미연결이면 호출하지 않는다. */
@Composable
fun SpenderChips(names: SpenderNames, selected: Spender, onSelect: (Spender) -> Unit) {
    ChipFlowRow {
        Spender.entries.forEach { option ->
            SalimChip(label = names.labelOf(option), selected = option == selected, onClick = { onSelect(option) })
        }
    }
}

/** 업체 상태 칩 (계약 완료: Coral 채움 / 상담 중: 옅은 칩). 목록 표시용 — 누를 수 없다. */
@Composable
fun VendorStatusTag(status: VendorStatus) {
    val contracted = status == VendorStatus.CONTRACTED
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (contracted) SalimTokens.Accent else SalimTokens.AccentSoft)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            stringResource(status.labelRes()),
            style = SalimType.labelSm.copy(fontWeight = FontWeight.Bold),
            color = if (contracted) Color.White else SalimTokens.Accent,
        )
    }
}

/** 카드 안 라벨 좌 / 값 우 한 줄 (업체 상세·예산 카드). */
@Composable
fun LabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = SalimTokens.TextPrimary,
    labelColor: Color = SalimTokens.TextMuted,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = SalimType.bodyMd, color = labelColor)
        Text(
            value,
            style = SalimType.bodyMd.copy(fontWeight = FontWeight.Medium),
            color = valueColor,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SalimTokens.Divider),
    )
}

@Composable
fun CenteredEmpty(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Text(text, style = SalimType.bodyLg, color = SalimTokens.TextMuted)
    }
}

@Composable
fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    body: String = stringResource(R.string.common_delete_confirm_body),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.common_delete_confirm), style = SalimType.titleLg, color = SalimTokens.TextPrimary)
        },
        text = { Text(body, style = SalimType.bodyMd, color = SalimTokens.TextMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_delete), color = SalimTokens.Accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = SalimTokens.TextMuted) }
        },
        containerColor = SalimTokens.CardSurface,
    )
}

/** 바텀시트 안 한 줄 입력 (할 일 시트와 같은 모양). */
@Composable
fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    maxLength: Int,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onValueChange(it) },
        textStyle = SalimType.bodyLg.copy(color = SalimTokens.TextPrimary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        cursorBrush = SolidColor(SalimTokens.Accent),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SalimTokens.Background)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .focusRequester(focusRequester),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, style = SalimType.bodyLg, color = SalimTokens.TextMuted)
                inner()
            }
        },
    )
}

@Composable
fun SheetSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = SalimType.labelMd, color = SalimTokens.TextMuted)
        content()
    }
}

/** 탭 가능한 텍스트 링크(Coral). */
@Composable
fun AccentLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        style = SalimType.bodyMd.copy(fontWeight = FontWeight.Medium),
        color = SalimTokens.Accent,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

// --- 서식 ---

private fun utcFormat(pattern: String) = SimpleDateFormat(pattern, Locale.KOREAN).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

/** "2027년 5월 22일 (토)" */
fun formatFullDate(utcMillis: Long): String = utcFormat("yyyy년 M월 d일 (E)").format(Date(utcMillis))

/** "5월 22일 (토)" — 지출 내역 날짜 헤더 */
fun formatDayHeader(utcMillis: Long): String = utcFormat("M월 d일 (E)").format(Date(utcMillis))

/** "5월 10일" — 업체 메타의 잔금일 */
fun formatShortDate(utcMillis: Long): String = utcFormat("M월 d일").format(Date(utcMillis))

/** "2027.05.22" — 다른 해도 섞일 수 있는 지출 목록 */
fun formatDotDate(utcMillis: Long): String = utcFormat("yyyy.MM.dd").format(Date(utcMillis))

/**
 * 숫자만 있는 전화번호에 하이픈을 붙인다. 입력 중인 짧은 번호도 자연스럽게 끊는다.
 * 02(서울)는 2자리 국번, 나머지는 3자리.
 */
fun formatPhone(digits: String): String {
    val d = digits.filter { it.isDigit() }
    if (d.startsWith("02")) {
        return when {
            d.length <= 2 -> d
            d.length <= 5 -> "02-${d.substring(2)}"
            d.length <= 9 -> "02-${d.substring(2, 5)}-${d.substring(5)}"
            else -> "02-${d.substring(2, 6)}-${d.substring(6)}"
        }
    }
    return when {
        d.length <= 3 -> d
        d.length <= 6 -> "${d.substring(0, 3)}-${d.substring(3)}"
        d.length <= 10 -> "${d.substring(0, 3)}-${d.substring(3, 6)}-${d.substring(6)}"
        else -> "${d.substring(0, 3)}-${d.substring(3, 7)}-${d.substring(7)}"
    }
}

/** 전화번호 최대 자릿수 (휴대폰 11자리). */
const val PHONE_MAX_DIGITS = 11

/** 입력칸에는 숫자만 두고 화면에만 하이픈을 붙인다 — 커서가 하이픈에 걸려 지워지지 않는 일이 없게. */
val PhoneTransformation = object : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatPhone(text.text)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var seen = 0
                formatted.forEachIndexed { i, c ->
                    if (c != '-') {
                        seen++
                        if (seen == offset) return i + 1
                    }
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset).count { it != '-' }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
