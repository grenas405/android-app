package dev.grenas.hello3d.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes the persisted [Profile] (null until the first read completes) and small, intent-named edits. */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)

    val profile: StateFlow<Profile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setName(value: String) = edit { it.copy(name = value.take(Profile.MAX_NAME)) }

    fun setLocation(value: String) = edit { it.copy(location = value.take(Profile.MAX_LOCATION)) }

    fun setLanguages(value: List<String>) = edit {
        it.copy(languages = value.map(Languages::normalise).filter { l -> l.isNotBlank() }.distinct().take(Profile.MAX_LANGUAGES))
    }

    fun toggleLanguage(language: String) = edit { current ->
        val next = if (language in current.languages) current.languages - language
        else if (current.languages.size < Profile.MAX_LANGUAGES) current.languages + language
        else current.languages
        current.copy(languages = next)
    }

    fun setTheme(themeId: String) = edit { it.copy(themeId = themeId) }

    fun setQuality(quality: RenderPreset) = edit { it.copy(quality = quality) }

    fun setRainIntensity(intensity: RainIntensity) = edit { it.copy(rainIntensity = intensity) }

    private fun edit(transform: (Profile) -> Profile) {
        viewModelScope.launch { repository.update(transform) }
    }
}
