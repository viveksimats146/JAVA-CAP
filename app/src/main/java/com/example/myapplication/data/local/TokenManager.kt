package com.example.myapplication.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
open class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context?
) {
    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    open val authTokenFlow: Flow<String?> = context?.dataStore?.data?.map { preferences ->
        preferences[AUTH_TOKEN_KEY]
    } ?: flowOf(null)

    open val usernameFlow: Flow<String?> = context?.dataStore?.data?.map { preferences ->
        preferences[USERNAME_KEY]
    } ?: flowOf(null)

    open suspend fun saveAuthToken(token: String, username: String = "") {
        context?.dataStore?.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
            if (username.isNotBlank()) {
                preferences[USERNAME_KEY] = username
            }
        }
    }

    open suspend fun getToken(): String? {
        return authTokenFlow.firstOrNull()
    }

    open suspend fun clearToken() {
        context?.dataStore?.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(USERNAME_KEY)
        }
    }
}
