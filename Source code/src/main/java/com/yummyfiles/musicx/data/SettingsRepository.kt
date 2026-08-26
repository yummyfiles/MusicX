package com.yummyfiles.musicx.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yummyfiles.musicx.ui.theme.CustomTheme
import com.yummyfiles.musicx.ui.theme.ThemeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_state")
    private val generalSettingsKey = stringPreferencesKey("general_settings")
    private val savedThemesKey = stringPreferencesKey("saved_themes")

    val themeState: Flow<ThemeState> = context.dataStore.data.map { preferences ->
        val json = preferences[themeKey]
        if (json != null) {
            try {
                Json.decodeFromString<ThemeState>(json)
            } catch (_: Exception) {
                ThemeState()
            }
        } else {
            ThemeState()
        }
    }

    val generalSettings: Flow<GeneralSettings> = context.dataStore.data.map { preferences ->
        val json = preferences[generalSettingsKey]
        if (json != null) {
            try {
                Json.decodeFromString<GeneralSettings>(json)
            } catch (_: Exception) {
                GeneralSettings()
            }
        } else {
            GeneralSettings()
        }
    }

    val savedThemes: Flow<List<CustomTheme>> = context.dataStore.data.map { preferences ->
        val json = preferences[savedThemesKey]
        if (json != null) {
            try {
                Json.decodeFromString<List<CustomTheme>>(json)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun updateTheme(update: (ThemeState) -> ThemeState) {
        val current = themeState.first()
        val next = update(current)
        setTheme(next)
    }

    suspend fun updateGeneralSettings(update: (GeneralSettings) -> GeneralSettings) {
        val current = generalSettings.first()
        val next = update(current)
        context.dataStore.edit { preferences ->
            preferences[generalSettingsKey] = Json.encodeToString(next)
        }
    }

    suspend fun setTheme(theme: ThemeState) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = Json.encodeToString(theme)
        }
    }

    suspend fun saveTheme(name: String, state: ThemeState) {
        val currentThemes = savedThemes.first().toMutableList()
        currentThemes.removeAll { it.name == name }
        currentThemes.add(CustomTheme(name, state))
        context.dataStore.edit { preferences ->
            preferences[savedThemesKey] = Json.encodeToString(currentThemes)
        }
    }

    suspend fun deleteTheme(name: String) {
        val currentThemes = savedThemes.first().toMutableList()
        currentThemes.removeAll { it.name == name }
        context.dataStore.edit { preferences ->
            preferences[savedThemesKey] = Json.encodeToString(currentThemes)
        }
    }
}
