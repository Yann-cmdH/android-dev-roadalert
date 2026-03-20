package com.roadalert.cameroun.detection

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class SMSDispatcher(private val context: Context) {

    companion object {
        private const val SMS_TIMEOUT_MS = 30_000L
        private const val ACTION_SMS_SENT =
            "com.roadalert.cameroun.SMS_SENT"
    }

    suspend fun sendSms(
        phoneNumber: String,
        message: String
    ): SmsResult {
        return try {
            withTimeoutOrNull(SMS_TIMEOUT_MS) {
                sendSmsWithConfirmation(phoneNumber, message)
            } ?: SmsResult.Failed(
                "Timeout — pas de confirmation apres 30s"
            )
        } catch (e: Exception) {
            SmsResult.Failed(e.message ?: "Erreur inconnue")
        }
    }

    private suspend fun sendSmsWithConfirmation(
        phoneNumber: String,
        message: String
    ): SmsResult = suspendCancellableCoroutine { continuation ->

        val sentAction =
            "${ACTION_SMS_SENT}_${System.currentTimeMillis()}"

        val sentIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(sentAction).apply {
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val sentReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                ctx: Context,
                intent: Intent
            ) {
                try {
                    context.unregisterReceiver(this)
                } catch (e: IllegalArgumentException) {
                    // Deja desregistre
                }
                if (continuation.isActive) {
                    if (resultCode == Activity.RESULT_OK) {
                        continuation.resume(SmsResult.Sent)
                    } else {
                        continuation.resume(
                            SmsResult.Failed(
                                "Echec envoi code: $resultCode"
                            )
                        )
                    }
                }
            }
        }

        val filter = IntentFilter(sentAction)
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                sentReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(sentReceiver, filter)
        }

        continuation.invokeOnCancellation {
            try {
                context.unregisterReceiver(sentReceiver)
            } catch (e: IllegalArgumentException) {
                // Ignore
            }
        }

        try {
            getSmsManager().sendTextMessage(
                phoneNumber,
                null,
                message,
                sentIntent,
                null
            )
        } catch (e: Exception) {
            try {
                context.unregisterReceiver(sentReceiver)
            } catch (ex: IllegalArgumentException) {
                // Ignore
            }
            if (continuation.isActive) {
                continuation.resume(
                    SmsResult.Failed(
                        e.message ?: "Erreur envoi SMS"
                    )
                )
            }
        }
    }

    private fun getSmsManager(): SmsManager {
        return if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    suspend fun sendToContacts(
        contacts: List<Pair<String, String>>,
        message: String
    ): List<Pair<String, SmsResult>> {
        val results = mutableListOf<Pair<String, SmsResult>>()
        for ((name, phone) in contacts) {
            val result = sendSms(phone, message)
            results.add(Pair(name, result))
        }
        return results
    }
}