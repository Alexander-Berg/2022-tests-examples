package ru.yandex.yandexmaps.app

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.times
import org.mockito.internal.verification.VerificationModeFactory
import ru.yandex.maps.utils.CaptorUtils
import ru.yandex.yandexmaps.app.lifecycle.ActivityLifecycleFilter
import ru.yandex.yandexmaps.app.lifecycle.AppLifecycleDelegation
import ru.yandex.yandexmaps.app.lifecycle.AppLifecycleDelegation.Suspendable
import ru.yandex.yandexmaps.app.lifecycle.AppLifecycleDelegationImpl

class AppLifecycleDelegationImplTest {

    private lateinit var appLifecycleDelegation: AppLifecycleDelegation
    private lateinit var mockSuspendable: Suspendable
    private lateinit var lifecycleCallbacks: ActivityLifecycleCallbacks

    private val activity = object : Activity() {}
    private val filter = object : ActivityLifecycleFilter {
        override fun filter(activity: Activity): Boolean = true
    }

    @Before
    fun setUp() {
        val mockApp = mock<Application>()
        mockSuspendable = mock()
        appLifecycleDelegation = AppLifecycleDelegationImpl(mockApp, filter)
        val captor = CaptorUtils.captor(ActivityLifecycleCallbacks::class.java)
        verify(mockApp).registerActivityLifecycleCallbacks(captor.capture())
        lifecycleCallbacks = captor.value
    }

    @Test
    fun resume_whenFirstActivityStart() {
        appLifecycleDelegation.suspendAlongLifecycle(mockSuspendable, true)
        verify(mockSuspendable).suspend()
        lifecycleCallbacks.onActivityStarted(activity)
        verify(mockSuspendable).resume()
        verifyNoMoreInteractions(mockSuspendable)
    }

    @Test
    fun suspend_whenFirstActivityStop() {
        appLifecycleDelegation.suspendAlongLifecycle(mockSuspendable, true)
        verify(mockSuspendable).suspend()
        lifecycleCallbacks.onActivityStarted(activity)
        verify(mockSuspendable).resume()
        lifecycleCallbacks.onActivityStopped(activity)
        verify(mockSuspendable, times(2)).suspend()
        verifyNoMoreInteractions(mockSuspendable)
    }

    @Test
    fun resume_whenActivityAlreadyStarted() {
        lifecycleCallbacks.onActivityStarted(activity)
        appLifecycleDelegation.suspendAlongLifecycle(mockSuspendable, true)
        verify(mockSuspendable).resume()
        verifyNoMoreInteractions(mockSuspendable)
    }

    @Test
    fun suspend_whenActivityStillStopped() {
        lifecycleCallbacks.onActivityStarted(activity)
        lifecycleCallbacks.onActivityStopped(activity)
        appLifecycleDelegation.suspendAlongLifecycle(mockSuspendable, true)
        verify(mockSuspendable).suspend()
        verifyNoMoreInteractions(mockSuspendable)
    }

    @Test
    fun suspend_whenActivityNeverStarted() {
        appLifecycleDelegation.suspendAlongLifecycle(mockSuspendable, true)
        verify(mockSuspendable).suspend()
        verifyNoMoreInteractions(mockSuspendable)
    }

    @Test
    fun resume_notCalledTwice() {
        lifecycleCallbacks.onActivityStarted(activity)
        appLifecycleDelegation.suspendAlongLifecycle(mockSuspendable, true)
        verify(mockSuspendable).resume()
        lifecycleCallbacks.onActivityStarted(activity)
        verifyNoMoreInteractions(mockSuspendable)
    }

    @Test
    fun suspend_notCalledTwice() {
        lifecycleCallbacks.onActivityStarted(activity)
        lifecycleCallbacks.onActivityStopped(activity)
        appLifecycleDelegation.suspendAlongLifecycle(mockSuspendable, true)
        verify(mockSuspendable).suspend()
        lifecycleCallbacks.onActivityStopped(activity)
        verifyNoMoreInteractions(mockSuspendable)
    }

    @Test
    fun callbackNotFired_whenActivitiesLiveInWrongOrder() {
        appLifecycleDelegation.suspendAlongLifecycle(mockSuspendable, true)
        verify(mockSuspendable).suspend()
        lifecycleCallbacks.onActivityStarted(activity)
        verify(mockSuspendable).resume()
        lifecycleCallbacks.onActivityStarted(activity)
        lifecycleCallbacks.onActivityStopped(activity)
        verifyNoMoreInteractions(mockSuspendable)
    }

    @Test
    fun callbackFired_whenWeSubscribingInMiddleOfActivityLifecycle() {
        appLifecycleDelegation.suspendAlongLifecycle(mockSuspendable, true)
        verify(mockSuspendable).suspend()
        lifecycleCallbacks.onActivityStopped(activity)
        lifecycleCallbacks.onActivityStarted(activity)
        verify(mockSuspendable).resume()
        verifyNoMoreInteractions(mockSuspendable)
    }

    @Test
    fun noSuspend_whenAddedWithFalseNotifyFlag() {
        appLifecycleDelegation.suspendAlongLifecycle(mockSuspendable, false)
        verify(mockSuspendable, VerificationModeFactory.noMoreInteractions()).suspend()
        lifecycleCallbacks.onActivityStarted(activity)
        verify(mockSuspendable).resume()
        verifyNoMoreInteractions(mockSuspendable)
    }
}
