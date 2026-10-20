package com.example.data.repository

import com.example.data.local.CalculationHistoryDao
import com.example.data.local.StudySessionsDao
import com.example.data.model.CalculationHistory
import com.example.data.model.StudySession
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class StatsRepository(
    private val studySessionsDao: StudySessionsDao,
    private val calculationHistoryDao: CalculationHistoryDao
) {
    val allSessions: Flow<List<StudySession>> = studySessionsDao.getAllSessions()
    val recentCalculations: Flow<List<CalculationHistory>> = calculationHistoryDao.getRecentCalculations()

    fun getTodayStudyMinutes(): Flow<Int?> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return studySessionsDao.getTotalStudyMinutesSince(calendar.timeInMillis)
    }

    suspend fun recordStudySession(subject: String, durationMinutes: Int, notesCount: Int = 0, calculationsCount: Int = 0): Long {
        val session = StudySession(
            subject = subject,
            durationMinutes = durationMinutes,
            notesCount = notesCount,
            calculationsCount = calculationsCount
        )
        return studySessionsDao.insertSession(session)
    }

    suspend fun saveCalculation(expression: String, result: String): Long {
        val entry = CalculationHistory(
            expression = expression,
            result = result
        )
        return calculationHistoryDao.insertCalculation(entry)
    }

    suspend fun clearCalculationHistory() {
        calculationHistoryDao.clearHistory()
    }
}
