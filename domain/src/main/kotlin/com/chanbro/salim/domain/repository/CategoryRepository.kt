package com.chanbro.salim.domain.repository

import com.chanbro.salim.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * 가계부 카테고리 저장소. (firestore-schema.md {categories} — 미연결=users/{uid}, 연결=couples/{coupleId})
 */
interface CategoryRepository {
    /** [Category.order]순 전체 목록. 경로에 카테고리가 없으면 기본 목록을 심고 그 목록을 내보낸다. */
    fun observe(): Flow<List<Category>>

    /** 여러 항목을 한 번에 저장한다. 고정 교체처럼 두 문서가 함께 바뀌어야 하는 경우를 위해 원자적으로 쓴다. */
    suspend fun save(categories: List<Category>)
}
