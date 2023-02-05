package com.yandex.mail.react.translator

import android.os.Bundle
import com.yandex.mail.R
import com.yandex.mail.react.model.MessageBodyLoader
import com.yandex.mail.react.translator.LanguagesAdapter.Language
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.tools.Accounts
import com.yandex.mail.util.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(IntegrationTestRunner::class)
class ReactTranslatorHolderTest : BaseIntegrationTest() {

    lateinit var holder: ReactTranslatorsHolder

    lateinit var messageBodyLoader: MessageBodyLoader

    @Before
    fun setUp() {
        init(Accounts.testLoginData)
        messageBodyLoader = MessageBodyLoader(app, user.uid)
        holder = ReactTranslatorsHolder(messageBodyLoader)
    }

    @Test
    fun `holder should persist configuration change`() {
        val bundle = Bundle()

        val translator1 = holder.resolveNewTranslator(
            app.resources,
            1L,
            Language("Russian", "ru"),
            Language("Korean", "kr")
        )

        val translator2 = holder.resolveNewTranslator(
            app.resources,
            2L,
            Language("Korean", "kr"),
            Language("Czech", "cz")
        )

        holder.onSaveState(bundle)

        holder = ReactTranslatorsHolder(messageBodyLoader)

        holder.onRestoreState(bundle)

        assertThat(holder.getTranslatorMeta(1L)!!.reactTranslator).isEqualTo(translator1.reactTranslator)
        assertThat(holder.getTranslatorMeta(2L)!!.reactTranslator).isEqualTo(translator2.reactTranslator)
    }

    @Test
    fun `showError should show different message for no internet error`() {
        val mid = 1L

        holder.resolveNewTranslator(
            app.resources,
            mid,
            Language("Russian", "ru"),
            Language("Korean", "kr")
        )

        holder.showError(app.resources, mid, R.string.connection_error)

        assertThat(holder.getTranslatorMeta(mid)!!.reactTranslator.errorMessage).isEqualTo(app.getString(R.string.connection_error).toLowerCase())

        holder.showError(app.resources, mid, R.string.translator_error_message)

        assertThat(holder.getTranslatorMeta(mid)!!.reactTranslator.errorMessage)
            .isEqualTo(app.getString(R.string.translator_error_message).toLowerCase())
    }
}
