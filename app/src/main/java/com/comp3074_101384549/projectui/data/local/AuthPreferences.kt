package com.comp3074_101384549.projectui.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creates a singleton DataStore instance for authentication preferences
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Manages secure storage and retrieval of the user's authentication token and ID.
 */
class AuthPreferences(private val context: Context) {

    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = stringPreferencesKey("user_id")

        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
    }

    val authToken: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[Keys.AUTH_TOKEN] }

    val userId: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[Keys.USER_ID] }


    val username: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[Keys.USERNAME] }

    val email: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[Keys.EMAIL] }

    // Updated to save username and email
    suspend fun saveAuthDetails(token: String, userId: String, username: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AUTH_TOKEN] = token
            preferences[Keys.USER_ID] = userId
            preferences[Keys.USERNAME] = username
            preferences[Keys.EMAIL] = email
        }
    }

    suspend fun clearAuthDetails() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

}
