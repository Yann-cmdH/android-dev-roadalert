package com.roadalert.cameroun.ui.alert

import com.roadalert.cameroun.data.db.entity.AccidentEvent

sealed class AlertSentUiState {

    // État 1 — Envoi en cours
    object Sending : AlertSentUiState()

    // État 2 — Succès total
    data class Sent(
        val event: AccidentEvent
    ) : AlertSentUiState()

    // État 3 — Échec partiel
    data class Partial(
        val event: AccidentEvent
    ) : AlertSentUiState()

    // État 4 — Échec total — pas d'auto-fermeture
    data class Failed(
        val event: AccidentEvent
    ) : AlertSentUiState()
}