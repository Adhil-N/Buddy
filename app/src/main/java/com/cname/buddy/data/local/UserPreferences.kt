package com.cname.buddy.data.local // Keep your package name!

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 1. Create the DataStore instance (this is a singleton that lives in the context)
private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferences(private val context: Context) {

    // 2. Define the "Keys" we will use to save our data
    companion object {
        val USER_EMAIL = stringPreferencesKey("user_email")
        val PROFILE_PIC_URL = stringPreferencesKey("profile_pic_url")
    }

    // 3. Create "Flows" that automatically emit the saved data
    val userEmailFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL]
    }

    val profilePicUrlFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PROFILE_PIC_URL]
    }

    // 4. Create a function to save the data when they log in
    suspend fun saveUserData(email: String, picUrl: String?) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL] = email
            if (picUrl != null) {
                preferences[PROFILE_PIC_URL] = picUrl
            }
        }
    }

    // 5. Create a function to clear the data when they log out
    suspend fun clearUserData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}