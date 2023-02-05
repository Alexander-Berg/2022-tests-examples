package com.yandex.frankenstein.agent.client

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Intent
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

private const val ID = 42

@RunWith(RobolectricTestRunner::class)
class BroadcastObserverGeneralTest {

    @Mock
    private lateinit var commandInput: CommandInput
    @Mock
    private lateinit var activity: Activity
    @Mock
    private lateinit var application: Application
    @Mock
    private lateinit var intent: Intent
    @Captor
    private lateinit var receiverCaptor: ArgumentCaptor<BroadcastReceiver>
    private val basicJsonObject = JSONObject().put("id", ID).put("intent_filter", JSONObject())
    private val oneShotKey = "one_shot"

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(activity.application).thenReturn(application)
        `when`(commandInput.activity).thenReturn(activity)
    }

    @Test
    fun awaitBroadcastReceiveOneShot() {
        `when`(commandInput.arguments).thenReturn(basicJsonObject.put(oneShotKey, true))
        awaitBroadcastReceive(commandInput)
        val receiver = interceptReceiverAndOnReceive()
        verify(application).unregisterReceiver(receiver)
    }

    @Test
    fun awaitBroadcastReceiveOneShotByDefault() {
        `when`(commandInput.arguments).thenReturn(basicJsonObject)
        awaitBroadcastReceive(commandInput)
        val receiver = interceptReceiverAndOnReceive()
        verify(application).unregisterReceiver(receiver)
    }

    @Test
    fun awaitBroadcastReceiveNotOneShot() {
        `when`(commandInput.arguments).thenReturn(basicJsonObject.put(oneShotKey, false))
        awaitBroadcastReceive(commandInput)
        val receiver = interceptReceiverAndOnReceive()
        verify(application, never()).unregisterReceiver(receiver)
    }

    @Test
    fun unregisterReceiverByKey() {
        `when`(commandInput.arguments).thenReturn(basicJsonObject.put(oneShotKey, false))
        awaitBroadcastReceive(commandInput)
        verify(commandInput).reportResult(any())
        val receiver = interceptReceiverAndOnReceive()
        verify(application, never()).unregisterReceiver(any())
        unregisterReceiver(commandInput)
        verify(application).unregisterReceiver(receiver)
        verify(commandInput, times(2)).reportResult(any())
    }

    @Test
    fun unregisterReceiverTwice() {
        `when`(commandInput.arguments).thenReturn(basicJsonObject.put(oneShotKey, false))
        awaitBroadcastReceive(commandInput)
        val receiver = interceptReceiverAndOnReceive()
        unregisterReceiver(commandInput)
        verify(application).unregisterReceiver(receiver)
        unregisterReceiver(commandInput)
        verify(application, times(1)).unregisterReceiver(receiver)
    }

    private fun interceptReceiverAndOnReceive(): BroadcastReceiver {
        verify(application).registerReceiver(receiverCaptor.capture(), any())
        val receiver = receiverCaptor.value
        receiver.onReceive(application, intent)
        return receiver
    }
}
