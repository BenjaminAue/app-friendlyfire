// Fichier: app/src/main/java/com/example/friendlyfire/data/monitoring/DatabaseHealthMonitor.kt

package com.example.friendlyfire.data.monitoring

import android.util.Log
import com.example.friendlyfire.data.database.dao.PlayerDao
import com.example.friendlyfire.data.database.dao.QuestionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseHealthMonitor @Inject constructor(
    private val playerDao: PlayerDao,
    private val questionDao: QuestionDao
) {
    companion object {
        private const val TAG = "DatabaseHealthMonitor"
        private const val HEALTH_CHECK_INTERVAL = 300_000L // 5 minutes
    }

    private val monitoringScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isMonitoring = false

    fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        Log.d(TAG, "Starting database health monitoring")

        monitoringScope.launch {
            while (isMonitoring) {
                try {
                    performHealthCheck()
                    delay(HEALTH_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in health monitoring loop", e)
                    delay(60_000L) // Retry after 1 minute on error
                }
            }
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
        Log.d(TAG, "Stopped database health monitoring")
    }

    private suspend fun performHealthCheck() {
        val healthReport = DatabaseHealthReport()

        try {
            // Check players table
            val playerCount = playerDao.getPlayerCount()
            healthReport.playerCount = playerCount
            healthReport.playersTableHealthy = true

            // Check questions table
            val baseQuestionCount = questionDao.getBaseQuestionCount()
            healthReport.baseQuestionCount = baseQuestionCount
            healthReport.questionsTableHealthy = true

            // Check custom questions
            val customQuestionCount = questionDao.getCustomQuestionCount("friendly_fire")
            healthReport.customQuestionCount = customQuestionCount

            // Overall health assessment
            healthReport.overallHealthy = healthReport.playersTableHealthy &&
                    healthReport.questionsTableHealthy &&
                    baseQuestionCount > 0 // Should have base questions

            // Log health status
            if (healthReport.overallHealthy) {
                Log.d(TAG, "Database health check: HEALTHY - Players: $playerCount, Base Questions: $baseQuestionCount, Custom: $customQuestionCount")
            } else {
                Log.w(TAG, "Database health check: ISSUES - $healthReport")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Database health check failed", e)
            healthReport.overallHealthy = false
            healthReport.lastError = e.message
        }

        // Store the last report
        lastHealthReport = healthReport
    }

    // Get the latest health report
    private var lastHealthReport: DatabaseHealthReport? = null

    fun getLastHealthReport(): DatabaseHealthReport? = lastHealthReport

    // Force a health check (useful for debugging)
    suspend fun forceHealthCheck(): DatabaseHealthReport {
        performHealthCheck()
        return lastHealthReport ?: DatabaseHealthReport(overallHealthy = false, lastError = "No report available")
    }
}

data class DatabaseHealthReport(
    val timestamp: Long = System.currentTimeMillis(),
    var overallHealthy: Boolean = false,
    var playersTableHealthy: Boolean = false,
    var questionsTableHealthy: Boolean = false,
    var playerCount: Int = 0,
    var baseQuestionCount: Int = 0,
    var customQuestionCount: Int = 0,
    var lastError: String? = null
) {
    override fun toString(): String {
        return "DatabaseHealthReport(healthy=$overallHealthy, players=$playerCount, baseQuestions=$baseQuestionCount, customQuestions=$customQuestionCount, error=$lastError)"
    }
}