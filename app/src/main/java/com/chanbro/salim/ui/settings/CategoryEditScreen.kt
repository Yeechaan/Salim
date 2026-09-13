package com.chanbro.salim.ui.settings

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.Category
import com.chanbro.salim.domain.model.CategoryNameError
import com.chanbro.salim.domain.model.DefaultCategories
import com.chanbro.salim.ui.common.CategoryChip
import com.chanbro.salim.ui.common.CategoryIconBadge
import com.chanbro.salim.ui.common.ChipFlowRow
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType

// ---------------------------------------------------------------------------
// 카테고리 수정 (settings.md 3-2, PRD 7)
// 고정 5개 교체(더보기 항목의 "고정으로"), 더보기 항목 추가, 모든 항목 이름 수정.
// 삭제/사용 중지는 아직 범위 밖이다.
// ---------------------------------------------------------------------------

@Composable
fun CategoryEditScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var renamingId by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var promotingId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SalimTokens.Background),
    ) {
        CategoryEditTopBar(onClose)
        // 로딩 전에는 목록을 그리지 않는다 (빈 카드가 잠깐 보이는 것 방지).
        val loaded = state ?: return@Column
        CategoryEditContent(
            state = loaded,
            onRename = { renamingId = it.id },
            onPromote = { promotingId = it.id },
            onAdd = { adding = true },
        )
    }

    val loaded = state ?: return
    loaded.all.firstOrNull { it.id == renamingId }?.let { target ->
        CategoryNameDialog(
            title = stringResource(R.string.category_edit_rename_title),
            initialName = target.name,
            validate = { viewModel.validate(it, excludingId = target.id) },
            onConfirm = { name ->
                viewModel.rename(target.id, name)
                renamingId = null
            },
            onDismiss = { renamingId = null },
        )
    }
    if (adding) {
        CategoryNameDialog(
            title = stringResource(R.string.category_edit_add_title),
            initialName = "",
            validate = { viewModel.validate(it) },
            onConfirm = { name ->
                viewModel.add(name)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }
    loaded.more.firstOrNull { it.id == promotingId }?.let { target ->
        SwapFixedSheet(
            promoting = target,
            fixed = loaded.fixed,
            onSelect = { demoted ->
                viewModel.swapFixed(fixedId = demoted.id, moreId = target.id)
                promotingId = null
            },
            onDismiss = { promotingId = null },
        )
    }
}

@Composable
private fun CategoryEditContent(
    state: CategoryEditUiState,
    onRename: (Category) -> Unit,
    onPromote: (Category) -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(R.string.category_edit_guide),
            style = SalimType.bodySm,
            color = SalimTokens.TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )

        SectionLabel(stringResource(R.string.category_edit_fixed), Modifier.padding(top = 8.dp))
        SalimCard(cornerRadius = 20.dp, contentPadding = 4.dp) {
            state.fixed.forEachIndexed { index, category ->
                if (index > 0) RowDivider()
                CategoryRow(category = category, onClick = { onRename(category) })
            }
        }

        SectionLabel(stringResource(R.string.category_edit_more), Modifier.padding(top = 12.dp))
        SalimCard(cornerRadius = 20.dp, contentPadding = 4.dp) {
            state.more.forEach { category ->
                CategoryRow(category = category, onClick = { onRename(category) }) {
                    TextButton(onClick = { onPromote(category) }) {
                        Text(
                            stringResource(R.string.category_edit_to_fixed),
                            style = SalimType.bodyMd,
                            color = SalimTokens.Accent,
                        )
                    }
                }
                RowDivider()
            }
            Text(
                stringResource(R.string.category_edit_add),
                style = SalimType.bodyLg,
                color = SalimTokens.Accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun CategoryEditTopBar(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = SalimTokens.TextPrimary,
            )
        }
        Text(stringResource(R.string.category_edit_title), style = SalimType.titleLg, color = SalimTokens.TextPrimary)
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = SalimType.labelMd, color = SalimTokens.TextMuted, modifier = modifier.padding(horizontal = 4.dp))
}

