package com.roadalert.cameroun.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.db.entity.AlertStatus
import com.roadalert.cameroun.data.repository.AccidentRepository

class RetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val repository = AccidentRepository(
                db.accidentEventDAO()
            )
            val pending = repository.getPendingRetries()
            pending.forEach { event ->
                repository.incrementRetryCount(event.id)
                if (event.retryCount >= 3) {
                    repository.updateStatus(
                        event.id,
                        AlertStatus.FAILED
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}