package ru.auto.test.core

import org.junit.Before
import ru.auto.ara.RxTest
import rx.Scheduler
import rx.android.plugins.RxAndroidPlugins
import rx.android.plugins.RxAndroidSchedulersHook
import rx.schedulers.Schedulers

@Suppress("UnstableApiUsage")
abstract class RxTestAndroid: RxTest() {

    @Before
    fun setupRxJavaSchedulersForAndroid() {
        RxCustomizer.setupRxJavaSchedulers()
    }

    object RxCustomizer {
        private var isInitialSetup = true


        fun setupRxJavaSchedulers() {
            if (isInitialSetup) {
                // Setup to replace all schedulers with Schedulers.immediate()
                RxAndroidPlugins.getInstance().registerSchedulersHook(RxAndroidImmediate())
                isInitialSetup = false
            }
        }
    }

    private class RxAndroidImmediate : RxAndroidSchedulersHook() {
        override fun getMainThreadScheduler(): Scheduler = Schedulers.immediate()
    }
}
