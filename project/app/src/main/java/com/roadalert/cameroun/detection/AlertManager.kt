package com.roadalert.cameroun.detection

import android.content.Context
import android.content.Intent
import com.roadalert.cameroun.data.db.entity.AccidentEvent
import com.roadalert.cameroun.data.db.entity.AlertStatus
import com.roadalert.cameroun.data.db.entity.SmsStatus
import com.roadalert.cameroun.data.db.entity.TriggerType
import com.roadalert.cameroun.data.repository.AccidentRepository
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.data.worker.RetryWorker
import com.roadalert.cameroun.ui.alert.AlertSentActivity
import com.roadalert.cameroun.util.IdGenerator
import com.roadalert.cameroun.util.ServiceActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlertManager(
    private val context: Context,
    private val accidentRepository: AccidentRepository,
    private val userProfileRepository: UserProfileRepository
) {

    private var isAlertInProgress = false

    private val locationCaptureManager =
        LocationCaptureManager(context)
    private val smsDispatcher = SMSDispatcher(context)

    fun triggerAlert(
        triggerType: TriggerType,
        gForceValue: Float = 0f,
        scope: CoroutineScope
    ) {
        if (isAlertInProgress) return
        isAlertInProgress = true

        scope.launch(Dispatchers.IO) {
            executeAlert(triggerType, gForceValue)
        }
    }

    private suspend fun executeAlert(
        triggerType: TriggerType,
        gForceValue: Float
    ) {
        try {
            val user = userProfileRepository.getUserSync()
            if (user == null) {
                isAlertInProgress = false
                return
            }

            val contacts = userProfileRepository
                .getContactsSync(user.id)
            if (contacts.isEmpty()) {
                isAlertInProgress = false
                return
            }

            val eventId = IdGenerator.newId()
            val timestamp = System.currentTimeMillis()

            val accidentEvent = AccidentEvent(
                id = eventId,
                userId = user.id,
                timestamp = timestamp,
                gForceValue = gForceValue,
                alertStatus = AlertStatus.PENDING.name,
                triggerType = triggerType.name,
                smsContactsCount = contacts.size
            )
            accidentRepository.saveAccidentEvent(accidentEvent)

            openAlertSentActivity(eventId)

            // context retiré — captureLocation() sans paramètre
            val locationResult = locationCaptureManager
                .captureLocation()

            when (locationResult) {
                is LocationResult.Success -> {
                    accidentRepository.updateLocation(
                        eventId,
                        locationResult.latitude,
                        locationResult.longitude,
                        locationResult.isApproximate
                    )
                }
                LocationResult.Unavailable -> { }
            }

            val smsMessage = SmsTemplate.build(
                user = user,
                locationResult = locationResult,
                timestamp = timestamp
            )

            val contactList = contacts
                .sortedBy { it.priority }
                .map { Pair(it.name, it.phoneNumber) }

            val smsResults = smsDispatcher.sendToContacts(
                contacts = contactList,
                message = smsMessage
            )

            val sentCount = smsResults.count {
                it.second is SmsResult.Sent
            }

            val c1Status = getSmsStatusForIndex(smsResults, 0)
            val c2Status = getSmsStatusForIndex(smsResults, 1)
            val c3Status = getSmsStatusForIndex(smsResults, 2)

            val globalStatus = when {
                sentCount == smsResults.size ->
                    AlertStatus.SENT
                sentCount > 0 ->
                    AlertStatus.PARTIAL
                else ->
                    AlertStatus.FAILED
            }

            val updatedEvent = accidentEvent.copy(
                latitude = when (locationResult) {
                    is LocationResult.Success ->
                        locationResult.latitude
                    else -> null
                },
                longitude = when (locationResult) {
                    is LocationResult.Success ->
                        locationResult.longitude
                    else -> null
                },
                isPositionApproximate = when (locationResult) {
                    is LocationResult.Success ->
                        locationResult.isApproximate
                    else -> false
                },
                alertStatus = globalStatus.name,
                smsContact1Status = c1Status,
                smsContact2Status = c2Status,
                smsContact3Status = c3Status
            )
            accidentRepository.updateAccidentEvent(updatedEvent)

            if (globalStatus == AlertStatus.PARTIAL ||
                globalStatus == AlertStatus.FAILED) {
                RetryWorker.schedule(context, eventId)
            }

            broadcastAlertResult(eventId, globalStatus)

        } catch (e: Exception) {
            // Exception inattendue
        } finally {
            isAlertInProgress = false
        }
    }

    private fun openAlertSentActivity(eventId: String) {
        val intent = Intent(
            context,
            AlertSentActivity::class.java
        ).apply {
            putExtra(
                ServiceActions.EXTRA_ACCIDENT_EVENT_ID,
                eventId
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    private fun broadcastAlertResult(
        eventId: String,
        status: AlertStatus
    ) {
        val intent = Intent(
            ServiceActions.ACTION_ALERT_RESULT
        ).apply {
            putExtra(
                ServiceActions.EXTRA_ACCIDENT_EVENT_ID,
                eventId
            )
            putExtra(
                ServiceActions.EXTRA_ALERT_STATUS,
                status.name
            )
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    private fun getSmsStatusForIndex(
        results: List<Pair<String, SmsResult>>,
        index: Int
    ): String {
        if (index >= results.size) return SmsStatus.PENDING.name
        return when (results[index].second) {
            is SmsResult.Sent -> SmsStatus.SENT.name
            is SmsResult.Failed -> SmsStatus.FAILED.name
        }
    }
}