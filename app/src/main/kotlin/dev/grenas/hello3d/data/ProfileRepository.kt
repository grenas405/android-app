package dev.grenas.hello3d.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.profileStore: DataStore<Preferences> by preferencesDataStore(name = "profile")

/** Persists the [Profile] in Preferences DataStore. One key per field; languages are joined with a separator. */
class ProfileRepository(context: Context) {

    private val store = context.applicationContext.profileStore

    val profile: Flow<Profile> = store.data
        .catch { failure -> if (failure is IOException) emit(emptyPreferences()) else throw failure }
        .map { it.toProfile() }

    suspend fun update(transform: (Profile) -> Profile) {
        store.edit { preferences -> preferences.write(transform(preferences.toProfile())) }
    }

    private object Keys {
        val NAME = stringPreferencesKey("name")
        val LOCATION = stringPreferencesKey("location")
        val LANGUAGES = stringPreferencesKey("languages")
        val THEME = stringPreferencesKey("theme")
        val QUALITY = stringPreferencesKey("quality")
        val RAIN = stringPreferencesKey("rain")
    }

    private fun Preferences.toProfile(): Profile {
        val defaults = Profile()
        return Profile(
            name = this[Keys.NAME] ?: defaults.name,
            location = this[Keys.LOCATION] ?: defaults.location,
            languages = this[Keys.LANGUAGES]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: defaults.languages,
            themeId = this[Keys.THEME] ?: defaults.themeId,
            quality = this[Keys.QUALITY]?.let { name -> RenderPreset.entries.firstOrNull { it.name == name } }
                ?: defaults.quality,
            rainIntensity = this[Keys.RAIN]?.let { name -> RainIntensity.entries.firstOrNull { it.name == name } }
                ?: defaults.rainIntensity,
        )
    }

    private fun MutablePreferences.write(profile: Profile) {
        this[Keys.NAME] = profile.name
        this[Keys.LOCATION] = profile.location
        this[Keys.LANGUAGES] = profile.languages.joinToString(SEPARATOR)
        this[Keys.THEME] = profile.themeId
        this[Keys.QUALITY] = profile.quality.name
        this[Keys.RAIN] = profile.rainIntensity.name
    }

    private companion object {
        /** ASCII unit separator: never typed by a user, so it is a safe list delimiter. */
        const val SEPARATOR = "\u001F"
    }
}
