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
import ru.yandex.autotests.mobile.disk.android.core.driver.PackageResolver
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Offline folders")
@UserTags("offlineFolders")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OfflineFoldersTest : OfflineFoldersTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1050")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(BusinessLogic::class)
    fun shouldAddFolderToOfflineOverLogTap() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.addSelectedFilesToOffline()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotSeeOfflineQueuedMarkForFile(FilesAndFolders.PHOTO)
        onFiles.navigateUp()
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onFiles.navigateUp()
        onFiles.navigateUp()
        onBasePage.openSettings()
        onBasePage.wait(20, TimeUnit.SECONDS)
        val current = onSettings.currentOfflineSize
        MatcherAssert.assertThat("expect not empty cache", current, Matchers.greaterThan(0L))
    }

    @Test
    @TmsLink("1105")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldDownloadFileContainedInOfflineFolderWhenRenamed() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.waitDownloadComplete()
        onMobile.switchNetworkSpeedToEDGE()
        onUserDiskApi.rename(
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.BIG_FILE,
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.RENAMED_FILE,
            true
        )
        onFiles.updateFileList()
        onFiles.shouldSeeOfflineQueuedMarkers(1)
        onFiles.shouldSeeUploadProgressBar()
    }

    @Test
    @TmsLink("1110")
    @UploadFiles(
        filePaths = [FilesAndFolders.PHOTO],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFolderFromOfflineWhenContainerFolderRenamedOverWeb() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onUserDiskApi.rename(FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.RENAMED_FOLDER, true)
        onFiles.updateFileList()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.PHOTO,
            String.format(
                DeviceFilesAndFolders.DISK_CACHE,
                PackageResolver.resolveAppPackage()
            ) + FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER
        )
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.PHOTO,
            String.format(
                DeviceFilesAndFolders.DISK_CACHE,
                PackageResolver.resolveAppPackage()
            ) + FilesAndFolders.RENAMED_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER
        )
    }

    @Test
    @TmsLink("1112")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO], targetFolder = FilesAndFolders.CONTAINER_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFolderFromOfflineWhenRenamedOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER)
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onUserDiskApi.rename(FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.RENAMED_FOLDER, true)
        onFiles.updateFileList()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.PHOTO,
            String.format(
                DeviceFilesAndFolders.DISK_CACHE,
                PackageResolver.resolveAppPackage()
            ) + FilesAndFolders.CONTAINER_FOLDER
        )
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.PHOTO,
            String.format(
                DeviceFilesAndFolders.DISK_CACHE,
                PackageResolver.resolveAppPackage()
            ) + FilesAndFolders.RENAMED_FOLDER
        )
    }

    @Test
    @TmsLink("1113")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotDownloadFileWhenMovedFromOfflineFolderToOfflineFolderOnClient() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FILE_FOR_VIEWING_1)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.approveMovingOrRenamingOfflineFiles()
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.pressButton(FileActions.MOVE)
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldNotSeeQueuedMark()
    }

    @Test
    @TmsLink("1114")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotDownloadFileWhenMovedFromOfflineFolderToOfflineFolderOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onUserDiskApi.moveFileToFolder(
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.BIG_FILE,
            FilesAndFolders.CONTAINER_FOLDER,
            true
        )
        onBasePage.switchNetworkSpeedToEDGE()
        onFiles.updateFileList()
        onFiles.shouldSeeUploadProgressBar()
    }

    @Test
    @TmsLink("1115")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldRemoveFromOfflineFileContainedInOfflineFolderWhenMovedToNonOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.navigateUp()
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.BIG_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.approveMovingOrRenamingOfflineFiles()
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.pressButton(FileActions.MOVE)
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotSeeQueuedMark()
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
    }

    @Test
    @TmsLink("1116")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldRemoveFromOfflineFileContainedInOfflineFolderWhenMovedToNonOfflineFolderOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.navigateUp()
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onUserDiskApi.moveFileToFolder(
            FilesAndFolders.ORIGINAL_FOLDER + "/" + FilesAndFolders.BIG_FILE,
            FilesAndFolders.TARGET_FOLDER,
            true
        )
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotSeeQueuedMark()
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
    }

    @Test
    @TmsLink("1082")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldCleanOfflineFullyOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onBasePage.switchToAirplaneMode()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        onSettings.clearOfflineFilesCompletely()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.equalTo(0L))
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_FILE,
            String.format(DeviceFilesAndFolders.DISK_CACHE, PackageResolver.resolveAppPackage())
        )
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_FOLDER,
            String.format(DeviceFilesAndFolders.DISK_CACHE, PackageResolver.resolveAppPackage())
        )
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openOffline()
        onOffline.shouldSeeOfflineStub()
    }
}
