package com.roadalert.cameroun.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.db.entity.AlertStatus
import com.roadalert.cameroun.data.db.entity.SmsStatus
import com.roadalert.cameroun.data.repository.AccidentRepository
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.detection.LocationResult
import com.roadalert.cameroun.detection.SMSDispatcher
import com.roadalert.cameroun.detection.SmsResult
import com.roadalert.cameroun.detection.SmsTemplate
import java.util.concurrent.TimeUnit

class RetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val KEY_EVENT_ID = "event_id"
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_SECONDS = 30L

        fun schedule(context: Context, eventId: String) {
            val workRequest =
                OneTimeWorkRequestBuilder<RetryWorker>()
                    .setInputData(
                        workDataOf(KEY_EVENT_ID to eventId)
                    )
                    .setInitialDelay(
                        RETRY_DELAY_SECONDS,
                        TimeUnit.SECONDS
                    )
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "retry_$eventId",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
        }
    }

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID)
            ?: return Result.failure()

        val database = AppDatabase.getInstance(applicationContext)
        val accidentRepository = AccidentRepository(
            database.accidentEventDAO()
        )
        val userProfileRepository = UserProfileRepository(
            database.userDAO(),
            database.emergencyContactDAO()
        )
        val smsDispatcher = SMSDispatcher(applicationContext)

        return try {
            val event = accidentRepository
                .getAccidentEventById(eventId)
                ?: return Result.failure()

            if (event.retryCount >= MAX_RETRY_COUNT) {
                accidentRepository.updateAlertStatus(
                    eventId, AlertStatus.FAILED
                )
                return Result.success()
            }

            accidentRepository.incrementRetryCount(eventId)

            val user = userProfileRepository.getUserSync()
                ?: return Result.failure()

            val contacts = userProfileRepository
                .getContactsSync(user.id)
            if (contacts.isEmpty()) {
                accidentRepository.updateAlertStatus(
                    eventId, AlertStatus.FAILED
                )
                return Result.success()
            }

            val locationResult =
                if (event.latitude != null &&
                    event.longitude != null) {
                    LocationResult.Success(
                        latitude = event.latitude,
                        longitude = event.longitude,
                        isApproximate = event.isPositionApproximate
                    )
                } else {
                    LocationResult.Unavailable
                }

            val smsMessage = SmsTemplate.build(
                user = user,
                locationResult = locationResult,
                timestamp = event.timestamp
            )

            val failedContacts =
                mutableListOf<Pair<String, String>>()

            if (event.smsContact1Status ==
                SmsStatus.FAILED.name &&
                contacts.size >= 1) {
                failedContacts.add(
                    Pair(
                        contacts[0].name,
                        contacts[0].phoneNumber
                    )
                )
            }
            if (event.smsContact2Status ==
                SmsStatus.FAILED.name &&
                contacts.size >= 2) {
                failedContacts.add(
                    Pair(
                        contacts[1].name,
                        contacts[1].phoneNumber
                    )
                )
            }
            if (event.smsContact3Status ==
                SmsStatus.FAILED.name &&
                contacts.size >= 3) {
                failedContacts.add(
                    Pair(
                        contacts[2].name,
                        contacts[2].phoneNumber
                    )
                )
            }

            if (failedContacts.isEmpty()) {
                accidentRepository.updateAlertStatus(
                    eventId, AlertStatus.SENT
                )
                return Result.success()
            }

            val retryResults = smsDispatcher.sendToContacts(
                contacts = failedContacts,
                message = smsMessage
            )

            val allSent = retryResults.all {
                it.second is SmsResult.Sent
            }
            val someSent = retryResults.any {
                it.second is SmsResult.Sent
            }

            val newStatus = when {
                allSent -> AlertStatus.SENT
                someSent -> AlertStatus.PARTIAL
                event.retryCount + 1 >= MAX_RETRY_COUNT ->
                    AlertStatus.FAILED
                else -> AlertStatus.PENDING_RETRY
            }

            accidentRepository.updateAlertStatus(
                eventId, newStatus
            )

            if (newStatus == AlertStatus.PENDING_RETRY) {
                schedule(applicationContext, eventId)
            }

            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }
}