package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "estudihambre_prefs")

class PreferencesManager(private val context: Context) {
    private val currencySymbolKey = stringPreferencesKey("currency_symbol")
    private val activeLogicalDateKey = stringPreferencesKey("active_logical_date")
    private val unlockedAchievementsKey = stringSetPreferencesKey("unlocked_weight_achievements")

    val currencySymbol: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[currencySymbolKey] ?: "$"
    }

    val activeLogicalDateOverride: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[activeLogicalDateKey]
    }

    val unlockedAchievements: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[unlockedAchievementsKey] ?: emptySet()
    }

    suspend fun getUnlockedAchievementsOnce(): Set<String> {
        return unlockedAchievements.first()
    }

    suspend fun unlockAchievement(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[unlockedAchievementsKey] ?: emptySet()
            prefs[unlockedAchievementsKey] = current + id
        }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { prefs ->
            prefs[currencySymbolKey] = symbol.ifBlank { "$" }
        }
    }

    suspend fun setActiveLogicalDate(date: String?) {
        context.dataStore.edit { prefs ->
            if (date == null) prefs.remove(activeLogicalDateKey)
            else prefs[activeLogicalDateKey] = date
        }
    }
}
