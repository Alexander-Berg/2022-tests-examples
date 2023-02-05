package com.yandex.mail.tools

import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.schedulers.TestScheduler

/*
 * Helper for robolectric integrations tests. Should be used to sync calls on MainScheduler for complicated tests like MailActivityTests
 */
class MainSchedulerSynchronizer(private val testScheduler: TestScheduler) {

    fun sync() {
        testScheduler.triggerActions()
    }

    companion object {

        @JvmStatic
        fun create(): MainSchedulerSynchronizer {
            val testScheduler = TestScheduler()
            RxAndroidPlugins.setMainThreadSchedulerHandler { testScheduler }
            return MainSchedulerSynchronizer(testScheduler)
        }

        @JvmStatic
        fun sync(run: Runnable) {
            val syncronizer = create()
            run.run()
            syncronizer.sync()
        }
    }
}
