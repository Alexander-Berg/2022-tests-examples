package ru.yandex.autotests.mobile.disk.android.diskui

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.openqa.selenium.ScreenOrientation
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.CommonsSteps
import ru.yandex.autotests.mobile.disk.android.steps.FilesSteps
import ru.yandex.autotests.mobile.disk.android.steps.SettingsSteps
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Disk. UI")
@UserTags("diskUI")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class DiskUITest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onMobile: CommonsSteps

    @Test
    @TmsLink("1431")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldSeeErrorStubWhenLoadFolderContentOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onMobile.switchToAirplaneMode()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldUnableToFileListStubBePresented()
    }

    @Test
    @TmsLink("1434")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldSeeErrorToastWhenUpdateFolderContentOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onMobile.switchToAirplaneMode()
        onFiles.updateFileList()
        onMobile.shouldSeeToastWithMessage(ToastMessages.ERROR_WHILE_UPDATING_CATALOG_TOAST)
    }

    @Test
    @TmsLink("1794")
    @Category(Regression::class)
    fun shouldNotCloseClearOfflineDialogWhenRotateDevice() {
        onBasePage.openSettings()
        onSettings.openClearOfflineDialog()
        onSettings.shouldClearOfflineDialogBePresented()
        onSettings.rotate(ScreenOrientation.LANDSCAPE)
        onSettings.shouldClearOfflineDialogBePresented()
    }

    @Test
    @TmsLink("1795")
    @Category(Quarantine::class)
    fun shouldCloseModalDialogsOnSettingsPageAfterRotatingDevice() {
        onBasePage.openSettings()
        onSettings.openSyncOfflineSectionModalDialog()
        onSettings.shouldSyncModalDialogBeDisplayed()
        onSettings.rotate(ScreenOrientation.LANDSCAPE)
        onSettings.shouldSyncModalDialogBeNotDisplayed()
        onSettings.rotate(ScreenOrientation.PORTRAIT)
        onSettings.openUpdatingAllPhotosSectionModalDialog()
        onSettings.shouldSyncModalDialogBeDisplayed()
        onSettings.rotate(ScreenOrientation.LANDSCAPE)
        onSettings.shouldSyncModalDialogBeNotDisplayed()
    }

    @Test
    @TmsLink("3548")
    @Category(Regression::class)
    fun shouldCloseFABMenuByHardBack() {
        onBasePage.openFiles()
        onFiles.pressFAB()
        onFiles.shouldSeeFabMenu()
        onMobile.pressHardBack()
        onFiles.shouldNotSeeFabMenu()
    }
}
