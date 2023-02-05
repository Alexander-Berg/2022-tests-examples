package ru.yandex.autotests.mobile.disk.android.offlinefiles

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
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Offline files")
@UserTags("offlineFiles")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OfflineFilesTest3 : OfflineFilesTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2474")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @Category(Regression::class)
    fun shouldDisplayedOfflineMarkerOnOfflineFiles() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.ORIGINAL_TEXT_FILE)
    }

    @Test
    @TmsLink("995")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.BIG_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldNotDownloadOfflineFileWhenMovedToOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.BIG_FILE, FilesAndFolders.TARGET_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.BIG_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.approveMovingOrRenamingOfflineFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotSeeQueuedMark()
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.BIG_FILE)
    }

    @Test
    @TmsLink("999")
    @UploadFiles(
        filePaths = [FilesAndFolders.BIG_FILE, FilesAndFolders.FILE_FOR_VIEWING_1],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @Category(Regression::class)
    fun shouldNotDownloadOfflineFileWhenContainedFolderMovedToOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.swipeDownToUpNTimes(1)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.BIG_FILE)
        onFiles.waitDownloadComplete()
        onFiles.navigateUp()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CONTAINER_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.approveMovingOrRenamingOfflineFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldNotSeeOfflineQueuedMarkForFile(FilesAndFolders.BIG_FILE)
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.BIG_FILE)
    }

    @Test
    @TmsLink("1021")
    @UploadFiles(
        filePaths = [FilesAndFolders.BIG_FILE, FilesAndFolders.FILE_FOR_VIEWING_2, FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.FILE_FOR_SEARCH],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(Quarantine::class) // http://st/MOBDISKQA-3168
    fun shouldAddFilesToQueueWhenAddFileToOffline() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onBasePage.switchNetworkSpeedToEDGE()
        onFiles.addFilesOrFoldersToOffline(
            FilesAndFolders.BIG_FILE, FilesAndFolders.FILE_FOR_VIEWING_2, FilesAndFolders.FILE_FOR_VIEWING_1,
            FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.FILE_FOR_SEARCH
        )
        onFiles.shouldSeeOfflineQueuedMarkers(6)
        onFiles.shouldSeeUploadProgressBar()
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(
            FilesAndFolders.BIG_FILE, FilesAndFolders.FILE_FOR_VIEWING_2, FilesAndFolders.FILE_FOR_VIEWING_1,
            FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.FILE_FOR_SEARCH
        )
    }

    @Test
    @TmsLink("1004")
    @UploadFiles(
        filePaths = [FilesAndFolders.THIRD, FilesAndFolders.FOURTH],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @UploadFiles(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND],
        targetFolder = FilesAndFolders.ORIGINAL_FOLDER
    )
    @UploadFiles(filePaths = [FilesAndFolders.FIFTH])
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.FIFTH])
    @Category(Regression::class)
    fun shouldSeeOnlyOfflineFilesInViewer() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FIFTH)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.SECOND)
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.THIRD)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.openFileIntoViewer(FilesAndFolders.FIFTH)
        onPreview.shouldCurrentImageBe(Images.FIFTH)
        onPreview.swipeLeftToRight()
        onPreview.shouldCurrentImageBe(Images.FIFTH)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.THIRD)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.THIRD)
    }

    @Test
    @TmsLink("990")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Regression::class)
    fun shouldRemoveFileFromOfflineWhenAviaModeEnabled() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onBasePage.switchToAirplaneMode()
        onOffline.deleteFromOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("983")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @Category(FullRegress::class)
    fun shouldAddFileToOfflineAfterEnablingWiFi() {
        onBasePage.openFiles()
        onBasePage.switchToAirplaneMode()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onBasePage.shouldSeeToastWithMessage(ToastMessages.SAVING_TO_OFFLINE_WITHOUT_NETWORK_TOAST)
        onBasePage.switchToData()
        onFiles.waitDownloadComplete()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
    }

    @Test
    @TmsLink("987")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldSeeStubWhenRemoveLastFileOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.deleteFromOffline(FilesAndFolders.ORIGINAL_FILE)
        onOffline.shouldSeeOfflineStub()
        onOffline.navigateUp() //escape from Offline
        onBasePage.openSettings()
        onSettings.shouldCurrentOfflineCacheSizeBeZero()
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.openFileIntoViewer(FilesAndFolders.ORIGINAL_FILE)
        onPreview.deleteCurrentFileFromOffline()
        onOffline.shouldSeeOfflineStub()
        onOffline.navigateUp() //escape from Offline
        onBasePage.openSettings()
        onSettings.shouldCurrentOfflineCacheSizeBeZero()
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.deleteFromOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.shouldSeeOfflineStub()
        onOffline.navigateUp() //escape from Offline
        onBasePage.openSettings()
        onSettings.shouldCurrentOfflineCacheSizeBeZero()
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onDiskApi.removeFiles(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.shouldSeeOfflineStub()
        onOffline.navigateUp() //escape from Offline
        onBasePage.openSettings()
        onSettings.shouldCurrentOfflineCacheSizeBeZero()
    }
}
