package com.example.ui.screens.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiStudyAssistant
import com.example.ai.AiSummaryResult
import com.example.ai.QuizQuestion
import com.example.data.local.StudyFocusDatabase
import com.example.data.model.StudyNote
import com.example.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<StudyNote> = emptyList(),
    val subjects: List<String> = emptyList(),
    val selectedSubject: String? = null,
    val searchQuery: String = "",
    val isAiLoading: Boolean = false,
    val activeQuizQuestions: List<QuizQuestion> = emptyList(),
    val aiErrorMessage: String? = null
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StudyFocusDatabase.getDatabase(application)
    private val repository = NotesRepository(database.studyNotesDao())
    private val aiAssistant = AiStudyAssistant(application)

    private val _selectedSubject = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isAiLoading = MutableStateFlow(false)
    private val _activeQuizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    private val _aiErrorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NotesUiState> = combine(
        repository.allNotes,
        repository.allSubjects,
        _selectedSubject,
        _searchQuery,
        _isAiLoading
    ) { allNotes, subjects, subjectFilter, query, aiLoading ->
        val filtered = allNotes.filter { note ->
            val matchesSubject = subjectFilter == null || note.subject.equals(subjectFilter, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    note.topicTag.contains(query, ignoreCase = true)
            matchesSubject && matchesQuery
        }

        NotesUiState(
            notes = filtered,
            subjects = subjects,
            selectedSubject = subjectFilter,
            searchQuery = query,
            isAiLoading = aiLoading,
            activeQuizQuestions = _activeQuizQuestions.value,
            aiErrorMessage = _aiErrorMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState()
    )

    fun setSubjectFilter(subject: String?) {
        _selectedSubject.value = subject
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveNote(
        id: Long = 0,
        title: String,
        content: String,
        subject: String,
        topicTag: String,
        summary: String = "",
        revisionPoints: String = "",
        isPinned: Boolean = false
    ) {
        viewModelScope.launch {
            val note = StudyNote(
                id = id,
                title = title,
                content = content,
                subject = subject.ifBlank { "General" },
                topicTag = topicTag,
                summary = summary,
                revisionPoints = revisionPoints,
                updatedAt = System.currentTimeMillis(),
                isPinned = isPinned
            )
            if (id == 0L) {
                repository.insertNote(note)
            } else {
                repository.updateNote(note)
            }
        }
    }

    fun deleteNote(note: StudyNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun togglePin(note: StudyNote) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun summarizeNoteWithAi(note: StudyNote, onComplete: (AiSummaryResult) -> Unit = {}) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            try {
                val result = aiAssistant.summarizeAndExtract(
                    title = note.title,
                    content = note.content,
                    subject = note.subject
                )
                // Persist the summary and revision points into the note
                val updated = note.copy(
                    summary = result.summary,
                    revisionPoints = result.revisionPoints.joinToString("\n• ", prefix = "• "),
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateNote(updated)
                onComplete(result)
            } catch (e: Exception) {
                _aiErrorMessage.value = e.localizedMessage ?: "AI Summarization error"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun generatePracticeQuiz(note: StudyNote) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val result = aiAssistant.summarizeAndExtract(
                    title = note.title,
                    content = note.content,
                    subject = note.subject
                )
                _activeQuizQuestions.value = result.practiceQuestions
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearQuiz() {
        _activeQuizQuestions.value = emptyList()
    }
}
