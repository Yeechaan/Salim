package com.chanbro.salim.domain.model

/**
 * 가계부 카테고리. (PRD 4 지출 입력 / PRD 7 카테고리 수정, firestore-schema.md {categories})
 *
 * 지출 입력에는 [fixed] 항목이 칩으로, 나머지가 "+더보기" 시트에 나온다.
 * 고정 항목은 언제나 [DefaultCategories.FIXED_COUNT]개다 — 교체는 더보기 항목과 자리를 맞바꾸는 것뿐이다.
 */
data class Category(
    val id: String,
    val name: String,
    /** 아이콘/색 키. 기본 카테고리는 각자 고유 키, 사용자가 추가한 항목은 [CUSTOM_ICON]. */
    val iconKey: String,
    val fixed: Boolean,
    /** 칩·시트·수정 화면의 표시 순서. 고정/더보기 구역 안에서 이 값으로 정렬한다. */
    val order: Int,
) {
    companion object {
        const val CUSTOM_ICON = "custom"
    }
}

object DefaultCategories {
    const val FIXED_COUNT = 5

    /** 지출 입력 칩 한 줄에 여러 개가 들어가야 해서 짧게 제한한다. */
    const val NAME_MAX_LENGTH = 8

    /**
     * 카테고리 문서가 하나도 없는 경로(최초 진입·연결 직후)에 심는 목록.
     * id가 고정값이라 두 사람이 동시에 심어도 같은 문서를 덮어쓸 뿐 중복이 생기지 않는다.
     */
    val all: List<Category> = listOf(
        Category("food", "식비", "food", fixed = true, order = 0),
        Category("cafe", "카페", "cafe", fixed = true, order = 1),
        Category("shopping", "쇼핑", "shopping", fixed = true, order = 2),
        Category("culture", "문화", "culture", fixed = true, order = 3),
        Category("travel", "여행", "travel", fixed = true, order = 4),
        Category("transport", "교통", "transport", fixed = false, order = 5),
        Category("living", "생활", "living", fixed = false, order = 6),
        Category("health", "의료/건강", "health", fixed = false, order = 7),
        Category("housing", "주거/통신", "housing", fixed = false, order = 8),
        Category("gift", "경조사/선물", "gift", fixed = false, order = 9),
        Category("etc", "기타", "etc", fixed = false, order = 10),
    )

    /**
     * categoryId 없이 이름만 저장된 예전 지출의 아이콘 키. 이름이 지금과 달랐던 시절의 표기도 받는다.
     * 이름 자체는 저장된 그대로 보여주고, 모양만 기본 카테고리에 맞춘다.
     */
    fun legacyIconKey(savedName: String): String = when (savedName) {
        "문화/여가" -> "culture"
        else -> all.firstOrNull { it.name == savedName }?.iconKey ?: "etc"
    }
}

/** 이름 검증 결과. null이면 통과. */
enum class CategoryNameError { BLANK, TOO_LONG, DUPLICATE }

/**
 * 추가·이름 수정 공통 규칙. 앞뒤 공백은 잘라서 본다.
 * @param excludingId 이름 수정일 때 자기 자신 — 같은 이름으로 다시 저장하는 것은 중복이 아니다.
 */
fun validateCategoryName(
    name: String,
    existing: List<Category>,
    excludingId: String? = null,
): CategoryNameError? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> CategoryNameError.BLANK
        trimmed.length > DefaultCategories.NAME_MAX_LENGTH -> CategoryNameError.TOO_LONG
        existing.any { it.id != excludingId && it.name == trimmed } -> CategoryNameError.DUPLICATE
        else -> null
    }
}

/** 화면에 그릴 지출의 카테고리 이름과 모양. */
data class CategoryLabel(val name: String, val iconKey: String)

/**
 * 지출이 가리키는 카테고리를 지금 이름으로 푼다 — 이름을 바꾸면 기존 지출에도 새 이름이 보인다(PRD 7).
 * id가 없는 예전 지출이나 목록에서 찾지 못한 id는 저장해 둔 이름으로 보여준다.
 */
fun List<Category>.labelOf(expense: Expense): CategoryLabel {
    val current = expense.categoryId?.let { id -> firstOrNull { it.id == id } }
    return if (current != null) {
        CategoryLabel(current.name, current.iconKey)
    } else {
        CategoryLabel(expense.categoryName, DefaultCategories.legacyIconKey(expense.categoryName))
    }
}
