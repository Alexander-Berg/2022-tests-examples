package com.yandex.mail.util

import android.app.Application
import android.content.Context
import android.content.Intent
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.mail.BuildConfig
import com.yandex.mail.FeaturesConfig
import com.yandex.mail.runners.IntegrationTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.robolectric.RuntimeEnvironment

@RunWith(IntegrationTestRunner::class)
class IntentFactoryTest {

    val context: Context = spy<Application>(RuntimeEnvironment.application)

    @Before
    fun setUp() {
        whenever(context.packageName).thenReturn(BuildConfig.APPLICATION_ID)
    }

    @Test
    fun `getDirectGooglePlayUri should return correct uri`() {
        assertThat(IntentFactory.getDirectGooglePlayUri(context).toString())
            .isEqualTo(getDirectGooglePlayUri())
    }

    @Test
    fun `getIndirectGooglePlayUri should return correct uri`() {
        assertThat(IntentFactory.getIndirectGooglePlayUri(context).toString())
            .isEqualTo(getIndirectGooglePlayUri())
    }

    @Test
    fun `newDirectGooglePlayIntent should return correct intent`() {
        val intent = IntentFactory.newDirectGooglePlayIntent(context)
        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.data!!.toString())
            .isEqualTo(getDirectGooglePlayUri())
    }

    @Test
    fun `newIndirectGooglePlayIntent should return correct intent`() {
        val intent = IntentFactory.newIndirectGooglePlayIntent(context)
        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.data!!.toString())
            .isEqualTo(getIndirectGooglePlayUri())
    }

    private fun getDirectGooglePlayUri() =
        "market://details?id=" +
            (if (FeaturesConfig.BETA) "ru.yandex.mail.beta" else "ru.yandex.mail")

    private fun getIndirectGooglePlayUri() =
        "http://play.google.com/store/apps/details?id=" +
            (if (FeaturesConfig.BETA) "ru.yandex.mail.beta" else "ru.yandex.mail")
}
