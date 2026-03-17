package com.roadalert.cameroun.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.roadalert.cameroun.data.db.AppDatabase

class HomeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                AppDatabase.buildRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}