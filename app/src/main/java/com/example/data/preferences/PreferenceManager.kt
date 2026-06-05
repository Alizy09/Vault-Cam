package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vaultcam_preferences")

class PreferenceManager(private val context: Context) {

    companion object {
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_BIOMETRICS_ENABLED = booleanPreferencesKey("biometrics_enabled")
        private val KEY_USE_FRONT_CAMERA = booleanPreferencesKey("use_front_camera")
        private val KEY_ACTIVE_ICON = stringPreferencesKey("active_icon")

        const val ICON_DEFAULT = "com.example.AliasVaultCam"
        const val ICON_CALCULATOR = "com.example.AliasCalculator"
        const val ICON_NOTES = "com.example.AliasNotes"
        const val ICON_COMPASS = "com.example.AliasCompass"
    }

    val pinHashFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PIN_HASH]
    }

    val biometricsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRICS_ENABLED] ?: false
    }

    val useFrontCameraFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_USE_FRONT_CAMERA] ?: false
    }

    val activeIconFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_ICON] ?: ICON_DEFAULT
    }

    suspend fun savePin(pin: String) {
        val hashed = hashString(pin)
        context.dataStore.edit { preferences ->
            preferences[KEY_PIN_HASH] = hashed
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val hashedInput = hashString(pin)
        var storedHash: String? = null
        context.dataStore.edit { preferences ->
            storedHash = preferences[KEY_PIN_HASH]
        }
        return storedHash == hashedInput
    }

    suspend fun setBiometricsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRICS_ENABLED] = enabled
        }
    }

    suspend fun setUseFrontCamera(front: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USE_FRONT_CAMERA] = front
        }
    }

    suspend fun setActiveIcon(iconAliasName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_ICON] = iconAliasName
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
