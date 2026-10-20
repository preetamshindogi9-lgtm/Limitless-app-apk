package com.example.ui.screens.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.AngleMode
import com.example.calculator.ScientificCalculatorEngine
import com.example.data.local.StudyFocusDatabase
import com.example.data.model.CalculationHistory
import com.example.data.model.StudyNote
import com.example.data.repository.NotesRepository
import com.example.data.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val expression: String = "",
    val liveResult: String = "",
    val finalResult: String = "",
    val angleMode: AngleMode = AngleMode.DEGREE,
    val isShiftActive: Boolean = false,
    val history: List<CalculationHistory> = emptyList(),
    val errorMessage: String? = null,
    val noteCreatedToast: String? = null
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StudyFocusDatabase.getDatabase(application)
    private val statsRepository = StatsRepository(database.studySessionsDao(), database.calculationHistoryDao())
    private val notesRepository = NotesRepository(database.studyNotesDao())

    private val _expression = MutableStateFlow("")
    private val _liveResult = MutableStateFlow("")
    private val _finalResult = MutableStateFlow("")
    private val _angleMode = MutableStateFlow(AngleMode.DEGREE)
    private val _isShiftActive = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _noteCreatedToast = MutableStateFlow<String?>(null)

    private data class CalcInputs(
        val expr: String,
        val live: String,
        val final: String,
        val mode: AngleMode,
        val shift: Boolean
    )

    private val _inputs = combine(
        _expression,
        _liveResult,
        _finalResult,
        _angleMode,
        _isShiftActive
    ) { expr, live, final, mode, shift ->
        CalcInputs(expr, live, final, mode, shift)
    }

    val uiState: StateFlow<CalculatorUiState> = combine(
        _inputs,
        statsRepository.recentCalculations,
        _errorMessage,
        _noteCreatedToast
    ) { inputs, hist, error, toast ->
        CalculatorUiState(
            expression = inputs.expr,
            liveResult = inputs.live,
            finalResult = inputs.final,
            angleMode = inputs.mode,
            isShiftActive = inputs.shift,
            history = hist,
            errorMessage = error,
            noteCreatedToast = toast
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalculatorUiState()
    )

    fun onInput(char: String) {
        _errorMessage.value = null
        val current = _expression.value
        val updated = current + char
        _expression.value = updated
        evaluateLive(updated)
    }

    fun onFunction(func: String) {
        _errorMessage.value = null
        val current = _expression.value
        val updated = "$current$func("
        _expression.value = updated
        evaluateLive(updated)
    }

    fun onBackspace() {
        _errorMessage.value = null
        val current = _expression.value
        if (current.isNotEmpty()) {
            val updated = current.dropLast(1)
            _expression.value = updated
            evaluateLive(updated)
        }
    }

    fun onClear() {
        _expression.value = ""
        _liveResult.value = ""
        _finalResult.value = ""
        _errorMessage.value = null
    }

    fun toggleAngleMode() {
        val newMode = if (_angleMode.value == AngleMode.DEGREE) AngleMode.RADIAN else AngleMode.DEGREE
        _angleMode.value = newMode
        evaluateLive(_expression.value)
    }

    fun toggleShift() {
        _isShiftActive.value = !_isShiftActive.value
    }

    private fun evaluateLive(expr: String) {
        if (expr.isBlank()) {
            _liveResult.value = ""
            return
        }
        val result = ScientificCalculatorEngine.evaluate(expr, _angleMode.value)
        if (result.isSuccess) {
            _liveResult.value = ScientificCalculatorEngine.formatResult(result.getOrThrow())
        }
    }

    fun onEquals() {
        val expr = _expression.value
        if (expr.isBlank()) return
        val result = ScientificCalculatorEngine.evaluate(expr, _angleMode.value)
        if (result.isSuccess) {
            val formatted = ScientificCalculatorEngine.formatResult(result.getOrThrow())
            _finalResult.value = formatted
            _liveResult.value = formatted
            viewModelScope.launch {
                statsRepository.saveCalculation(expr, formatted)
            }
        } else {
            _errorMessage.value = "Invalid expression"
        }
    }

    fun saveCalculationToStudyNote(subject: String = "Mathematics") {
        val expr = _expression.value
        val res = _finalResult.value.ifBlank { _liveResult.value }
        if (expr.isBlank()) return

        viewModelScope.launch {
            val note = StudyNote(
                title = "Calculation: $expr = $res",
                content = "Mathematical calculation evaluated in StudyFocus:\n\nExpression: $expr\nResult: $res\nAngle Mode: ${_angleMode.value.name}\nTimestamp: ${System.currentTimeMillis()}",
                subject = subject,
                topicTag = "Calculations",
                summary = "$expr = $res"
            )
            notesRepository.insertNote(note)
            _noteCreatedToast.value = "Calculation saved to Study Notes!"
        }
    }

    fun clearToast() {
        _noteCreatedToast.value = null
    }

    fun loadFromHistory(history: CalculationHistory) {
        _expression.value = history.expression
        _finalResult.value = history.result
        _liveResult.value = history.result
        _errorMessage.value = null
    }
}
