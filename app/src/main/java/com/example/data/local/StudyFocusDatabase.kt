package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppLimit
import com.example.data.model.CalculationHistory
import com.example.data.model.StudyNote
import com.example.data.model.StudySession

@Database(
    entities = [
        StudyNote::class,
        AppLimit::class,
        StudySession::class,
        CalculationHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudyFocusDatabase : RoomDatabase() {
    abstract fun studyNotesDao(): StudyNotesDao
    abstract fun appLimitsDao(): AppLimitsDao
    abstract fun studySessionsDao(): StudySessionsDao
    abstract fun calculationHistoryDao(): CalculationHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: StudyFocusDatabase? = null

        fun getDatabase(context: Context): StudyFocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyFocusDatabase::class.java,
                    "study_focus_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Direct SQLite seeding to avoid any recursive Room open deadlocks
            db.execSQL(
                """
                INSERT OR IGNORE INTO study_notes (id, title, content, subject, topicTag, summary, revisionPoints, practiceQuestions, createdAt, updatedAt, isPinned)
                VALUES (1, 'Process Management & Scheduling', 'A process is a program in execution. Operating Systems manage processes through Process Control Blocks (PCB) containing PID, State, Program Counter, CPU registers, and scheduling info.

CPU Scheduling Algorithms:
1. FCFS (First-Come, First-Served) - Non-preemptive, suffers from convoy effect.
2. SJF (Shortest Job First) - Optimal average waiting time, but difficult to predict burst times.
3. Round Robin (RR) - Preemptive, uses time quantum (q). Ideal for time-sharing systems.
4. Priority Scheduling - Can suffer from starvation; solved via aging.', 'Operating Systems', 'CPU Scheduling', 'Processes are managed via PCBs. CPU scheduling ensures optimal resource allocation via FCFS, SJF, Round Robin, or Priority-based algorithms.', '• Process vs Program: active vs passive entity
• States: New, Ready, Running, Waiting, Terminated
• Starvation countermeasure: Aging
• Context Switch overhead: time spent saving/restoring PCB', '', 1700000000000, 1700000000000, 1)
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO study_notes (id, title, content, subject, topicTag, summary, revisionPoints, practiceQuestions, createdAt, updatedAt, isPinned)
                VALUES (2, 'Stack & Queue Operations in DSA', 'Stack follows LIFO (Last In First Out) principle.
Operations:
- push(x): O(1)
- pop(): O(1)
- peek()/top(): O(1)
Applications: Function call stack, recursion, undo mechanisms, parenthesis matching.

Queue follows FIFO (First In First Out) principle.
Operations:
- enqueue(x): O(1)
- dequeue(): O(1)
Applications: CPU scheduling, BFS traversal, printer queues.', 'Data Structures', 'Linear Structures', 'Stacks operate on LIFO with O(1) push/pop. Queues operate on FIFO with O(1) enqueue/dequeue.', '• Stack: LIFO, used in parenthesis matching and DFS
• Queue: FIFO, used in BFS and scheduling queues
• Circular Queue prevents memory wastage', '', 1700000000000, 1700000000000, 1)
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO study_notes (id, title, content, subject, topicTag, summary, revisionPoints, practiceQuestions, createdAt, updatedAt, isPinned)
                VALUES (3, 'Definite Integrals & Fundamental Theorem', 'The Fundamental Theorem of Calculus connects differentiation and integration.
Part 1: If f is continuous on [a, b], then g(x) = ∫[a to x] f(t) dt is continuous on [a, b] and differentiable on (a, b), and g''(x) = f(x).
Part 2: ∫[a to b] f(x) dx = F(b) - F(a), where F is any antiderivative of f.
Useful substitution rule: ∫ f(g(x)) * g''(x) dx = ∫ f(u) du where u = g(x).', 'Mathematics', 'Calculus', 'Fundamental Theorem of Calculus relates derivatives with definite integrals as reverse processes.', '• FTC Part 1: Derivative of integral accumulator
• FTC Part 2: Evaluation using antiderivative F(b) - F(a)
• Substitution: Chain rule in reverse', '', 1700000000000, 1700000000000, 0)
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO app_limits (packageName, appName, dailyLimitMinutes, isEnabled, category, usedMinutesToday, lastResetDate)
                VALUES ('com.instagram.android', 'Instagram', 30, 1, 'Social Media', 22, '')
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO app_limits (packageName, appName, dailyLimitMinutes, isEnabled, category, usedMinutesToday, lastResetDate)
                VALUES ('com.google.android.youtube', 'YouTube', 45, 1, 'Entertainment', 38, '')
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO app_limits (packageName, appName, dailyLimitMinutes, isEnabled, category, usedMinutesToday, lastResetDate)
                VALUES ('com.reddit.frontpage', 'Reddit', 25, 1, 'Social Media', 15, '')
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO app_limits (packageName, appName, dailyLimitMinutes, isEnabled, category, usedMinutesToday, lastResetDate)
                VALUES ('com.zhiliaoapp.musically', 'TikTok', 20, 1, 'Short Video', 18, '')
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT OR IGNORE INTO app_limits (packageName, appName, dailyLimitMinutes, isEnabled, category, usedMinutesToday, lastResetDate)
                VALUES ('com.twitter.android', 'X (Twitter)', 20, 1, 'Social Media', 8, '')
                """.trimIndent()
            )
        }
    }
}
