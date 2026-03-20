package com.roadalert.cameroun.ui.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadalert.cameroun.data.db.entity.AlertStatus
import com.roadalert.cameroun.data.db.entity.SmsStatus
import com.roadalert.cameroun.data.repository.AccidentRepository
import com.roadalert.cameroun.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlertSentViewModel(
    private val eventId: String,
    private val accidentRepository: AccidentRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    // ── État UI principal ─────────────────────────────────
    private val _uiState =
        MutableStateFlow<AlertSentUiState>(
            AlertSentUiState.Sending
        )
    val uiState: StateFlow<AlertSentUiState> = _uiState

    // ── Contacts avec leur statut SMS ─────────────────────
    data class ContactDisplayInfo(
        val name: String,
        val smsStatus: String
    )

    private val _contacts =
        MutableStateFlow<List<ContactDisplayInfo>>(emptyList())
    val contacts: StateFlow<List<ContactDisplayInfo>> = _contacts

    init {
        loadContactNames()
        observeAccidentEvent()
    }

    // ── Charger les noms des contacts ─────────────────────

    private fun loadContactNames() {
        viewModelScope.launch {
            val user = userProfileRepository.getUserSync()
                ?: return@launch
            val contactList = userProfileRepository
                .getContactsSync(user.id)
            _contacts.value = contactList.map { contact ->
                ContactDisplayInfo(
                    name = contact.name,
                    smsStatus = SmsStatus.PENDING.name
                )
            }
        }
    }

    // ── Observer l'AccidentEvent depuis Room ──────────────

    private fun observeAccidentEvent() {
        viewModelScope.launch {
            accidentRepository
                .observeAccidentEvent(eventId)
                .collect { event ->
                    if (event == null) return@collect

                    // Mettre à jour statuts SMS contacts
                    val user = userProfileRepository
                        .getUserSync() ?: return@collect
                    val contactList = userProfileRepository
                        .getContactsSync(user.id)

                    val smsStatuses = listOf(
                        event.smsContact1Status,
                        event.smsContact2Status
                            ?: SmsStatus.PENDING.name,
                        event.smsContact3Status
                            ?: SmsStatus.PENDING.name
                    )

                    _contacts.value =
                        contactList.mapIndexed { index, contact ->
                            ContactDisplayInfo(
                                name = contact.name,
                                smsStatus = smsStatuses
                                    .getOrElse(index) {
                                        SmsStatus.PENDING.name
                                    }
                            )
                        }

                    // Mettre à jour l'état UI
                    _uiState.value = when (
                        event.toAlertStatus()
                    ) {
                        AlertStatus.PENDING ->
                            AlertSentUiState.Sending

                        AlertStatus.SENT ->
                            AlertSentUiState.Sent(event)

                        AlertStatus.PARTIAL,
                        AlertStatus.PENDING_RETRY ->
                            AlertSentUiState.Partial(event)

                        AlertStatus.FAILED ->
                            AlertSentUiState.Failed(event)
                    }
                }
        }
    }
}