/** 줄 전체를 누르면 이름 수정. 오른쪽 끝에 줄마다 다른 동작([trailing])을 붙일 수 있다. */
@Composable
private fun CategoryRow(
    category: Category,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIconBadge(category.iconKey, category.colorKey, size = 36.dp)
        Text(
            category.name,
            style = SalimType.bodyLg,
            color = SalimTokens.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(SalimTokens.Divider),
    )
}

/** 추가·이름 수정 공용 입력 창. 확인을 눌렀을 때 검증하고, 통과해야 닫힌다. */
@Composable
private fun CategoryNameDialog(
    title: String,
    initialName: String,
    validate: (String) -> CategoryNameError?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var error by rememberSaveable { mutableStateOf<CategoryNameError?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = SalimType.titleLg) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicTextField(
                    value = name,
                    // 최대 글자 수에서 더 입력되지 않게 막는다 — 확인을 누른 뒤에야 넘쳤다는 걸 알게 하지 않는다.
                    onValueChange = {
                        name = it.take(DefaultCategories.NAME_MAX_LENGTH)
                        error = null
                    },
                    textStyle = SalimType.bodyLg.copy(color = SalimTokens.TextPrimary),
                    singleLine = true,
                    cursorBrush = SolidColor(SalimTokens.Accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SalimTokens.Background)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (name.isEmpty()) {
                                    Text(
                                        stringResource(R.string.category_edit_name_hint),
                                        style = SalimType.bodyLg,
                                        color = SalimTokens.TextMuted,
                                    )
                                }
                                inner()
                            }
                            Text(
                                "${name.length}/${DefaultCategories.NAME_MAX_LENGTH}",
                                style = SalimType.labelMd,
                                color = SalimTokens.TextMuted,
                            )
                        }
                    },
                )
                error?.let {
                    Text(
                        when (it) {
                            CategoryNameError.BLANK -> stringResource(R.string.category_error_blank)
                            CategoryNameError.TOO_LONG ->
                                stringResource(R.string.category_error_too_long, DefaultCategories.NAME_MAX_LENGTH)
                            CategoryNameError.DUPLICATE -> stringResource(R.string.category_error_duplicate)
                        },
                        style = SalimType.bodySm,
                        color = SalimTokens.Warning,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = validate(name)
                    if (result == null) onConfirm(name.trim()) else error = result
                },
                enabled = name.isNotBlank(),
            ) {
                Text(
                    stringResource(R.string.common_confirm),
                    color = if (name.isNotBlank()) SalimTokens.Accent else SalimTokens.TextMuted,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = SalimTokens.TextMuted)
            }
        },
        containerColor = SalimTokens.CardSurface,
    )
}

/** 올릴 더보기 항목이 정해진 상태에서, 내릴 고정 항목을 고른다. 고정 개수는 늘 5개로 유지된다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapFixedSheet(
    promoting: Category,
    fixed: List<Category>,
    onSelect: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = SalimTokens.CardSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.category_edit_swap_title),
                style = SalimType.titleLg,
                color = SalimTokens.TextPrimary,
            )
            Text(
                stringResource(R.string.category_edit_swap_body, promoting.name),
                style = SalimType.bodyMd,
                color = SalimTokens.TextMuted,
            )
            ChipFlowRow(modifier = Modifier.padding(top = 8.dp)) {
                fixed.forEach { category ->
                    CategoryChip(
                        iconKey = category.iconKey,
                        colorKey = category.colorKey,
                        label = category.name,
                        selected = false,
                        onClick = { onSelect(category) },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 프리뷰
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CategoryEditScreenPreview() {
    SalimTheme {
        Column(Modifier.background(SalimTokens.Background)) {
            CategoryEditTopBar(onClose = {})
            CategoryEditContent(
                state = CategoryEditUiState(
                    fixed = DefaultCategories.all.filter { it.fixed },
                    more = DefaultCategories.all.filterNot { it.fixed },
                ),
                onRename = {},
                onPromote = {},
                onAdd = {},
            )
        }
    }
}
