package com.comp3074_101384549.projectui.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Auth state and app mode: one login, separate [hasOwnerAccount] vs [currentMode].
 * [role] stores server/mock role (e.g. "user", "admin").
 */
class AuthPreferences(private val context: Context) {

    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val HAS_OWNER_ACCOUNT = booleanPreferencesKey("has_owner_account")
        val CURRENT_MODE = stringPreferencesKey("current_mode")
        val ROLE = stringPreferencesKey("role")
        /** Legacy key — migrated into [HAS_OWNER_ACCOUNT] / [CURRENT_MODE] on write. */
        val LEGACY_IS_SPOT_OWNER = booleanPreferencesKey("is_spot_owner")
    }

    val authToken: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[Keys.AUTH_TOKEN] }

    val userId: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[Keys.USER_ID] }

    val username: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[Keys.USERNAME] }

    val email: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[Keys.EMAIL] }

    /** Server/mock role: e.g. "user", "admin". */
    val role: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[Keys.ROLE] }

    /** True if this user completed Spot Owner registration (capabilities persist across modes). */
    val hasOwnerAccount: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.HAS_OWNER_ACCOUNT] ?: (p[Keys.LEGACY_IS_SPOT_OWNER] ?: false)
    }

    /** UI mode: [MODE_DRIVER] or [MODE_OWNER]. */
    val currentMode: Flow<String> = context.dataStore.data.map { p ->
        p[Keys.CURRENT_MODE] ?: MODE_DRIVER
    }

    /** True when the user is operating in Spot Owner context (drawer + owner screens). */
    val isInOwnerMode: Flow<Boolean> = currentMode.map { it == MODE_OWNER }

    suspend fun saveAuthDetails(
        token: String,
        userId: String,
        username: String,
        email: String,
        hasOwnerAccount: Boolean = false,
        currentMode: String = MODE_DRIVER,
        role: String = "user",
    ) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AUTH_TOKEN] = token
            preferences[Keys.USER_ID] = userId
            preferences[Keys.USERNAME] = username
            preferences[Keys.EMAIL] = email
            preferences[Keys.HAS_OWNER_ACCOUNT] = hasOwnerAccount
            preferences[Keys.CURRENT_MODE] = currentMode
            preferences[Keys.ROLE] = role
            preferences.remove(Keys.LEGACY_IS_SPOT_OWNER)
        }
    }

    /** After completing Become Owner registration — owner account exists and start in owner mode. */
    suspend fun registerAsOwner() {
        context.dataStore.edit { preferences ->
            preferences[Keys.HAS_OWNER_ACCOUNT] = true
            preferences[Keys.CURRENT_MODE] = MODE_OWNER
            preferences.remove(Keys.LEGACY_IS_SPOT_OWNER)
        }
    }

    suspend fun setCurrentMode(mode: String) {
        require(mode == MODE_DRIVER || mode == MODE_OWNER) { "Invalid mode: $mode" }
        context.dataStore.edit { preferences ->
            preferences[Keys.CURRENT_MODE] = mode
        }
    }

    suspend fun clearAuthDetails() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        const val MODE_DRIVER = "driver"
        const val MODE_OWNER = "owner"
    }
}
