package com.example.musicx.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.musicx.ui.theme.CustomTheme
import com.example.musicx.ui.theme.ThemeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val THEME_JSON = stringPreferencesKey("theme_json")
        val SAVED_THEMES_JSON = stringPreferencesKey("saved_themes_json")
        val GENERAL_SETTINGS_JSON = stringPreferencesKey("general_settings_json")
        val YT_API_BASE_URL = stringPreferencesKey("yt_api_base_url")
    }

    val ytApiBaseUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[YT_API_BASE_URL] ?: "http://localhost:5000"
    }

    suspend fun setYtApiBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[YT_API_BASE_URL] = url
        }
    }

    val themeState: Flow<ThemeState> = dataStore.data.map { preferences ->
        try {
            val jsonStr = preferences[THEME_JSON]
            if (jsonStr != null) {
                Json.decodeFromString<ThemeState>(jsonStr)
            } else {
                ThemeState()
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Failed to decode theme", e)
            ThemeState()
        }
    }

    val savedThemes: Flow<List<CustomTheme>> = dataStore.data.map { preferences ->
        try {
            val jsonStr = preferences[SAVED_THEMES_JSON]
            if (jsonStr != null) {
                Json.decodeFromString(ListSerializer(CustomTheme.serializer()), jsonStr)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Failed to decode saved themes", e)
            emptyList()
        }
    }

    val generalSettings: Flow<GeneralSettings> = dataStore.data.map { preferences ->
        try {
            val jsonStr = preferences[GENERAL_SETTINGS_JSON]
            if (jsonStr != null) {
                Json.decodeFromString<GeneralSettings>(jsonStr)
            } else {
                GeneralSettings()
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Failed to decode general settings", e)
            GeneralSettings()
        }
    }

    suspend fun updateTheme(update: (ThemeState) -> ThemeState) {
        dataStore.edit { preferences ->
            val currentJson = preferences[THEME_JSON]
            val currentState = if (currentJson != null) {
                try {
                    Json.decodeFromString<ThemeState>(currentJson)
                } catch (e: Exception) {
                    ThemeState()
                }
            } else {
                ThemeState()
            }
            val newState = update(currentState)
            preferences[THEME_JSON] = Json.encodeToString(newState)
        }
    }
    
    suspend fun updateGeneralSettings(update: (GeneralSettings) -> GeneralSettings) {
        dataStore.edit { preferences ->
            val currentJson = preferences[GENERAL_SETTINGS_JSON]
            val currentState = if (currentJson != null) {
                try {
                    Json.decodeFromString<GeneralSettings>(currentJson)
                } catch (e: Exception) {
                    GeneralSettings()
                }
            } else {
                GeneralSettings()
            }
            val newState = update(currentState)
            preferences[GENERAL_SETTINGS_JSON] = Json.encodeToString(newState)
        }
    }

    suspend fun setTheme(theme: ThemeState) {
        dataStore.edit { preferences ->
            preferences[THEME_JSON] = Json.encodeToString(theme)
        }
    }

    suspend fun saveTheme(name: String, state: ThemeState) {
        dataStore.edit { preferences ->
            val currentJson = preferences[SAVED_THEMES_JSON]
            val currentThemes = if (currentJson != null) {
                try {
                    Json.decodeFromString(ListSerializer(CustomTheme.serializer()), currentJson).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }
            
            // Overwrite if name exists, otherwise add
            val existingIndex = currentThemes.indexOfFirst { it.name == name }
            if (existingIndex != -1) {
                currentThemes[existingIndex] = CustomTheme(name, state)
            } else {
                currentThemes.add(CustomTheme(name, state))
            }
            
            preferences[SAVED_THEMES_JSON] = Json.encodeToString(ListSerializer(CustomTheme.serializer()), currentThemes)
        }
    }

    suspend fun deleteTheme(name: String) {
        dataStore.edit { preferences ->
            val currentJson = preferences[SAVED_THEMES_JSON]
            if (currentJson != null) {
                try {
                    val currentThemes = Json.decodeFromString(ListSerializer(CustomTheme.serializer()), currentJson).toMutableList()
                    currentThemes.removeAll { it.name == name }
                    preferences[SAVED_THEMES_JSON] = Json.encodeToString(ListSerializer(CustomTheme.serializer()), currentThemes)
            } catch (e: Exception) {
                android.util.Log.w("SettingsRepository", "Failed to delete theme", e)
            }
            }
        }
    }
}
