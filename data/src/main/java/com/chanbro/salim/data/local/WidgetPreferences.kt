package com.chanbro.salim.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.chanbro.salim.domain.repository.WidgetSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget",
)

/**
 * 홈 화면 위젯 설정을 기기 로컬에 저장. (PRD 7/10-2)
 * 이 기기의 위젯에만 적용되므로 Firestore가 아닌 DataStore를 쓴다. (CLAUDE.md 2번 data/local)
 */
class WidgetPreferences(
    private val context: Context,
) : WidgetSettingsRepository {

    override val showAmount: Flow<Boolean> =
        context.widgetDataStore.data.map { it[KEY_SHOW_AMOUNT] ?: true }

    override suspend fun setShowAmount(show: Boolean) {
        context.widgetDataStore.edit { it[KEY_SHOW_AMOUNT] = show }
    }

    private companion object {
        val KEY_SHOW_AMOUNT = booleanPreferencesKey("show_amount")
    }
}
