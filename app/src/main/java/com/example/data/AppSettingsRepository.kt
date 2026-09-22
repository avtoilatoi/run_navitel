package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ExecutorMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "navitel_settings")

class AppSettingsRepository(private val context: Context) {

    companion object {
        private val KEY_AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")
        private val KEY_RUN_ONCE_PER_BOOT = booleanPreferencesKey("run_once_per_boot")
        private val KEY_CHECK_ON_APP_START = booleanPreferencesKey("check_on_app_start")
        private val KEY_SKIP_WARNING = booleanPreferencesKey("skip_cold_restart_warning")
        private val KEY_ADB_HOST = stringPreferencesKey("adb_host")
        private val KEY_ADB_PORT = intPreferencesKey("adb_port")
        private val KEY_EXECUTOR_MODE = stringPreferencesKey("executor_mode")
        private val KEY_SIMULATED_DISPLAY_ID = intPreferencesKey("simulated_display_id")

        // In-memory flag across runtime lifecycle
        @Volatile
        var hasExecutedThisBoot: Boolean = false
    }

    val autoStartOnBoot: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_START_BOOT] ?: false
    }

    val runOncePerBoot: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_RUN_ONCE_PER_BOOT] ?: true
    }

    val checkOnAppStart: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_CHECK_ON_APP_START] ?: false
    }

    val skipColdRestartWarning: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SKIP_WARNING] ?: false
    }

    val adbHost: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ADB_HOST] ?: "127.0.0.1"
    }

    val adbPort: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_ADB_PORT] ?: 5555
    }

    val executorMode: Flow<ExecutorMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_EXECUTOR_MODE]
        try {
            if (raw != null) ExecutorMode.valueOf(raw) else ExecutorMode.SIMULATION_FAKE
        } catch (e: Exception) {
            ExecutorMode.SIMULATION_FAKE
        }
    }

    val simulatedDisplayId: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_SIMULATED_DISPLAY_ID] ?: 21
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_START_BOOT] = enabled }
    }

    suspend fun setRunOncePerBoot(enabled: Boolean) {
        context.dataStore.edit { it[KEY_RUN_ONCE_PER_BOOT] = enabled }
    }

    suspend fun setCheckOnAppStart(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CHECK_ON_APP_START] = enabled }
    }

    suspend fun setSkipColdRestartWarning(skip: Boolean) {
        context.dataStore.edit { it[KEY_SKIP_WARNING] = skip }
    }

    suspend fun setAdbConfig(host: String, port: Int) {
        context.dataStore.edit {
            it[KEY_ADB_HOST] = host
            it[KEY_ADB_PORT] = port
        }
    }

    suspend fun setExecutorMode(mode: ExecutorMode) {
        context.dataStore.edit { it[KEY_EXECUTOR_MODE] = mode.name }
    }

    suspend fun setSimulatedDisplayId(displayId: Int) {
        context.dataStore.edit { it[KEY_SIMULATED_DISPLAY_ID] = displayId }
    }
}
