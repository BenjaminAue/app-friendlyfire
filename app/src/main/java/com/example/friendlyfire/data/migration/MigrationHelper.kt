// Fichier: app/src/main/java/com/example/friendlyfire/data/migration/MigrationHelper.kt

package com.example.friendlyfire.data.migration

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.friendlyfire.R
import com.example.friendlyfire.data.database.dao.PlayerDao
import com.example.friendlyfire.data.database.dao.QuestionDao
import com.example.friendlyfire.data.database.entities.PlayerEntity
import com.example.friendlyfire.data.database.entities.QuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationHelper @Inject constructor(
    private val context: Context,
    private val playerDao: PlayerDao,
    private val questionDao: QuestionDao
) {
    companion object {
        private const val TAG = "MigrationHelper"
        private const val PREFS_NAME = "migration_prefs"
        private const val KEY_MIGRATION_COMPLETED = "migration_completed"
        private const val KEY_MIGRATION_VERSION = "migration_version"
        private const val CURRENT_MIGRATION_VERSION = 1
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun performMigrationIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val migrationCompleted = prefs.getBoolean(KEY_MIGRATION_COMPLETED, false)
        val migrationVersion = prefs.getInt(KEY_MIGRATION_VERSION, 0)

        if (migrationCompleted && migrationVersion >= CURRENT_MIGRATION_VERSION) {
            Log.d(TAG, "Migration already completed, version: $migrationVersion")
            return@withContext false
        }

        try {
            Log.d(TAG, "Starting data migration...")

            // Migrer les joueurs
            val playersMigrated = migratePlayersFromFile()
            Log.d(TAG, "Players migrated: $playersMigrated")

            // Migrer les questions de base depuis les ressources RAW
            val baseQuestionsMigrated = migrateBaseQuestionsFromRaw()
            Log.d(TAG, "Base questions migrated: $baseQuestionsMigrated")

            // Migrer les questions custom
            val customQuestionsMigrated = migrateCustomQuestionsFromFiles()
            Log.d(TAG, "Custom questions migrated: $customQuestionsMigrated")

            // Marquer la migration comme terminée
            prefs.edit()
                .putBoolean(KEY_MIGRATION_COMPLETED, true)
                .putInt(KEY_MIGRATION_VERSION, CURRENT_MIGRATION_VERSION)
                .apply()

            Log.d(TAG, "Migration completed successfully!")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            return@withContext false
        }
    }

    private suspend fun migratePlayersFromFile(): Int {
        val playersFile = File(context.filesDir, "players.txt")

        if (!playersFile.exists()) {
            Log.d(TAG, "No players file found to migrate")
            return 0
        }

        try {
            val existingPlayers = playersFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

            if (existingPlayers.isEmpty()) {
                Log.d(TAG, "Players file is empty")
                return 0
            }

            val playerEntities = existingPlayers.map { playerName ->
                PlayerEntity(
                    name = playerName,
                    createdAt = System.currentTimeMillis(),
                    lastUsed = System.currentTimeMillis()
                )
            }

            playerDao.insertPlayers(playerEntities)
            Log.d(TAG, "Migrated ${playerEntities.size} players")

            // Créer une sauvegarde de l'ancien fichier
            createBackupFile(playersFile, "players_backup.txt")

            return playerEntities.size

        } catch (e: Exception) {
            Log.e(TAG, "Error migrating players", e)
            return 0
        }
    }

    private suspend fun migrateBaseQuestionsFromRaw(): Int {
        try {
            val inputStream = context.resources.openRawResource(R.raw.new_questions)
            val reader = BufferedReader(InputStreamReader(inputStream))

            val questionEntities = mutableListOf<QuestionEntity>()

            reader.forEachLine { line ->
                if (line.isNotEmpty()) {
                    val parts = line.split("|")
                    if (parts.size == 2) {
                        val questionText = parts[0].trim()
                        val penalties = parts[1].trim().toIntOrNull() ?: 0
                        if (questionText.isNotEmpty()) {
                            questionEntities.add(
                                QuestionEntity(
                                    questionText = questionText,
                                    penalties = penalties,
                                    isCustom = false,
                                    gameId = null,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }

            reader.close()

            if (questionEntities.isNotEmpty()) {
                questionDao.insertQuestions(questionEntities)
                Log.d(TAG, "Migrated ${questionEntities.size} base questions from RAW resource")
            }

            return questionEntities.size

        } catch (e: Exception) {
            Log.e(TAG, "Error migrating base questions from RAW", e)
            return 0
        }
    }

    private suspend fun migrateCustomQuestionsFromFiles(): Int {
        val filesDir = context.filesDir
        val customQuestionFiles = filesDir.listFiles { file ->
            file.name.startsWith("custom_questions_") && file.name.endsWith(".txt")
        } ?: emptyArray()

        var totalMigrated = 0

        for (file in customQuestionFiles) {
            try {
                // Extraire le gameId du nom de fichier
                // Format: custom_questions_[gameId].txt
                val gameId = file.name
                    .removePrefix("custom_questions_")
                    .removeSuffix(".txt")

                val questionEntities = mutableListOf<QuestionEntity>()

                file.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { line ->
                        val parts = line.split("|")
                        if (parts.size == 2) {
                            val questionText = parts[0].trim()
                            val penalties = parts[1].trim().toIntOrNull() ?: 0
                            if (questionText.isNotEmpty()) {
                                questionEntities.add(
                                    QuestionEntity(
                                        questionText = questionText,
                                        penalties = penalties,
                                        isCustom = true,
                                        gameId = gameId,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }

                if (questionEntities.isNotEmpty()) {
                    questionDao.insertQuestions(questionEntities)
                    Log.d(TAG, "Migrated ${questionEntities.size} custom questions for game: $gameId")
                    totalMigrated += questionEntities.size

                    // Créer une sauvegarde
                    createBackupFile(file, "backup_${file.name}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error migrating custom questions from file: ${file.name}", e)
            }
        }

        return totalMigrated
    }

    private fun createBackupFile(originalFile: File, backupFileName: String) {
        try {
            val backupFile = File(context.filesDir, backupFileName)
            originalFile.copyTo(backupFile, overwrite = true)
            Log.d(TAG, "Created backup: $backupFileName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create backup for: ${originalFile.name}", e)
        }
    }

    // Méthode pour nettoyer les anciens fichiers après confirmation que tout fonctionne
    suspend fun cleanupOldFiles(): Boolean = withContext(Dispatchers.IO) {
        try {
            val filesToDelete = listOf(
                File(context.filesDir, "players.txt"),
                File(context.filesDir, "questions.txt")
            )

            // Supprimer les fichiers custom_questions_*.txt
            val filesDir = context.filesDir
            val customQuestionFiles = filesDir.listFiles { file ->
                file.name.startsWith("custom_questions_") && file.name.endsWith(".txt")
            } ?: emptyArray()

            val allFilesToDelete = filesToDelete + customQuestionFiles

            var deletedCount = 0
            for (file in allFilesToDelete) {
                if (file.exists() && file.delete()) {
                    deletedCount++
                    Log.d(TAG, "Deleted old file: ${file.name}")
                }
            }

            Log.d(TAG, "Cleaned up $deletedCount old files")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old files", e)
            return@withContext false
        }
    }

    // Méthode pour vérifier l'état de la migration
    fun getMigrationStatus(): MigrationStatus {
        val migrationCompleted = prefs.getBoolean(KEY_MIGRATION_COMPLETED, false)
        val migrationVersion = prefs.getInt(KEY_MIGRATION_VERSION, 0)

        return MigrationStatus(
            isCompleted = migrationCompleted,
            version = migrationVersion,
            isUpToDate = migrationCompleted && migrationVersion >= CURRENT_MIGRATION_VERSION
        )
    }
}

data class MigrationStatus(
    val isCompleted: Boolean,
    val version: Int,
    val isUpToDate: Boolean
)