package com.roadalert.cameroun.ui.history

sealed class HistoryUiState {

    // Chargement initial depuis Room
    object Loading : HistoryUiState()

    // Aucun événement en DB
    object Empty : HistoryUiState()

    // Filtre actif mais aucun résultat
    data class EmptyFilter(
        val filter: HistoryFilter
    ) : HistoryUiState()

    // Données disponibles
    data class Content(
        val items: List<HistoryListItem>,
        val stats: HistoryStats,
        val filter: HistoryFilter
    ) : HistoryUiState()
}