package com.roadalert.cameroun.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.detection.AccidentDetectionService
import com.roadalert.cameroun.util.ServiceActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Vérifier que c'est bien un BOOT_COMPLETED
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        // goAsync() — donne plus de 10s pour
        // l'appel DB asynchrone
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val repository = UserProfileRepository(
                    db.userDAO(),
                    db.emergencyContactDAO()
                )

                // Vérifier profil complet
                // avant de démarrer le service
                val profileComplete =
                    repository.isProfileComplete()

                if (profileComplete) {
                    startDetectionService(context)
                }
                // Si profil incomplet → ne rien faire
                // L'utilisateur n'a pas encore configuré
                // l'app — inutile de démarrer le service

            } catch (e: Exception) {
                // DB inaccessible au boot
                // Ne pas crasher — ignorer silencieusement
            } finally {
                // Libérer le BroadcastReceiver
                pendingResult.finish()
            }
        }
    }

    private fun startDetectionService(context: Context) {
        val serviceIntent = Intent(
            context,
            AccidentDetectionService::class.java
        ).apply {
            action = ServiceActions.ACTION_START_SERVICE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}