package com.example.musicx.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicx.data.GeneralSettings
import com.example.musicx.data.SettingsRepository
import com.example.musicx.ui.theme.CustomTheme
import com.example.musicx.ui.theme.ThemeState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val themeState: StateFlow<ThemeState> = repository.themeState
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ThemeState()
        )

    val savedThemes: StateFlow<List<CustomTheme>> = repository.savedThemes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val generalSettings: StateFlow<GeneralSettings> = repository.generalSettings
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            GeneralSettings()
        )

    fun updateTheme(update: (ThemeState) -> ThemeState) {
        viewModelScope.launch {
            repository.updateTheme(update)
        }
    }
    
    fun updateGeneralSettings(update: (GeneralSettings) -> GeneralSettings) {
        viewModelScope.launch {
            repository.updateGeneralSettings(update)
        }
    }

    fun setTheme(theme: ThemeState) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }

    fun exportTheme(): String {
        return Json.encodeToString(themeState.value)
    }
    
    fun importTheme(jsonStr: String) {
        try {
            val theme = Json.decodeFromString<ThemeState>(jsonStr)
            setTheme(theme)
        } catch (e: Exception) {
            android.util.Log.w("SettingsViewModel", "Failed to import theme", e)
        }
    }
    
    fun resetToDefault() {
        setTheme(ThemeState())
    }

    fun saveCurrentTheme(name: String) {
        viewModelScope.launch {
            repository.saveTheme(name, themeState.value)
        }
    }

    fun deleteTheme(name: String) {
        viewModelScope.launch {
            repository.deleteTheme(name)
        }
    }
}
