package com.roadalert.cameroun.ui.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.roadalert.cameroun.R
import com.roadalert.cameroun.databinding.ActivityHistoryBinding
import com.roadalert.cameroun.util.LocaleHelper
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private lateinit var binding: ActivityHistoryBinding

    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(applicationContext)
    }

    private lateinit var adapter: HistoryAdapter

    // ── Index tab actif — 0=Tous 1=Accidents 2=SOS ────────
    private var activeTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        setupRecyclerView()
        setupTabs()
        observeViewModel()
    }

    // ── Bouton retour ─────────────────────────────────────

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    // ── RecyclerView ──────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onCardClick = {
                Snackbar.make(
                    binding.root,
                    getString(R.string.history_snackbar_tap),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        )
        binding.rvHistory.adapter = adapter
        binding.rvHistory.layoutManager =
            LinearLayoutManager(this)
        binding.rvHistory.setHasFixedSize(false)
    }

    // ── Tabs filtres ──────────────────────────────────────

    private fun setupTabs() {
        binding.tabAll.setOnClickListener {
            viewModel.setFilter(HistoryFilter.ALL)
            updateTabUI(0)
        }
        binding.tabAccidents.setOnClickListener {
            viewModel.setFilter(HistoryFilter.AUTO)
            updateTabUI(1)
        }
        binding.tabSos.setOnClickListener {
            viewModel.setFilter(HistoryFilter.MANUAL)
            updateTabUI(2)
        }
    }

    private fun updateTabUI(selectedIndex: Int) {
        activeTabIndex = selectedIndex

        // Réinitialiser tous les tabs
        binding.tabAll.setTextColor(0xFF7F8C8D.toInt())
        binding.tabAll.setTypeface(null,
            android.graphics.Typeface.NORMAL)
        binding.tabAccidents.setTextColor(0xFF7F8C8D.toInt())
        binding.tabAccidents.setTypeface(null,
            android.graphics.Typeface.NORMAL)
        binding.tabSos.setTextColor(0xFF7F8C8D.toInt())
        binding.tabSos.setTypeface(null,
            android.graphics.Typeface.NORMAL)

        // Activer le tab sélectionné
        val activeTab = when (selectedIndex) {
            0 -> binding.tabAll
            1 -> binding.tabAccidents
            else -> binding.tabSos
        }
        activeTab.setTextColor(0xFFC0392B.toInt())
        activeTab.setTypeface(null,
            android.graphics.Typeface.BOLD)

        // Déplacer l'indicateur sous le tab actif
        val tabWidth = binding.tabAll.width
        val indicatorX = selectedIndex * tabWidth
        binding.tabIndicator.animate()
            .translationX(indicatorX.toFloat())
            .setDuration(150)
            .start()
    }

    // ── Observer ViewModel ────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is HistoryUiState.Loading ->
                        showLoading()

                    is HistoryUiState.Empty ->
                        showEmpty()

                    is HistoryUiState.EmptyFilter ->
                        showEmptyFilter(state.filter)

                    is HistoryUiState.Content -> {
                        showContent()
                        adapter.submitList(state.items)
                        binding.tvStats.text =
                            state.stats.toDisplayString()
                        binding.tvStats.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    // ── États visuels ─────────────────────────────────────

    private fun showLoading() {
        binding.pbLoading.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
        binding.llEmpty.visibility = View.GONE
        binding.llEmptyFilter.visibility = View.GONE
        binding.tvStats.visibility = View.GONE
    }

    private fun showEmpty() {
        binding.pbLoading.visibility = View.GONE
        binding.rvHistory.visibility = View.GONE
        binding.llEmpty.visibility = View.VISIBLE
        binding.llEmptyFilter.visibility = View.GONE
        binding.tvStats.visibility = View.GONE
    }

    private fun showEmptyFilter(filter: HistoryFilter) {
        binding.pbLoading.visibility = View.GONE
        binding.rvHistory.visibility = View.GONE
        binding.llEmpty.visibility = View.GONE
        binding.llEmptyFilter.visibility = View.VISIBLE
        binding.tvStats.visibility = View.GONE

        // Message adapté selon le filtre actif
        binding.tvEmptyFilterMessage.text = when (filter) {
            HistoryFilter.AUTO ->
                getString(
                    R.string.history_empty_filter_accidents_title
                )
            HistoryFilter.MANUAL ->
                getString(
                    R.string.history_empty_filter_sos_title
                )
            HistoryFilter.ALL ->
                getString(R.string.history_empty_title)
        }

        binding.tvEmptyFilterSub.text =
            getString(R.string.history_empty_filter_sub)
    }

    private fun showContent() {
        binding.pbLoading.visibility = View.GONE
        binding.rvHistory.visibility = View.VISIBLE
        binding.llEmpty.visibility = View.GONE
        binding.llEmptyFilter.visibility = View.GONE
    }
}