package com.devhjs.mathgraphstudy.data.datasource.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** 사용 기록(광고/리뷰 정책용)을 저장하는 DataStore 접근 클래스 */
class UsageDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun read(): Preferences = dataStore.data.first()

    suspend fun write(functionsAdded: Int, reviewRequested: Boolean, lastInterstitialAt: Long) {
        dataStore.edit { prefs ->
            prefs[FUNCTIONS_ADDED] = functionsAdded
            prefs[REVIEW_REQUESTED] = reviewRequested
            prefs[LAST_INTERSTITIAL_AT] = lastInterstitialAt
        }
    }

    companion object {
        val FUNCTIONS_ADDED = intPreferencesKey("functions_added")
        val REVIEW_REQUESTED = booleanPreferencesKey("review_requested")
        val LAST_INTERSTITIAL_AT = longPreferencesKey("last_interstitial_at")
    }
}
