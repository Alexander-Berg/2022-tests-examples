package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findByText
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.ApplyLabel
import com.yandex.xplat.testopithecus.LabelName
import io.qameta.allure.kotlin.Allure
import org.hamcrest.CoreMatchers.allOf

class ApplyLabelImpl(private val device: UiDevice) : ApplyLabel {
    override fun selectLabelsToAdd(labelNames: YSArray<LabelName>) {
        Allure.step("Выделяем метки $labelNames для добавления") {
            device.find("item_label_dialog_text")
            for (labelName in labelNames) {
                device.findByText(labelName).click()
            }
        }
    }

    override fun deselectLabelsToRemove(labelNames: YSArray<LabelName>) {
        Allure.step("Снимаем выделение с меток $labelNames для удаления") {
            device.find("item_label_dialog_text")
            for (labelName in labelNames){
                device.findByText(labelName).click()
            }
        }
    }

    override fun tapOnDoneButton() {
        Allure.step("Нажимаем на кнопку Done") {
            onView(allOf(withId(android.R.id.button1), withText("DONE"))).perform(ViewActions.click())
        }
    }

    override fun tapOnCreateLabel() {
        Allure.step("Открываем экран создания метки") {
            device.find("item_label_new_container").click()
            device.find("add_menu_action_add").click()
        }
    }

    override fun getSelectedLabels(): YSArray<LabelName> {
        TODO("Not yet implemented")
    }

    override fun getLabelList(): YSArray<LabelName> {
        return Allure.step("Получаем список меток") {
            return@step device.findMany("item_label_dialog_text").map { it.text }.toMutableList()
        }
    }
}
