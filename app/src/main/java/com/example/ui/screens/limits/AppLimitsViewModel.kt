package com.example.ui.screens.limits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.StudyFocusDatabase
import com.example.data.model.AppLimit
import com.example.data.repository.AppLimitsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppLimitsUiState(
    val limits: List<AppLimit> = emptyList(),
    val installedApps: List<Pair<String, String>> = emptyList(),
    val isUsagePermissionGranted: Boolean = false,
    val selectedAppForShield: AppLimit? = null
)

class AppLimitsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StudyFocusDatabase.getDatabase(application)
    private val repository = AppLimitsRepository(application, database.appLimitsDao())

    private val _installedApps = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    private val _selectedAppForShield = MutableStateFlow<AppLimit?>(null)

    val uiState: StateFlow<AppLimitsUiState> = combine(
        repository.allLimits,
        _installedApps,
        _selectedAppForShield
    ) { limits, installed, shieldApp ->
        AppLimitsUiState(
            limits = limits,
            installedApps = installed,
            isUsagePermissionGranted = repository.isUsagePermissionGranted(),
            selectedAppForShield = shieldApp
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppLimitsUiState()
    )

    init {
        loadInstalledApps()
        refreshUsage()
    }

    fun checkPermission(): Boolean {
        return repository.isUsagePermissionGranted()
    }

    fun refreshUsage() {
        viewModelScope.launch {
            repository.refreshUsageStats()
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            _installedApps.value = apps
        }
    }

    fun updateDailyLimit(limit: AppLimit, newLimitMinutes: Int) {
        viewModelScope.launch {
            val updated = limit.copy(dailyLimitMinutes = newLimitMinutes)
            repository.saveLimit(updated)
        }
    }

    fun toggleLimitEnabled(limit: AppLimit, enabled: Boolean) {
        viewModelScope.launch {
            val updated = limit.copy(isEnabled = enabled)
            repository.saveLimit(updated)
        }
    }

    fun addAppLimit(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch {
            val limit = AppLimit(
                packageName = packageName,
                appName = appName,
                dailyLimitMinutes = limitMinutes,
                isEnabled = true,
                usedMinutesToday = 0
            )
            repository.saveLimit(limit)
        }
    }

    fun deleteAppLimit(limit: AppLimit) {
        viewModelScope.launch {
            repository.deleteLimit(limit)
        }
    }

    fun simulateUsage(packageName: String, minutes: Int) {
        viewModelScope.launch {
            repository.simulateUsageIncrement(packageName, minutes)
        }
    }

    fun resetUsage(packageName: String) {
        viewModelScope.launch {
            repository.resetTodayUsage(packageName)
        }
    }

    fun showShieldForApp(app: AppLimit) {
        _selectedAppForShield.value = app
    }

    fun dismissShield() {
        _selectedAppForShield.value = null
    }
}
