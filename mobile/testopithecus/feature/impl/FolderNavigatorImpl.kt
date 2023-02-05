package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.formatIfTabName
import com.yandex.mail.testopithecus.pages.FolderListPage
import com.yandex.mail.testopithecus.steps.clickOnRecyclerItemByTextInUiObject2
import com.yandex.mail.testopithecus.steps.closeDrawer
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.mail.testopithecus.steps.gone
import com.yandex.mail.testopithecus.steps.openDrawer
import com.yandex.mail.testopithecus.steps.scrollDrawerToTop
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.YSMap
import com.yandex.xplat.common.stringToInt32
import com.yandex.xplat.testopithecus.DefaultFolderName
import com.yandex.xplat.testopithecus.FolderName
import com.yandex.xplat.testopithecus.FolderNavigator
import io.qameta.allure.kotlin.Allure

class FolderNavigatorImpl(private val device: UiDevice) : FolderNavigator {
    private val FIRST_SYSTEM_LABEL = FolderListPage.IMPORTANT
    private val TITLE_FOLDER_LIST = "FOLDERS"

    override fun openFolderList() {
        openDrawer()
    }

    override fun closeFolderList() {
        closeDrawer()
    }

    override fun getFoldersList(): YSMap<FolderName, Int> {
        device.scrollDrawerToTop()
        var folderList: YSMap<FolderName, Int> = mutableMapOf()

        val folders = device.findMany("folder_list_item_container_info")

        for (folder in folders) {
            val folderName = folder.find("folder_list_item_text")!!.text
            if (folderName == FIRST_SYSTEM_LABEL) {
                break
            }
            var counter = 0
            var counterText = "0"
            if (folder.find("folder_list_item_counter") != null) {
                counterText = folder.find("folder_list_item_counter")!!.text
            }
            if (counterText != "") {
                counter = stringToInt32(counterText)!!
            }
            folderList[folderName] = counter
        }

        folderList.remove(TITLE_FOLDER_LIST) // delete element FOLDERS in the list (title of list folders)
        if (this.isInTabsMode()) {
            // for asserts
            folderList[DefaultFolderName.mailingLists] = folderList.getValue(FolderListPage.SUBSCRIPTIONS)
            folderList[DefaultFolderName.socialNetworks] = folderList.getValue(FolderListPage.SOCIALMEDIA)

            folderList.remove(FolderListPage.SOCIALMEDIA)
            folderList.remove(FolderListPage.SUBSCRIPTIONS)
            folderList.remove(FolderListPage.WITH_ATTACHMENTS_LABEL)
        }
        // TODO: implement unified inbox in model and delete this
        folderList.remove("Unified inbox")

        folderList = folderList.filter { (folderName, _) -> !folderName.startsWith("yandex-team-") } as YSMap<FolderName, Int>

        return folderList
    }

    override fun goToFolder(folderDisplayName: String, parentFolders: YSArray<FolderName>) {
        Allure.step("Переходим в папку $folderDisplayName") {
            val folderName = formatIfTabName(folderDisplayName)
            device.gone("snackbar_text", 4000)
            device.scrollDrawerToTop()
            device.clickOnRecyclerItemByTextInUiObject2("folder_list", folderName)
        }
    }

    override fun isInTabsMode(): Boolean {
        return Allure.step("Проверка, активная ли настройка табов") {
            val inboxFolder = device.find("folder_list_item_text", 0).text
            val subscriptionsFolder = device.find("folder_list_item_text", 1).text
            val socialMediaFolder = device.find("folder_list_item_text", 2).text
            val withAttachmentsLabel = device.find("folder_list_item_text", 3).text

            return@step inboxFolder == FolderListPage.INBOX &&
                subscriptionsFolder == FolderListPage.SUBSCRIPTIONS &&
                socialMediaFolder == FolderListPage.SOCIALMEDIA &&
                withAttachmentsLabel == FolderListPage.WITH_ATTACHMENTS_LABEL
        }
    }

    override fun ptrFoldersList() {
        Allure.step("Выполняем PTR в списке папок") {
            device.scrollDrawerToTop()
            device.swipe(
                (device.displayWidth * .2).toInt(),
                (device.displayHeight * .2).toInt(),
                (device.displayWidth * .2).toInt(),
                device.displayHeight,
                50
            )
        }
    }

    override fun getCurrentContainer(): String? {
        return Allure.step("Получаем текущую папку/метку/фильтр") {
            val selectedContainer = device.findMany("folder_list_item_text").firstOrNull { it.isSelected }
            if (selectedContainer != null) {
                return@step selectedContainer.text
            }
            return@step null
        }
    }
}
