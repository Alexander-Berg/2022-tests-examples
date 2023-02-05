package com.yandex.launcher.alice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper.getMainLooper
import org.mockito.kotlin.*
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.speech.VOICE_SEARCH_CLOSED_ACTION
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.robolectric.Shadows.shadowOf

class ReportVoiceSearchClosedTest: BaseRobolectricTest() {

    private val mockReceiver = mock<BroadcastReceiver>()
    private val receiver = BroadCastReceiverForTest(mockReceiver)
    private val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
    private lateinit var context: ContextWrapper

    override fun setUp() {
        super.setUp()
        context = ContextWrapper(appContext)
        context.registerReceiver(receiver, IntentFilter(VOICE_SEARCH_CLOSED_ACTION))
    }

    @Ignore("Some issues with mockito")
    @Test
    fun `reported intent has correct action`() {
        verify(mockReceiver, never())

        VoiceSearchClosedReporter.reportVoiceSearchClosed(context)
        shadowOf(getMainLooper()).idle()
        verify(mockReceiver).onReceive(any(), intentCaptor.capture())
        assertThat(intentCaptor.value.action, equalTo(VOICE_SEARCH_CLOSED_ACTION))
    }

    @Test
    fun `reported intent has Launcher's package`() {
        verifyNoInteractions(mockReceiver)

        VoiceSearchClosedReporter.reportVoiceSearchClosed(context)
        shadowOf(getMainLooper()).idle()
        verify(mockReceiver).onReceive(any(), intentCaptor.capture())
        // it is necessary, to pass raw package name(i.e. to not use constant like BuildConfig)
        // if package will be changed,  test will fail and notify about it
        assertThat(intentCaptor.value.`package`, equalTo("com.yandex.launcher"))
    }
}

private open class BroadCastReceiverForTest(private val mockReceiver: BroadcastReceiver): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) { mockReceiver.onReceive(context, intent) }
}
