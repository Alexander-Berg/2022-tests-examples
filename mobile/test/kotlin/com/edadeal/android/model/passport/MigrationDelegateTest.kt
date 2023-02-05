package com.edadeal.android.model.passport

import com.edadeal.android.data.Prefs
import com.edadeal.android.di.ModuleLifecycle
import com.edadeal.android.helpers.StatefulMock
import com.edadeal.android.metrics.Metrics
import com.edadeal.android.metrics.YandexKit
import com.edadeal.android.model.ActivityProvider
import com.edadeal.android.model.Time
import com.edadeal.android.model.api.UsrApi
import com.edadeal.android.model.auth.passport.MigrationDelegate
import com.edadeal.android.model.auth.passport.PassportApiFacadeImpl
import com.edadeal.android.model.auth.passport.PassportContext
import com.edadeal.android.model.splashscreen.LaunchDelegate
import com.edadeal.android.model.splashscreen.LaunchHelper
import com.edadeal.android.model.splashscreen.LaunchState
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Before
import org.mockito.internal.verification.Times
import java.util.concurrent.TimeUnit

open class MigrationDelegateTest {
    protected val passportContext = PassportContext.TESTING
    protected val NOT_EMPTY_PASSPORT_UID = passportContext.getPassportUid(123L)

    private val scheduler = TestScheduler()
    lateinit var passportApi: PassportApiFacadeImpl
    lateinit var prefs: Prefs
    lateinit var launchHelper: LaunchHelper
    lateinit var launchState: LaunchState
    lateinit var launchDelegate: LaunchDelegate
    lateinit var metrics: Metrics
    lateinit var userApi: UsrApi
    lateinit var activityProvider: ActivityProvider
    lateinit var loginAction: (ActivityProvider, Any?) -> Unit
    lateinit var delegate: MigrationDelegate
    lateinit var time: Time
    lateinit var moduleLifecycle: ModuleLifecycle

    @Before
    fun beforeEach() {
        passportApi = mock()
        prefs = mock()
        launchHelper = mock()
        launchState = mock()
        launchDelegate = mock()
        metrics = mock()
        userApi = mock()
        activityProvider = mock()
        loginAction = mock()
        time = mock()
        moduleLifecycle = mock()
        whenever(launchHelper.yandexKit).then { mock<YandexKit>() }
        delegate = MigrationDelegate(moduleLifecycle, passportApi, prefs, time, launchHelper, launchState, launchDelegate, metrics, userApi, activityProvider, loginAction)
        RxJavaPlugins.setIoSchedulerHandler { scheduler }
    }

    @After
    fun teardown() {
        RxJavaPlugins.setIoSchedulerHandler(null)
    }

    protected fun TestObserver<*>.assertDelegateError(throwable: Throwable) {
        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        assertError(throwable::class.java)
        assertErrorMessage(throwable.message)
        verify(loginAction, Times(0)).invoke(any(), any())
        assertNotComplete()
    }

    protected fun TestObserver<*>.assertDelegateSuccess(amAllowedMock: StatefulMock<Boolean>) {
        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        assertComplete()
        assert(amAllowedMock.value)
    }
}
