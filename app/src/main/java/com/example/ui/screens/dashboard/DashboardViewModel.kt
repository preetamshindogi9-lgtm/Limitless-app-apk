package com.example.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiStudyAssistant
import com.example.ai.StudyRecommendation
import com.example.ai.UrgencyLevel
import com.example.data.local.StudyFocusDatabase
import com.example.data.model.AppLimit
import com.example.data.model.StudyNote
import com.example.data.repository.AppLimitsRepository
import com.example.data.repository.NotesRepository
import com.example.data.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val studyMinutesToday: Int = 0,
    val studyStreakDays: Int = 4,
    val trackedLimits: List<AppLimit> = emptyList(),
    val totalDistractionUsedMinutes: Int = 0,
    val totalDistractionLimitMinutes: Int = 0,
    val exceededLimits: List<AppLimit> = emptyList(),
    val recentNotes: List<StudyNote> = emptyList(),
    val recommendation: StudyRecommendation? = null,
    val isUsagePermissionGranted: Boolean = false,
    val activeFocusTimerSeconds: Int = 0,
    val isFocusTimerRunning: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StudyFocusDatabase.getDatabase(application)
    private val limitsRepository = AppLimitsRepository(application, database.appLimitsDao())
    private val notesRepository = NotesRepository(database.studyNotesDao())
    private val statsRepository = StatsRepository(database.studySessionsDao(), database.calculationHistoryDao())
    private val aiAssistant = AiStudyAssistant(application)

    private val _focusTimerSeconds = MutableStateFlow(0)
    private val _isFocusTimerRunning = MutableStateFlow(false)

    val uiState: StateFlow<DashboardUiState> = combine(
        limitsRepository.allLimits,
        notesRepository.allNotes,
        statsRepository.getTodayStudyMinutes(),
        _focusTimerSeconds,
        _isFocusTimerRunning
    ) { limits, notes, todayStudy, timerSecs, isTimerActive ->
        val studyMinutes = (todayStudy ?: 0) + (timerSecs / 60)
        val totalUsed = limits.filter { it.isEnabled }.sumOf { it.usedMinutesToday }
        val totalLimit = limits.filter { it.isEnabled }.sumOf { it.dailyLimitMinutes }
        val exceeded = limits.filter { it.isEnabled && it.usedMinutesToday >= it.dailyLimitMinutes }

        val recommendation = aiAssistant.analyzeUsageAndRecommend(
            usedMinutes = totalUsed,
            totalLimitMinutes = if (totalLimit > 0) totalLimit else 60,
            studyMinutesToday = studyMinutes,
            exceededAppsCount = exceeded.size
        )

        DashboardUiState(
            studyMinutesToday = studyMinutes,
            studyStreakDays = 5,
            trackedLimits = limits,
            totalDistractionUsedMinutes = totalUsed,
            totalDistractionLimitMinutes = totalLimit,
            exceededLimits = exceeded,
            recentNotes = notes.take(3),
            recommendation = recommendation,
            isUsagePermissionGranted = limitsRepository.isUsagePermissionGranted(),
            activeFocusTimerSeconds = timerSecs,
            isFocusTimerRunning = isTimerActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun startFocusSession(minutes: Int) {
        viewModelScope.launch {
            _focusTimerSeconds.value = minutes * 60
            _isFocusTimerRunning.value = true
        }
    }

    fun completeFocusSession(subject: String, minutes: Int) {
        viewModelScope.launch {
            statsRepository.recordStudySession(
                subject = subject,
                durationMinutes = minutes
            )
            _focusTimerSeconds.value = 0
            _isFocusTimerRunning.value = false
        }
    }

    fun cancelFocusSession() {
        _focusTimerSeconds.value = 0
        _isFocusTimerRunning.value = false
    }

    fun refreshUsage() {
        viewModelScope.launch {
            limitsRepository.refreshUsageStats()
        }
    }

    fun simulateSocialMediaScroll(packageName: String, minutes: Int) {
        viewModelScope.launch {
            limitsRepository.simulateUsageIncrement(packageName, minutes)
        }
    }

    fun resetAppUsage(packageName: String) {
        viewModelScope.launch {
            limitsRepository.resetTodayUsage(packageName)
        }
    }
}
