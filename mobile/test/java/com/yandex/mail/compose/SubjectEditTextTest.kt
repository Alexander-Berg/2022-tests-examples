package com.yandex.mail.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.yandex.mail.runners.IntegrationTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

@RunWith(IntegrationTestRunner::class)
class SubjectEditTextTest {

    private val clipboard = RuntimeEnvironment.application
        .applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Test
    fun simpleTextTest() {
        val text = "text"

        clipboard.setText(text)

        val editText = SubjectEditText(RuntimeEnvironment.application)
        editText.onTextContextMenuItem(android.R.id.paste)

        assertThat(editText.text.toString()).isEqualTo(text)
    }

    @Test
    fun htmlTextTest() {
        val text = "yandex"

        val clip = ClipData.newHtmlText("label", text, "<a href=\"https://ya.ru\">$text</a>")
        clipboard.setPrimaryClip(clip)

        val editText = SubjectEditText(RuntimeEnvironment.application)
        editText.onTextContextMenuItem(android.R.id.paste)

        assertThat(editText.text.toString()).isEqualTo(text)
    }

    @Test
    fun multilineText() {
        val text = "yandex" + System.getProperty("line.separator") + "mail"

        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)

        val editText = SubjectEditText(RuntimeEnvironment.application)
        editText.onTextContextMenuItem(android.R.id.paste)

        assertThat(editText.text.toString()).isEqualTo("yandex mail")
    }
}
