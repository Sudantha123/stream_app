package com.streamvault.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.streamvault.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "streamvault_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val BASE_URL_KEY = stringPreferencesKey("base_url")
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[BASE_URL_KEY] ?: BuildConfig.BASE_URL
    }

    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[BASE_URL_KEY] = url.trimEnd('/')
        }
    }

    suspend fun getBaseUrlOnce(): String {
        var result = BuildConfig.BASE_URL
        context.dataStore.data.collect { prefs ->
            result = prefs[BASE_URL_KEY] ?: BuildConfig.BASE_URL
            return@collect
        }
        return result
    }
}

