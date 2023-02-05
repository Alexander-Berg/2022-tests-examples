package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.xplat.testopithecus.LanguageName
import com.yandex.xplat.testopithecus.MessageViewerAndroid
import com.yandex.xplat.testopithecus.TranslatorLanguageName
import io.qameta.allure.kotlin.Allure

class MessageViewerAndroidImpl(private val device: UiDevice) : MessageViewerAndroid {
    override fun deleteMessageByIcon() {
        Allure.step("Удаляем открытое сообщение") {
            Espresso.onView(ViewMatchers.withId((R.id.action_delete))).perform(ViewActions.click())
        }
    }

    override fun getDefaultSourceLanguage(): LanguageName {
        return TranslatorLanguageName.select
    }
}
