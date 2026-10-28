package com.cheese.watchlist.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

/** Tracks whether the user has completed onboarding, and their display name, via DataStore. */
object OnboardingPreferences {
    private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    private val KEY_USER_NAME = stringPreferencesKey("user_name")
    private val KEY_FIRST_GREETING_SHOWN = booleanPreferencesKey("first_greeting_shown")

    fun isOnboardingCompleteFlow(context: Context): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[KEY_ONBOARDING_COMPLETE] ?: false }

    suspend fun isOnboardingComplete(context: Context): Boolean =
        isOnboardingCompleteFlow(context).first()

    suspend fun setOnboardingComplete(context: Context, complete: Boolean) {
        context.onboardingDataStore.edit { it[KEY_ONBOARDING_COMPLETE] = complete }
    }

    suspend fun getUserName(context: Context): String =
        context.onboardingDataStore.data.map { it[KEY_USER_NAME] ?: "" }.first()

    suspend fun setUserName(context: Context, name: String) {
        context.onboardingDataStore.edit { it[KEY_USER_NAME] = name.trim() }
    }

    /** True only the very first time the app is opened after onboarding — never again after that. */
    suspend fun consumeFirstGreeting(context: Context): Boolean {
        val alreadyShown = context.onboardingDataStore.data.map { it[KEY_FIRST_GREETING_SHOWN] ?: false }.first()
        if (!alreadyShown) {
            context.onboardingDataStore.edit { it[KEY_FIRST_GREETING_SHOWN] = true }
        }
        return !alreadyShown
    }
}
