package com.chanbro.salim.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.chanbro.salim.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding",
)

/**
 * 온보딩 노출 여부를 기기 로컬에 저장. (PRD 1.)
 * 공유 데이터가 아니므로 Firestore가 아닌 DataStore를 쓴다. (CLAUDE.md 2번 data/local)
 */
class OnboardingPreferences(
    private val context: Context,
) : OnboardingRepository {

    override val completed: Flow<Boolean> =
        context.onboardingDataStore.data.map { it[KEY_COMPLETED] == true }

    override suspend fun markCompleted() {
        context.onboardingDataStore.edit { it[KEY_COMPLETED] = true }
    }

    private companion object {
        val KEY_COMPLETED = booleanPreferencesKey("completed")
    }
}
