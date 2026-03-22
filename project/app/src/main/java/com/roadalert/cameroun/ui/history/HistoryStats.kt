package com.roadalert.cameroun.ui.history

data class HistoryStats(
    val incidentCount: Int = 0,
    val contactsAlerted: Int = 0
) {
    fun toDisplayString(): String {
        val incident = if (incidentCount == 1)
            "incident" else "incidents"
        val contact = if (contactsAlerted == 1)
            "contact alerte" else "contacts alertes"
        return "$incidentCount $incident" +
                " · $contactsAlerted $contact"
    }
}
