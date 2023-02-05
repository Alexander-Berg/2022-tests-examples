package com.yandex.mail.tools

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class NoOpWorker(context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        return Result.success()
    }
}
