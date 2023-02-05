package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findByText
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.mail.testopithecus.steps.formatEmail
import com.yandex.mail.testopithecus.steps.getRecyclerItemByText
import com.yandex.mail.testopithecus.steps.getTextFromResources
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.ContainerDeletionMethod
import com.yandex.xplat.testopithecus.FolderName
import com.yandex.xplat.testopithecus.ManageableFolder

class ManageableFolderImpl(private val device: UiDevice) : ManageableFolder {
    override fun openFolderManager() {
        device.getRecyclerItemByText(getTextFromResources(R.string.sidebar_headline_folders_title_caps)).parent.find("folder_list_item_clear")
            ?.click()
    }

    override fun closeFolderManager() {
        device.find("toolbar").children[0].click()
    }

    override fun openCreateFolderScreen() {
        device.find("add_menu_action_add").click()
    }

    override fun closeCreateFolderScreen() {
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up")).perform(ViewActions.click())
    }

    override fun enterNameForNewFolder(folderName: FolderName) {
        device.find("settings_folder_fragment_edit_text").text = folderName
    }

    override fun getCurrentNewFolderName(): FolderName {
        val folderName = device.find("settings_folder_fragment_edit_text").text
        if (folderName == "Name") {
            return ""
        } else
            return folderName
    }

    override fun getCurrentParentFolderForNewFolder(): String {
        return formatEmail(device.find("settings_folder_fragment_current_directory").text)
    }

    override fun submitNewFolder() {
        device.find("settings_folder_fragment_ok_button").click()
    }

    override fun openEditFolderScreen(folderName: FolderName, parentFolders: YSArray<FolderName>) {
        device.findByText(folderName).click()
    }

    override fun closeEditFolderScreen() {
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up")).perform(ViewActions.click())
    }

    override fun enterNameForEditedFolder(folderName: FolderName) {
        device.find("settings_folder_fragment_edit_text").text = folderName
    }

    override fun getCurrentEditedFolderName(): FolderName {
        return device.find("settings_folder_fragment_edit_text").text
    }

    override fun getCurrentParentFolderForEditedFolder(): String {
        return formatEmail(device.find("settings_folder_fragment_current_directory").text)
    }

    override fun submitEditedFolder() {
        device.find("settings_folder_fragment_ok_button").click()
    }

    override fun selectParentFolder(parentFolders: YSArray<FolderName>) {
        device.findByText(parentFolders[0]).click()
    }

    override fun openFolderLocationScreen() {
        device.find("settings_folder_fragment_choose_location").click()
    }

    override fun getFolderListForFolderLocationScreen(): YSArray<FolderName> {
        val folderList = device
            .findMany("settings_folder_list_item_text")
            .map { it.text }
            .toMutableList()
        folderList.add(
            device.find("settings_folder_chooser_fragment_mail_text").text.split('@')[0].replace(".", "-")
        )
        return folderList
    }

    override fun closeFolderLocationScreen() {
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up")).perform(ViewActions.click())
    }

    override fun deleteFolder(folderDisplayName: FolderName, parentFolders: YSArray<FolderName>, deletionMethod: ContainerDeletionMethod) {
        device.findByText(folderDisplayName).click()
        device.find("action_delete").click()
        device.findByText(getTextFromResources(R.string.folders_settings_delete_folder_button).toUpperCase()).click()
    }

    override fun getFolderListForManageFolderScreen(): YSArray<FolderName> {
        return device
            .findMany("settings_folder_list_item_text")
            .map { it.text }
            .toMutableList()
    }
}
