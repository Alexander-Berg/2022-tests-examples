package com.yandex.mobile.realty.worker

import android.content.Context
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.yandex.mobile.realty.plugin.CorePlugin

/**
 * @author rogovalex on 31.01.2022
 */
class TestWorkManagerPlugin(
    private val workerFactory: DelegatingWorkerFactory
) : CorePlugin {

    override fun name(): String {
        return "TestWorkManagerPlugin"
    }

    override fun onSetup(context: Context, mainProcess: Boolean) {
        val synchronousExecutor = SynchronousExecutor()
        val configuration = Configuration.Builder()
            .setExecutor(synchronousExecutor)
            .setTaskExecutor(synchronousExecutor)
            .setWorkerFactory(workerFactory)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
    }
}
