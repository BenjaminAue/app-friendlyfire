// ===== 1. Application Class =====
// Fichier: app/src/main/java/com/example/friendlyfire/FriendlyFireApplication.kt

package com.example.friendlyfire

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FriendlyFireApplication : Application()