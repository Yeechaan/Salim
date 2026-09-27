package com.chanbro.salim.ui.wedding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.VendorStatus
import com.chanbro.salim.domain.model.WeddingItem
import com.chanbro.salim.domain.model.WeddingVendor
import com.chanbro.salim.ui.common.ChipFlowRow
import com.chanbro.salim.ui.common.DatePickerModal
import com.chanbro.salim.ui.common.FieldDivider
import com.chanbro.salim.ui.common.FieldRow
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimChip
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import com.chanbro.salim.ui.common.SubScreenTopBar
import com.chanbro.salim.ui.common.TextInputField
import com.chanbro.salim.ui.common.ThousandsTransformation
import com.chanbro.salim.ui.common.formatDate
import com.chanbro.salim.ui.common.formatWon
import com.chanbro.salim.ui.common.todayUtcMillis

// ---------------------------------------------------------------------------
// 업체 상세 (wedding.md 12-7)
// ---------------------------------------------------------------------------

@Composable
fun VendorDetailScreen(
    vendorId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRecordExpense: () -> Unit,
    onExpenseClick: (String) -> Unit,
    viewModel: WeddingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading) return
    val overview = state.overview
    val vendor = overview.vendor(vendorId)
    if (vendor == null) {
        // 보고 있는 중에 상대방이 지웠다 — 업체 목록으로.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(SalimTokens.Background)) {
        SubScreenTopBar(
            title = vendor.name,
            onBack = onBack,
            action = {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.common_edit), tint = SalimTokens.TextPrimary)
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 정보 카드 — 비어 있는 값의 줄은 숨긴다.
            SalimCard(cornerRadius = 24.dp) {
                LabelValueRow(stringResource(R.string.wedding_item), stringResource(vendor.item.labelRes()))
                LabelValueRow(stringResource(R.string.wedding_vendor_status), stringResource(vendor.status.labelRes()))
                vendor.phone?.let { phone ->
                    val callLabel = stringResource(R.string.wedding_vendor_phone_call)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            // 다이얼 화면에 번호만 채워 연다 — 바로 걸지 않는다. (12-7)
                            .clickable {
                                runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
                            }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.wedding_vendor_phone), style = SalimType.bodyMd, color = SalimTokens.TextMuted)
                        Box(Modifier.weight(1f))
                        Text(formatPhone(phone), style = SalimType.bodyMd.copy(fontWeight = FontWeight.Medium), color = SalimTokens.Accent)
                        Icon(Icons.Filled.Call, contentDescription = callLabel, tint = SalimTokens.Accent, modifier = Modifier.size(18.dp))
                    }
                }
                vendor.balanceDueMillis?.let {
                    LabelValueRow(stringResource(R.string.wedding_vendor_balance_due), formatFullDate(it))
                }
                vendor.memo?.let { memo ->
                    Column(modifier = Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.wedding_memo), style = SalimType.bodyMd, color = SalimTokens.TextMuted)
                        Text(memo, style = SalimType.bodyMd, color = SalimTokens.TextPrimary)
                    }
                }
            }

            SalimCard(cornerRadius = 24.dp) {
                val spent = overview.spentFor(vendor.id)
                vendor.contractAmount?.let {
                    LabelValueRow(stringResource(R.string.wedding_vendor_contract_amount), formatWon(it))
                }
                LabelValueRow(stringResource(R.string.wedding_vendor_spent), formatWon(spent))
                overview.remainingFor(vendor)?.let { remaining ->
                    if (remaining >= 0) {
                        LabelValueRow(stringResource(R.string.wedding_remaining), formatWon(remaining))
                    } else {
                        // 계약보다 더 썼다 — 지출 예정에는 0으로 잡힌다.
                        LabelValueRow(
                            stringResource(R.string.wedding_remaining),
                            stringResource(R.string.wedding_vendor_over, formatWon(-remaining)),
                            valueColor = SalimTokens.Warning,
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.wedding_vendor_record_expense),
                style = SalimType.bodyLg.copy(fontWeight = FontWeight.Bold),
                color = SalimTokens.Accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SalimTokens.AccentSoft)
                    .clickable(onClick = onRecordExpense)
                    .padding(vertical = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            val expenses = overview.expensesOf(vendor.id)
            if (expenses.isEmpty()) {
                Text(
                    stringResource(R.string.wedding_expense_empty),
                    style = SalimType.bodyMd,
                    color = SalimTokens.TextMuted,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            } else {
                SalimCard(cornerRadius = 20.dp, contentPadding = 8.dp) {
                    expenses.forEachIndexed { index, expense ->
                        if (index > 0) ThinDivider(Modifier.padding(horizontal = 12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onExpenseClick(expense.id) }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    expense.memo?.takeIf { it.isNotBlank() } ?: stringResource(expense.item.labelRes()),
                                    style = SalimType.bodyLg,
                                    color = SalimTokens.TextPrimary,
                                )
                                Text(formatDotDate(expense.dateMillis), style = SalimType.bodySm, color = SalimTokens.TextMuted)
                            }
                            Text(
                                formatWon(expense.amount),
                                style = SalimType.bodyLg.copy(fontWeight = FontWeight.Medium),
                                color = SalimTokens.TextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 업체 추가 / 수정 (wedding.md 12-8) — 입력 항목이 많아 시트 대신 전체 화면
// ---------------------------------------------------------------------------

/**
 * @param vendorId 있으면 수정.
 * @param onSaved 추가 → 업체 목록, 수정 → 업체 상세로 (둘 다 한 단계 뒤로).
 * @param onDeleted 삭제 후 업체 목록으로 (상세를 건너뛴다).
 */
@Composable
fun VendorInputScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    vendorId: String? = null,
    viewModel: WeddingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading) return
    val overview = state.overview
    val baseline = remember(vendorId) { vendorId?.let(overview::vendor) }
    if (vendorId != null && baseline == null) {
        LaunchedEffect(Unit) { onDeleted() }
        return
    }
    VendorInputContent(
        baseline = baseline,
        linkedExpenseCount = baseline?.let { overview.expensesOf(it.id).size } ?: 0,
        onClose = onClose,
        onSave = { name, item, status, phone, contract, due, memo ->
            viewModel.saveVendor(baseline, name, item, status, phone, contract, due, memo)
            onSaved()
        },
        onDelete = {
            baseline?.let { viewModel.deleteVendor(it.id) }
            onDeleted()
        },
    )
}

@Composable
private fun VendorInputContent(
    baseline: WeddingVendor?,
    linkedExpenseCount: Int,
    onClose: () -> Unit,
    onSave: (String, WeddingItem, VendorStatus, String, Long?, Long?, String) -> Unit,
    onDelete: () -> Unit,
) {
    val isEdit = baseline != null
    var name by rememberSaveable { mutableStateOf(baseline?.name.orEmpty()) }
    var item by rememberSaveable { mutableStateOf(baseline?.item) }
    var status by rememberSaveable { mutableStateOf(baseline?.status ?: VendorStatus.CONSULTING) }
    var phone by rememberSaveable { mutableStateOf(baseline?.phone.orEmpty()) }
    var contractDigits by rememberSaveable { mutableStateOf(baseline?.contractAmount?.toString().orEmpty()) }
    var balanceDue by rememberSaveable { mutableStateOf(baseline?.balanceDueMillis) }
    var memo by rememberSaveable { mutableStateOf(baseline?.memo.orEmpty()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val selectedItem = item

    Column(modifier = Modifier.fillMaxSize().background(SalimTokens.Background)) {
        SubScreenTopBar(
            title = stringResource(if (isEdit) R.string.wedding_vendor_edit else R.string.wedding_vendor_add),
            onBack = onClose,
            action = if (isEdit) {
                {
                    IconButton(onClick = { confirmingDelete = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.common_delete), tint = SalimTokens.TextMuted)
                    }
                }
            } else {
                null
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            SalimCard(cornerRadius = 20.dp) {
                TextInputField(
                    label = stringResource(R.string.wedding_vendor_name),
                    value = name,
                    onValueChange = { if (it.length <= WeddingVendor.NAME_MAX_LENGTH) name = it },
                    hint = stringResource(R.string.wedding_vendor_name_hint),
                )
                FieldDivider()
                Column(modifier = Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.wedding_item), style = SalimType.bodyMd, color = SalimTokens.TextMuted)
                    WeddingItemChips(selected = selectedItem, onSelect = { item = it })
                }
                FieldDivider()
                Column(modifier = Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.wedding_vendor_status), style = SalimType.bodyMd, color = SalimTokens.TextMuted)
                    ChipFlowRow {
                        VendorStatus.entries.forEach { option ->
                            SalimChip(
                                label = stringResource(option.labelRes()),
                                selected = option == status,
                                onClick = { status = option },
                            )
                        }
                    }
                }
                FieldDivider()
                TextInputField(
                    label = stringResource(R.string.wedding_vendor_phone),
                    value = phone,
                    // 숫자만 들고 있고 하이픈은 화면에만 붙인다.
                    onValueChange = { phone = it.filter(Char::isDigit).take(PHONE_MAX_DIGITS) },
                    hint = stringResource(R.string.wedding_vendor_phone_hint),
                    keyboardType = KeyboardType.Phone,
                    visualTransformation = PhoneTransformation,
                )
                FieldDivider()
                TextInputField(
                    label = stringResource(R.string.wedding_vendor_contract_amount),
                    value = contractDigits,
                    onValueChange = { contractDigits = it.filter(Char::isDigit).take(10) },
                    hint = stringResource(R.string.wedding_vendor_contract_amount_hint),
                    keyboardType = KeyboardType.Number,
                    visualTransformation = ThousandsTransformation,
                )
                FieldDivider()
                FieldRow(
                    stringResource(R.string.wedding_vendor_balance_due),
                    balanceDue?.let(::formatDate) ?: stringResource(R.string.wedding_date_none),
                ) { showDatePicker = true }
                FieldDivider()
                TextInputField(
                    label = stringResource(R.string.wedding_memo),
                    value = memo,
                    onValueChange = { memo = it },
                    hint = stringResource(R.string.wedding_memo_hint),
                    singleLine = false,
                )
            }
        }
        SaveButton(
            enabled = name.isNotBlank() && selectedItem != null,
            onClick = {
                if (selectedItem != null) {
                    onSave(name, selectedItem, status, phone, contractDigits.toLongOrNull()?.takeIf { it > 0 }, balanceDue, memo)
                }
            },
            label = stringResource(if (isEdit) R.string.common_edit_done else R.string.common_save),
        )
    }

    if (showDatePicker) {
        DatePickerModal(
            initialMillis = balanceDue ?: todayUtcMillis(),
            onConfirm = {
                balanceDue = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            clearLabel = if (balanceDue != null) stringResource(R.string.wedding_date_none) else null,
            onClear = {
                balanceDue = null
                showDatePicker = false
            },
        )
    }
    if (confirmingDelete) {
        DeleteConfirmDialog(
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
            // 지출이 있으면 "N건은 남아요", 없으면 기본 문구 (12-8)
            body = if (linkedExpenseCount > 0) {
                stringResource(R.string.wedding_vendor_delete_body_with_expenses, linkedExpenseCount)
            } else {
                stringResource(R.string.common_delete_confirm_body)
            },
        )
    }
}
