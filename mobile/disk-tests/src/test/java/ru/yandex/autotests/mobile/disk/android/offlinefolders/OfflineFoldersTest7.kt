package ru.yandex.autotests.mobile.disk.android.offlinefolders

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.hamcrest.Matchers
import org.hamcrest.junit.MatcherAssert
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages
import java.util.concurrent.TimeUnit

@Feature("Offline folders")
@UserTags("offlineFolders")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OfflineFoldersTest7 : OfflineFoldersTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1103")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.CONTAINER_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldDownloadFolderMovedToOfflineFolderoverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotSeeOfflineFileMarker(FilesAndFolders.CONTAINER_FOLDER)
        onMobile.switchNetworkSpeedToEDGE()
        onUserDiskApi.moveFileToFolder(FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER, true)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldSeeUploadProgressBar()
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.BIG_FILE)
    }

    @Test
    @TmsLink("1104")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotDownloadFilesWhenOfflineFolderMovedToOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.navigateUp()
        onFiles.shouldMoveFileOrFolderAddedToOffline(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotSeeQueuedMark()
        onFiles.navigateUp()
        onFiles.shouldNotSeeOfflineFileMarker(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1095")
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldDownloadSubfolderOfOfflineFolderWhenUploadFiledOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.CONTAINER_FOLDER)
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onUserDiskApi.uploadFileToFolder(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER
        )
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER) //update file list
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateToRoot()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.greaterThan(offlineSizeBeforeOperation))
    }

    @Test
    @TmsLink("1057")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldAddFolderToQueueOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER) //caching file list
        onMobile.switchToAirplaneMode()
        onFiles.navigateUp()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onMobile.shouldSeeToastWithMessage(ToastMessages.SAVING_TO_OFFLINE_WITHOUT_NETWORK_TOAST)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeeOfflineQueuedMarkers(2)
        onFiles.wait(20, TimeUnit.SECONDS)
        onFiles.shouldSeeOfflineQueuedMarkers(2) // not remove files from queue on airplane mode
        onMobile.switchToData()
        onFiles.waitDownloadComplete()
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("1072")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteContainerFolderOfOfflineFolderOverWeb() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onUserDiskApi.removeFiles(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.CONTAINER_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
    }

    @Test
    @TmsLink("1054")
    @UploadFiles(
        filePaths = [FilesAndFolders.BIG_FILE, FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(Regression::class)
    fun shouldNotDownloadOfflineFilesWhenContainedFolderBeAddedToOffline() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.addFilesOrFoldersToOffline(
            FilesAndFolders.FILE_FOR_VIEWING_1,
            FilesAndFolders.FILE_FOR_VIEWING_2,
            FilesAndFolders.ORIGINAL_FILE
        )
        onFiles.waitDownloadComplete()
        onFiles.navigateUp()
        onMobile.switchNetworkSpeedToEDGE()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.swipeDownToUpNTimes(1) //
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldSeeOfflineQueuedMarkers(1)
        onFiles.shouldNotSeeOfflineQueuedMarkForFile(FilesAndFolders.FILE_FOR_VIEWING_1)
        onFiles.shouldNotSeeOfflineQueuedMarkForFile(FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.shouldNotSeeOfflineQueuedMarkForFile(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(
            FilesAndFolders.FILE_FOR_VIEWING_1,
            FilesAndFolders.FILE_FOR_VIEWING_2,
            FilesAndFolders.ORIGINAL_FILE
        )
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldFilesOrFoldersExist(
            FilesAndFolders.BIG_FILE,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.FILE_FOR_VIEWING_2,
            FilesAndFolders.FILE_FOR_VIEWING_1
        )
    }

    @Test
    @TmsLink("1052")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.CONTAINER_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.ORIGINAL_FILE])
    @Category(Regression::class)
    fun shouldAddFileFromSharedFolders() {
        onBasePage.openFiles()
        onFiles.shouldEnableGroupOperationMode()
        onFiles.shouldSelectFilesOrFolders(
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.CONTAINER_FOLDER,
            FilesAndFolders.ORIGINAL_FILE
        )
        onFiles.addSelectedFilesToOffline()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.CONTAINER_FOLDER,
            FilesAndFolders.ORIGINAL_FILE
        )
        onFiles.shouldSeeOfflineFileMarker(
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.CONTAINER_FOLDER,
            FilesAndFolders.ORIGINAL_FILE
        )
        onFiles.shouldNotSeeQueuedMark()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotSeeQueuedMark()
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldNotSeeQueuedMark()
    }
}
