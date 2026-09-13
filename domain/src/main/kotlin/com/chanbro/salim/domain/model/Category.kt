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
    /** 아이콘 키. 기본 카테고리는 각자 고유 키, 사용자가 추가한 항목은 [CUSTOM_ICON]. */
    val iconKey: String,
    /**
     * 색 키([CategoryColors.keys]). 카테고리에 붙어 다녀서 리스트·차트·칩 어디서나 같은 색이다.
     * **고정 항목끼리는 색 계열([CategoryColors.familyOf])이 겹치지 않는다** — 고정 교체 때 맞춰 준다.
     */
    val colorKey: String,
    val fixed: Boolean,
    /** 칩·시트·수정 화면의 표시 순서. 고정/더보기 구역 안에서 이 값으로 정렬한다. */
    val order: Int,
) {
    companion object {
        const val CUSTOM_ICON = "custom"
    }
}

/** 카테고리 색 팔레트. 실제 색 값은 UI(core-ui 토큰)가 키로 매핑한다. */
object CategoryColors {
    /**
     * 기본 11개가 하나씩 쓰고, 마지막 olive는 처음 추가하는 항목 몫으로 남겨 둔다.
     * 순서는 [leastUsed]의 동률 우선순위이기도 하다.
     */
    val keys: List<String> = listOf(
        "peach", "sage", "rose", "lavender", "sky", "mint",
        "butter", "lilac", "clay", "apricot", "warmgray", "olive",
    )

    /**
     * 색 계열. 키가 달라도 눈에는 비슷하게 보이는 것끼리 묶는다(피치·살구, 세이지·올리브, 라벤더·라일락).
     * "고정 항목끼리 색이 겹치지 않는다"는 이 계열 기준이다.
     */
    fun familyOf(colorKey: String): String = when (colorKey) {
        "peach", "apricot" -> "orange"
        "sage", "olive" -> "green"
        "mint" -> "teal"
        "rose" -> "pink"
        "lavender", "lilac" -> "purple"
        "sky" -> "blue"
        "butter" -> "yellow"
        "clay" -> "brown"
        else -> "gray"
    }

    /**
     * 가장 적게 쓰인 색. 동률이면 팔레트 앞쪽. 팔레트가 남아 있으면 아무도 안 쓰는 색이 나온다.
     * @param excludingFamilies 후보에서 뺄 색 계열 (남아 있는 고정 항목들의 계열)
     */
    fun leastUsed(existing: List<Category>, excludingFamilies: Set<String> = emptySet()): String {
        val counts = existing.groupingBy { it.colorKey }.eachCount()
        return keys.filterNot { familyOf(it) in excludingFamilies }.minBy { counts[it] ?: 0 }
    }
}

object DefaultCategories {
    const val FIXED_COUNT = 5

    /** 지출 입력 칩 한 줄에 여러 개가 들어가야 해서 짧게 제한한다. */
    const val NAME_MAX_LENGTH = 8

    /**
     * 카테고리 문서가 하나도 없는 경로(최초 진입·연결 직후)에 심는 목록. 11개 모두 색이 다르고,
     * 고정 5개는 계열이 모두 다르고(주황·초록·분홍·보라·파랑), 칩 순서가 무지개처럼 늘어서지 않게 섞었다.
     * id가 고정값이라 두 사람이 동시에 심어도 같은 문서를 덮어쓸 뿐 중복이 생기지 않는다.
     */
    val all: List<Category> = listOf(
        Category("food", "식비", "food", "peach", fixed = true, order = 0),
        Category("cafe", "카페", "cafe", "sage", fixed = true, order = 1),
        Category("shopping", "쇼핑", "shopping", "rose", fixed = true, order = 2),
        Category("culture", "문화", "culture", "lavender", fixed = true, order = 3),
        Category("travel", "여행", "travel", "sky", fixed = true, order = 4),
        Category("transport", "교통", "transport", "mint", fixed = false, order = 5),
        Category("living", "생활", "living", "butter", fixed = false, order = 6),
        Category("health", "의료/건강", "health", "lilac", fixed = false, order = 7),
        Category("housing", "주거/통신", "housing", "clay", fixed = false, order = 8),
        Category("gift", "경조사/선물", "gift", "apricot", fixed = false, order = 9),
        Category("etc", "기타", "etc", "warmgray", fixed = false, order = 10),
    )

    /** 색 필드가 생기기 전에 심긴 기본 카테고리 문서의 색. 기본 id가 아니면 null. */
    fun defaultColorKey(id: String): String? = all.firstOrNull { it.id == id }?.colorKey

    /** 예전 이름이 가리키던 기본 카테고리 id. 모르는 이름이면 null. */
    fun legacyDefaultId(savedName: String): String? = when (savedName) {
        "문화/여가" -> "culture"
        else -> all.firstOrNull { it.name == savedName }?.id
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

/**
 * 지출 수정 화면에서 미리 골라 둘 카테고리. id → 같은 이름 → 예전 기본 이름 순으로 찾는다.
 * id가 없는 예전 지출도 수정할 때 카테고리가 엉뚱하게 바뀌지 않게 하려는 것이다. 못 찾으면 null.
 */
fun List<Category>.findFor(expense: Expense): Category? =
    expense.categoryId?.let { id -> firstOrNull { it.id == id } }
        ?: firstOrNull { it.name == expense.categoryName }
        ?: DefaultCategories.legacyDefaultId(expense.categoryName)?.let { id -> firstOrNull { it.id == id } }

/** 화면에 그릴 지출의 카테고리 이름과 모양. */
data class CategoryLabel(val name: String, val iconKey: String, val colorKey: String)

/**
 * 지출이 가리키는 카테고리를 지금 이름으로 푼다 — 이름을 바꾸면 기존 지출에도 새 이름이 보인다(PRD 7).
 * id가 없는 예전 지출이나 목록에서 찾지 못한 id는 저장해 둔 이름으로 보여주고,
 * 모양은 예전 이름이 가리키던 기본 카테고리(모르면 "기타")를 따른다.
 */
fun List<Category>.labelOf(expense: Expense): CategoryLabel {
    val current = expense.categoryId?.let { id -> firstOrNull { it.id == id } }
    if (current != null) return CategoryLabel(current.name, current.iconKey, current.colorKey)
    val legacy = DefaultCategories.all.firstOrNull { it.id == DefaultCategories.legacyDefaultId(expense.categoryName) }
        ?: DefaultCategories.all.last()
    return CategoryLabel(expense.categoryName, legacy.iconKey, legacy.colorKey)
}
