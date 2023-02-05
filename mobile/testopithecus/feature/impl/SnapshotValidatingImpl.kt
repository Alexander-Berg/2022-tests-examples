package com.yandex.mail.testopithecus.feature.impl

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.icu.text.Transliterator
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.uiautomator.UiDevice
import com.karumi.shot.ScreenshotTest
import com.yandex.mail.testopithecus.steps.getElementFromMatchAtPosition
import com.yandex.mail.testopithecus.steps.getElementsCount
import com.yandex.mail.testopithecus.steps.nthChildOf
import com.yandex.mail.testopithecus.steps.setViewVisibility
import com.yandex.xplat.common.minInt32
import com.yandex.xplat.testopithecus.SnapshotValidating
import io.qameta.allure.kotlin.Allure

class SnapshotValidatingImpl(private val device: UiDevice) : SnapshotValidating, ScreenshotTest {

    val elementsToHide = listOf(
        withResourceName("date_time")!!, // время прихода письма
        nthChildOf(withResourceName("tabbar_calendar"), 0)!!, // иконка календарям меняет номер дня
        withResourceName("account_switcher_title")!!, // имя пользователя
        withResourceName("account_switcher_subtitle")!!, // адрес пользователя
        withResourceName("account_switcher_item_icon")!!, // аватарка пользователя
    )

    override fun verifyScreen(componentName: String, testName: String) {
        Allure.step("Сравниваем скриншоты") {
            val screenName = transliterateToFileName(
                testName
                    .subSequence(0, minInt32(testName.length, 75)).toString()
                    .replace('.', '_')
                    .replace(' ', '_')
            )
            Thread.sleep((5 * 1000).toLong())
            changeIgnoredElementsVisibility(false)
            compareScreenshot(getCurrentActivity(), name = screenName)
            changeIgnoredElementsVisibility(true)
        }
    }

    private fun changeIgnoredElementsVisibility(visibility: Boolean) {
        for (element in elementsToHide) {
            val countElements = getElementsCount(element)
            for (i in 0 until countElements) {
                val elementToHide = onView(getElementFromMatchAtPosition(element, i))
                elementToHide.perform(setViewVisibility(visibility))
            }
        }
    }

    private fun getCurrentActivity(): Activity {
        return Allure.step("Получаем root активити для скриншота") {
            val activity: Array<Activity?> = arrayOfNulls<Activity>(1)
            onView(isRoot()).check { view, _ ->
                activity[0] = unwrap((view.findViewById(android.R.id.content) as View).getContext())
            }
            return@step activity[0]!!
        }
    }

    private fun unwrap(context: Context): Activity? {
        var context: Context = context
        while (context !is Activity && context is ContextWrapper) {
            context = (context).baseContext
        }
        return context as Activity
    }

    private fun transliterateToFileName(st: String): String {
        val toLatinTrans = Transliterator.getInstance("Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC; [ʹ] Remove")
        return toLatinTrans.transliterate(st)
    }
}
