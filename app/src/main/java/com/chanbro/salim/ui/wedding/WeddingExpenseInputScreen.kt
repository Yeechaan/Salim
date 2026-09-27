package com.chanbro.salim.ui.wedding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.model.WeddingExpense
import com.chanbro.salim.domain.model.WeddingItem
import com.chanbro.salim.domain.model.WeddingOverview
import com.chanbro.salim.ui.common.AmountInput
import com.chanbro.salim.ui.common.CategoryIconBadge
import com.chanbro.salim.ui.common.DatePickerModal
import com.chanbro.salim.ui.common.FieldDivider
import com.chanbro.salim.ui.common.FieldRow
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import com.chanbro.salim.ui.common.SubScreenTopBar
import com.chanbro.salim.ui.common.TextInputField
import com.chanbro.salim.ui.common.formatDate
import com.chanbro.salim.ui.common.todayUtcMillis

// ---------------------------------------------------------------------------
// 웨딩 지출 기록 / 수정 (wedding.md 12-5) — 가계부 지출 입력과 같은 레이아웃, 입력 항목만 다르다.
// ---------------------------------------------------------------------------

/**
 * @param expenseId 있으면 수정.
 * @param vendorId 업체 상세에서 들어올 때 미리 채울 업체.
 */
@Composable
fun WeddingExpenseInputScreen(
    onClose: () -> Unit,
    onDone: () -> Unit,
    expenseId: String? = null,
    vendorId: String? = null,
    viewModel: WeddingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 프리필을 받기 전에는 그리지 않는다 — 빈 값으로 한 번 그려지면 입력 상태가 빈 값으로 굳는다.
    if (state.loading) return
    val overview = state.overview
    val baseline = remember(expenseId) { expenseId?.let { id -> overview.expenses.firstOrNull { it.id == id } } }
    if (expenseId != null && baseline == null) {
        // 목록에서 눌렀는데 그새 상대가 지웠다.
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val prefillVendor = remember(vendorId) { vendorId?.let(overview::vendor) }

    WeddingExpenseContent(
        baseline = baseline,
        prefillVendorId = prefillVendor?.id,
        prefillItem = prefillVendor?.item,
        overview = overview,
        state = state,
        onClose = onClose,
        onSave = { amount, date, item, vendor, spender, memo ->
            viewModel.saveExpense(baseline, amount, date, item, vendor, spender, memo)
            onDone()
        },
        onDelete = {
            baseline?.let { viewModel.deleteExpense(it.id) }
            onDone()
        },
    )
}

@Composable
private fun WeddingExpenseContent(
    baseline: WeddingExpense?,
    prefillVendorId: String?,
    prefillItem: WeddingItem?,
    overview: WeddingOverview,
    state: WeddingUiState,
    onClose: () -> Unit,
    onSave: (Long, Long, WeddingItem, String?, Spender, String) -> Unit,
    onDelete: () -> Unit,
) {
    val isEdit = baseline != null
    var amountDigits by rememberSaveable { mutableStateOf(baseline?.amount?.toString().orEmpty()) }
    var dateMillis by rememberSaveable { mutableLongStateOf(baseline?.dateMillis ?: todayUtcMillis()) }
    var item by rememberSaveable { mutableStateOf(baseline?.item ?: prefillItem) }
    // 지운 업체를 가리키던 지출은 "선택 안 함"으로 연다.
    var vendorId by rememberSaveable {
        mutableStateOf(baseline?.let { overview.vendorOf(it)?.id } ?: prefillVendorId)
    }
    var spender by rememberSaveable { mutableStateOf(baseline?.spender ?: Spender.ME) }
    var memo by rememberSaveable { mutableStateOf(baseline?.memo.orEmpty()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showVendorSheet by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    val amount = amountDigits.toLongOrNull() ?: 0L
    val selectedItem = item
    val vendor = vendorId?.let(overview::vendor)

    Column(modifier = Modifier.fillMaxSize().background(SalimTokens.Background)) {
        SubScreenTopBar(
            title = stringResource(if (isEdit) R.string.wedding_expense_edit else R.string.wedding_expense_add),
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AmountInput(digits = amountDigits, onDigitsChange = { amountDigits = it.filter(Char::isDigit).take(10) })
            SalimCard(cornerRadius = 20.dp) {
                // 시간은 받지 않는다 — 같은 날짜 안에서는 적은 순서 (12-5)
                FieldRow(stringResource(R.string.wedding_date), formatDate(dateMillis)) { showDatePicker = true }
                FieldDivider()
                Column(modifier = Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.wedding_item), style = SalimType.bodyMd, color = SalimTokens.TextMuted)
                    WeddingItemChips(selected = selectedItem, onSelect = { item = it })
                }
                FieldDivider()
                FieldRow(
                    stringResource(R.string.wedding_expense_vendor),
                    vendor?.name ?: stringResource(R.string.wedding_expense_vendor_none),
                ) { showVendorSheet = true }
                FieldDivider()
                if (state.connected) {
                    Column(modifier = Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.wedding_spender), style = SalimType.bodyMd, color = SalimTokens.TextMuted)
                        SpenderChips(names = state.names, selected = spender, onSelect = { spender = it })
                    }
                    FieldDivider()
                }
                TextInputField(
                    label = stringResource(R.string.wedding_memo),
                    value = memo,
                    onValueChange = { memo = it },
                    hint = stringResource(R.string.wedding_memo_hint),
                )
            }
        }
        SaveButton(
            enabled = amount > 0 && selectedItem != null,
            onClick = { if (selectedItem != null) onSave(amount, dateMillis, selectedItem, vendor?.id, spender, memo) },
            label = stringResource(if (isEdit) R.string.common_edit_done else R.string.common_save),
        )
    }

    if (showDatePicker) {
        DatePickerModal(
            initialMillis = dateMillis,
            onConfirm = {
                dateMillis = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showVendorSheet) {
        VendorPickerSheet(
            overview = overview,
            selectedId = vendor?.id,
            onDismiss = { showVendorSheet = false },
            onSelect = { picked ->
                vendorId = picked?.id
                // 업체를 고르면 웨딩 항목을 그 업체 항목으로 맞춘다. 이후 항목을 바꿔도 업체는 그대로. (12-5)
                if (picked != null) item = picked.item
                showVendorSheet = false
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
        )
    }
}

/** 맨 위 "선택 안 함" + 웨딩 항목별로 묶은 업체. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VendorPickerSheet(
    overview: WeddingOverview,
    selectedId: String?,
    onDismiss: () -> Unit,
    onSelect: (com.chanbro.salim.domain.model.WeddingVendor?) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = SalimTokens.CardSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.wedding_expense_vendor),
                style = SalimType.titleLg,
                color = SalimTokens.TextPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            PickerRow(stringResource(R.string.wedding_expense_vendor_none), selected = selectedId == null) { onSelect(null) }
            val groups = overview.vendorGroups
            if (groups.isEmpty()) {
                Text(
                    stringResource(R.string.wedding_expense_vendor_empty),
                    style = SalimType.bodyMd,
                    color = SalimTokens.TextMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                )
            }
            groups.forEach { (item, vendors) ->
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryIconBadge(icon = item.icon(), colorKey = item.colorKey(), size = 24.dp)
                    Text(stringResource(item.labelRes()), style = SalimType.labelMd, color = SalimTokens.TextMuted)
                }
                vendors.forEach { v -> PickerRow(v.name, selected = v.id == selectedId) { onSelect(v) } }
            }
        }
    }
}

@Composable
private fun PickerRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = SalimType.bodyLg.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) SalimTokens.Accent else SalimTokens.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = SalimTokens.Accent)
    }
}
