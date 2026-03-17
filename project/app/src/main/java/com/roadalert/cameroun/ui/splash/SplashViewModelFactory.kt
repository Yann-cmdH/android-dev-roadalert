package com.roadalert.cameroun.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.roadalert.cameroun.data.db.AppDatabase
import android.content.Context

class SplashViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SplashViewModel(
                AppDatabase.buildRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}