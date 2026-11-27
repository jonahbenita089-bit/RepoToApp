package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "repoforge_prefs")

data class UserPreferencesState(
    val githubToken: String = "",
    val themePreference: ThemePreference = ThemePreference.DARK,
    val autoPollRuns: Boolean = true,
)

class TokenPreferences(private val context: Context) {

    private object Keys {
        val GITHUB_TOKEN = stringPreferencesKey("github_pat")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_POLL_RUNS = booleanPreferencesKey("auto_poll_runs")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemePreference.DARK.name
        val theme = runCatching { ThemePreference.valueOf(themeStr) }
            .getOrDefault(ThemePreference.DARK)
        UserPreferencesState(
            githubToken = prefs[Keys.GITHUB_TOKEN] ?: "",
            themePreference = theme,
            autoPollRuns = prefs[Keys.AUTO_POLL_RUNS] ?: true,
        )
    }

    suspend fun saveGithubToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GITHUB_TOKEN] = token.trim()
        }
    }

    suspend fun clearGithubToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.GITHUB_TOKEN)
        }
    }

    suspend fun saveThemePreference(theme: ThemePreference) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = theme.name
        }
    }

    suspend fun setAutoPollRuns(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_POLL_RUNS] = enabled
        }
    }
}
