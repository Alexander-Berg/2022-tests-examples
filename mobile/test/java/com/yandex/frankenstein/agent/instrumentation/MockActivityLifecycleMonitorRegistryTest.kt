package com.yandex.frankenstein.agent.instrumentation

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.test.internal.runner.lifecycle.ActivityLifecycleMonitorImpl
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions

class MockActivityLifecycleMonitorRegistryTest {

    private val application = mock(Application::class.java)
    private val activity = mock(Activity::class.java)
    private val bundle = mock(Bundle::class.java)
    private val activityLifecycleMonitor = mock(ActivityLifecycleMonitorImpl::class.java)

    @Test
    fun testGetRealInstance() {
        registerMockActivityLifecycleMonitor(application)
        val actualMonitor = ActivityLifecycleMonitorRegistry.getInstance()

        assertThat(actualMonitor).isInstanceOf(ActivityLifecycleMonitorImpl::class.java)
    }

    @Test
    fun testGetInstance() {
        registerMockActivityLifecycleMonitor(activityLifecycleMonitor, application)
        val actualMonitor = ActivityLifecycleMonitorRegistry.getInstance()

        assertThat(actualMonitor).isEqualTo(activityLifecycleMonitor)
    }

    @Test
    fun testPauseActivity() {
        registerMockActivityLifecycleMonitor(activityLifecycleMonitor, application)
        getRegisteredActivityLifecycleCallbacks().onActivityPaused(activity)

        verify(activityLifecycleMonitor).signalLifecycleChange(Stage.PAUSED, activity)
    }

    @Test
    fun testResumeActivity() {
        registerMockActivityLifecycleMonitor(activityLifecycleMonitor, application)
        getRegisteredActivityLifecycleCallbacks().onActivityResumed(activity)

        verify(activityLifecycleMonitor).signalLifecycleChange(Stage.RESUMED, activity)
    }

    @Test
    fun testStartActivity() {
        registerMockActivityLifecycleMonitor(activityLifecycleMonitor, application)
        getRegisteredActivityLifecycleCallbacks().onActivityStarted(activity)

        verify(activityLifecycleMonitor).signalLifecycleChange(Stage.STARTED, activity)
    }

    @Test
    fun testDestroyActivity() {
        registerMockActivityLifecycleMonitor(activityLifecycleMonitor, application)
        getRegisteredActivityLifecycleCallbacks().onActivityDestroyed(activity)

        verify(activityLifecycleMonitor).signalLifecycleChange(Stage.DESTROYED, activity)
    }

    @Test
    fun testSaveActivityInstanceState() {
        registerMockActivityLifecycleMonitor(activityLifecycleMonitor, application)
        getRegisteredActivityLifecycleCallbacks().onActivitySaveInstanceState(activity, bundle)

        verifyZeroInteractions(activityLifecycleMonitor)
    }

    @Test
    fun testStopActivity() {
        registerMockActivityLifecycleMonitor(activityLifecycleMonitor, application)
        getRegisteredActivityLifecycleCallbacks().onActivityStopped(activity)

        verify(activityLifecycleMonitor).signalLifecycleChange(Stage.STOPPED, activity)
    }

    @Test
    fun testCreateActivity() {
        registerMockActivityLifecycleMonitor(activityLifecycleMonitor, application)
        getRegisteredActivityLifecycleCallbacks().onActivityCreated(activity, bundle)

        verify(activityLifecycleMonitor).signalLifecycleChange(Stage.CREATED, activity)
    }

    private fun getRegisteredActivityLifecycleCallbacks(): ActivityLifecycleCallbacks {
        val callbacksCaptor = ArgumentCaptor.forClass(ActivityLifecycleCallbacks::class.java)
        verify(application).registerActivityLifecycleCallbacks(callbacksCaptor.capture())

        return callbacksCaptor.value
    }
}
