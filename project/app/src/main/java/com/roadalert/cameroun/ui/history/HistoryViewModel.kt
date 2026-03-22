package com.roadalert.cameroun.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadalert.cameroun.data.db.entity.AccidentEvent
import com.roadalert.cameroun.data.db.entity.SmsStatus
import com.roadalert.cameroun.data.repository.AccidentRepository
import com.roadalert.cameroun.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class HistoryViewModel(
    private val accidentRepository: AccidentRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState

    private val _activeFilter =
        MutableStateFlow(HistoryFilter.ALL)
    val activeFilter: StateFlow<HistoryFilter> = _activeFilter

    // Cache de tous les événements reçus depuis Room
    private var allEvents: List<AccidentEvent> = emptyList()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val user = userProfileRepository.getUserSync()
            if (user == null) {
                _uiState.value = HistoryUiState.Empty
                return@launch
            }

            accidentRepository
                .getAllAccidentEvents(user.id)
                .collect { events ->
                    allEvents = events
                    processEvents(events, _activeFilter.value)
                }
        }
    }

    fun setFilter(filter: HistoryFilter) {
        _activeFilter.value = filter
        processEvents(allEvents, filter)
    }

    private fun processEvents(
        events: List<AccidentEvent>,
        filter: HistoryFilter
    ) {
        if (events.isEmpty()) {
            _uiState.value = HistoryUiState.Empty
            return
        }

        // Filtrer selon le filtre actif
        val filtered = when (filter) {
            HistoryFilter.ALL -> events
            HistoryFilter.AUTO -> events.filter {
                it.triggerType == "AUTO"
            }
            HistoryFilter.MANUAL -> events.filter {
                it.triggerType == "MANUAL"
            }
        }

        if (filtered.isEmpty()) {
            _uiState.value = HistoryUiState.EmptyFilter(filter)
            return
        }

        // Grouper par date
        val groups = groupByDate(filtered)

        // Construire liste plate pour RecyclerView
        val items = mutableListOf<HistoryListItem>()
        for ((title, groupEvents) in groups) {
            items.add(HistoryListItem.Header(title))
            groupEvents.forEach { event ->
                items.add(HistoryListItem.Event(event))
            }
        }

        // Calculer stats
        val stats = calculateStats(filtered)

        _uiState.value = HistoryUiState.Content(
            items = items,
            stats = stats,
            filter = filter
        )
    }

    private fun groupByDate(
        events: List<AccidentEvent>
    ): LinkedHashMap<String, List<AccidentEvent>> {

        val result = LinkedHashMap<String, MutableList<AccidentEvent>>()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        val startOfWeek = startOfToday - 6L * 24 * 60 * 60 * 1000
        val startOfMonth = startOfToday - 29L * 24 * 60 * 60 * 1000

        for (event in events) {
            val key = when {
                event.timestamp >= startOfToday ->
                    "AUJOURD'HUI"
                event.timestamp >= startOfWeek ->
                    "CETTE SEMAINE"
                event.timestamp >= startOfMonth ->
                    "CE MOIS"
                else ->
                    "PLUS ANCIEN"
            }
            result.getOrPut(key) { mutableListOf() }.add(event)
        }

        return LinkedHashMap(result)
    }

    private fun calculateStats(
        events: List<AccidentEvent>
    ): HistoryStats {
        val incidentCount = events.size
        val contactsAlerted = events
            .filter {
                it.alertStatus == "SENT" ||
                        it.alertStatus == "PARTIAL"
            }
            .sumOf { event ->
                var count = 0
                if (event.smsContact1Status ==
                    SmsStatus.SENT.name) count++
                if (event.smsContact2Status ==
                    SmsStatus.SENT.name) count++
                if (event.smsContact3Status ==
                    SmsStatus.SENT.name) count++
                count
            }

        return HistoryStats(
            incidentCount = incidentCount,
            contactsAlerted = contactsAlerted
        )
    }
}
