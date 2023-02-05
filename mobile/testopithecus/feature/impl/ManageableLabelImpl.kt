package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findByText
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.mail.testopithecus.steps.getRecyclerItemByText
import com.yandex.mail.testopithecus.steps.getTextFromResources
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.ContainerDeletionMethod
import com.yandex.xplat.testopithecus.LabelName
import com.yandex.xplat.testopithecus.ManageableLabel

class ManageableLabelImpl(private val device: UiDevice) : ManageableLabel {
    override fun openLabelManager() {
        device.getRecyclerItemByText(getTextFromResources(R.string.sidebar_headline_labels_title_caps)).parent.find("folder_list_item_clear")?.click()
    }

    override fun closeLabelManager() {
        device.find("toolbar").children[0].click()
    }

    override fun openCreateLabelScreen() {
        device.find("add_menu_action_add").click()
    }

    override fun closeCreateLabelScreen() {
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up")).perform(ViewActions.click())
    }

    override fun enterNameForNewLabel(labelName: LabelName) {
        device.find("settings_new_label_edit_text").text = labelName
    }

    override fun getCurrentNewLabelName(): LabelName {
        val labelName = device.find("settings_new_label_edit_text").text
        if (labelName == "Name") {
            return ""
        } else
            return labelName
    }

    override fun setNewLabelColor(index: Int) {
        device.find("settings_new_label_color_choose").children[index].click()
    }

    override fun getCurrentNewLabelColorIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun submitNewLabel() {
        device.pressBack()
        device.find("settings_new_label_ok_button").click()
    }

    override fun openEditLabelScreen(labelName: LabelName) {
        device.findByText(labelName).click()
    }

    override fun closeEditLabelScreen() {
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up")).perform(ViewActions.click())
    }

    override fun enterNameForEditedLabel(labelName: LabelName) {
        device.find("settings_new_label_edit_text").text = labelName
    }

    override fun getCurrentEditedLabelName(): LabelName {
        return device.find("settings_new_label_edit_text").text
    }

    override fun getCurrentEditedLabelColorIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun setEditedLabelColor(index: Int) {
        device.find("settings_new_label_color_choose").children[index].click()
    }

    override fun submitEditedLabel() {
        device.find("settings_new_label_ok_button").click()
    }

    override fun deleteLabel(labelName: LabelName, deletionMethod: ContainerDeletionMethod) {
        device.findByText(labelName).click()
        device.find("action_delete").click()
        device.findByText(getTextFromResources(R.string.labels_settings_delete_label_button).toUpperCase()).click()
    }

    override fun getLabelList(): YSArray<LabelName> {
        return device
            .findMany("settings_label_list_item_text")
            .map { it.text }
            .toMutableList()
    }
}
