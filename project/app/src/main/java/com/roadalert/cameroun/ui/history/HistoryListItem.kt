package com.roadalert.cameroun.ui.history

import com.roadalert.cameroun.data.db.entity.AccidentEvent

sealed class HistoryListItem {
    data class Header(
        val title: String
    ) : HistoryListItem()

    data class Event(
        val event: AccidentEvent
    ) : HistoryListItem()
}