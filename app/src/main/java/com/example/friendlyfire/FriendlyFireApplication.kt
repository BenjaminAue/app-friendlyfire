// ===== 1. Application Class =====
// Fichier: app/src/main/java/com/example/friendlyfire/FriendlyFireApplication.kt

package com.example.friendlyfire

import android.app.Application
import android.util.Log
import com.example.friendlyfire.data.migration.MigrationHelper
import com.example.friendlyfire.data.monitoring.DatabaseHealthMonitor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FriendlyFireApplication : Application() {

    @Inject
    lateinit var migrationHelper: MigrationHelper

    @Inject
    lateinit var databaseHealthMonitor: DatabaseHealthMonitor

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Démarrer la migration en arrière-plan
        applicationScope.launch {
            try {
                val migrationStatus = migrationHelper.getMigrationStatus()
                Log.d("FriendlyFireApp", "Migration status: $migrationStatus")

                if (!migrationStatus.isUpToDate) {
                    Log.d("FriendlyFireApp", "Starting migration...")
                    val success = migrationHelper.performMigrationIfNeeded()
                    Log.d("FriendlyFireApp", "Migration result: $success")
                } else {
                    Log.d("FriendlyFireApp", "Migration already up to date")
                }

                // Démarrer le monitoring de la DB après migration
                databaseHealthMonitor.startMonitoring()

            } catch (e: Exception) {
                Log.e("FriendlyFireApp", "Error during migration check", e)
            }
        }
    }
}