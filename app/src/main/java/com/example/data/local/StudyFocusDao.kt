package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppLimit
import com.example.data.model.CalculationHistory
import com.example.data.model.StudyNote
import com.example.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyNotesDao {
    @Query("SELECT * FROM study_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<StudyNote>>

    @Query("SELECT * FROM study_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): StudyNote?

    @Query("SELECT * FROM study_notes WHERE subject = :subject ORDER BY updatedAt DESC")
    fun getNotesBySubject(subject: String): Flow<List<StudyNote>>

    @Query("SELECT DISTINCT subject FROM study_notes WHERE subject != '' ORDER BY subject ASC")
    fun getAllSubjects(): Flow<List<String>>

    @Query("SELECT * FROM study_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR topicTag LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<StudyNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StudyNote): Long

    @Update
    suspend fun updateNote(note: StudyNote)

    @Delete
    suspend fun deleteNote(note: StudyNote)

    @Query("DELETE FROM study_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}

@Dao
interface AppLimitsDao {
    @Query("SELECT * FROM app_limits ORDER BY appName ASC")
    fun getAllLimits(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    suspend fun getLimitByPackage(packageName: String): AppLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLimit(limit: AppLimit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(limits: List<AppLimit>)

    @Query("UPDATE app_limits SET usedMinutesToday = :minutes WHERE packageName = :packageName")
    suspend fun updateUsedMinutes(packageName: String, minutes: Int)

    @Delete
    suspend fun deleteLimit(limit: AppLimit)
}

@Dao
interface StudySessionsDao {
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT SUM(durationMinutes) FROM study_sessions WHERE timestamp >= :sinceTimestamp")
    fun getTotalStudyMinutesSince(sinceTimestamp: Long): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long
}

@Dao
interface CalculationHistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC LIMIT 30")
    fun getRecentCalculations(): Flow<List<CalculationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calc: CalculationHistory): Long

    @Query("DELETE FROM calculation_history")
    suspend fun clearHistory()
}
