package com.example.data.repository

import com.example.data.local.StudyNotesDao
import com.example.data.model.StudyNote
import kotlinx.coroutines.flow.Flow

class NotesRepository(private val notesDao: StudyNotesDao) {
    val allNotes: Flow<List<StudyNote>> = notesDao.getAllNotes()
    val allSubjects: Flow<List<String>> = notesDao.getAllSubjects()

    fun getNotesBySubject(subject: String): Flow<List<StudyNote>> =
        notesDao.getNotesBySubject(subject)

    fun searchNotes(query: String): Flow<List<StudyNote>> =
        notesDao.searchNotes(query)

    suspend fun getNoteById(id: Long): StudyNote? =
        notesDao.getNoteById(id)

    suspend fun insertNote(note: StudyNote): Long =
        notesDao.insertNote(note)

    suspend fun updateNote(note: StudyNote) =
        notesDao.updateNote(note)

    suspend fun deleteNote(note: StudyNote) =
        notesDao.deleteNote(note)

    suspend fun deleteNoteById(id: Long) =
        notesDao.deleteNoteById(id)
}
