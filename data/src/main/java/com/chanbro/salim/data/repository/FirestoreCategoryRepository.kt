package com.chanbro.salim.data.repository

import android.util.Log
import com.chanbro.salim.domain.model.Category
import com.chanbro.salim.domain.model.CategoryColors
import com.chanbro.salim.domain.model.DefaultCategories
import com.chanbro.salim.domain.repository.CategoryRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore 기반 카테고리 저장소. (firestore-schema.md {categories})
 *
 * 경로는 다른 가계부 데이터와 같이 UserScope가 정한다 — 연결하면 커플이 한 목록을 함께 쓰고,
 * 연결 전 개인 목록은 옮기지 않는다(개인/공동 이중 경로 원칙).
 */
@Singleton
class FirestoreCategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userScope: UserScope,
) : CategoryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<List<Category>> = userScope.scope.flatMapLatest { scope ->
        if (scope == null) return@flatMapLatest flowOf(DefaultCategories.all)
        callbackFlow {
            // 메타데이터 변경도 받는다 — 캐시에서 온 빈 목록 뒤에 "서버도 비었다"는 확인이
            // 문서 변화 없이 메타데이터로만 오기 때문에, 이걸 놓치면 시드할 기회가 없다.
            val listener = collection(scope.doc).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toCategory() }.getOrNull() }
                    ?.sortedBy { it.order }
                    .orEmpty()
                if (categories.isNotEmpty()) {
                    trySend(categories)
                    return@addSnapshotListener
                }
                // 비어 있으면 기본 목록으로 먼저 그린다 — 첫 진입에 칩이 비어 보이지 않게.
                trySend(DefaultCategories.all)
                // 캐시가 비어 있는 것만으로는 서버도 비었는지 알 수 없다. 서버가 확인해 준 빈 목록일 때만 심는다.
                // id가 고정값이라 두 사람이 동시에 심어도 같은 문서를 덮어쓸 뿐이다.
                if (snapshot?.metadata?.isFromCache == false) {
                    seed(scope.doc)
                }
            }
            awaitClose { listener.remove() }
        }
    }.distinctUntilChanged()

    override suspend fun save(categories: List<Category>) {
        val scope = userScope.requireScope()
        write(scope.doc, categories).await()
    }

    private fun seed(scopeDoc: DocumentReference) {
        write(scopeDoc, DefaultCategories.all, isDefault = true)
            .addOnFailureListener { Log.w(TAG, "기본 카테고리 시드 실패", it) }
    }

    private fun write(scopeDoc: DocumentReference, categories: List<Category>, isDefault: Boolean? = null) =
        firestore.batch().apply {
            categories.forEach { category ->
                val data = buildMap {
                    put("name", category.name)
                    put("icon", category.iconKey)
                    put("color", category.colorKey)
                    put("fixed", category.fixed)
                    put("order", category.order)
                    if (isDefault != null) put("isDefault", isDefault)
                }
                // merge — 이름만 바꿔 저장할 때 isDefault 같은 다른 필드를 지우지 않는다.
                set(collection(scopeDoc).document(category.id), data, SetOptions.merge())
            }
        }.commit()

    private fun collection(scopeDoc: DocumentReference): CollectionReference =
        scopeDoc.collection("categories")

    private fun DocumentSnapshot.toCategory() = Category(
        id = id,
        name = getString("name") ?: error("이름 없는 카테고리 문서: $id"),
        iconKey = getString("icon") ?: Category.CUSTOM_ICON,
        // color 필드 이전에 심긴 기본 카테고리는 기본 색으로 읽는다. 다음 저장 때 필드가 채워진다.
        colorKey = getString("color") ?: DefaultCategories.defaultColorKey(id) ?: CategoryColors.keys.last(),
        fixed = getBoolean("fixed") ?: false,
        order = getLong("order")?.toInt() ?: Int.MAX_VALUE,
    )

    private companion object {
        const val TAG = "SalimCategory"
    }
}
