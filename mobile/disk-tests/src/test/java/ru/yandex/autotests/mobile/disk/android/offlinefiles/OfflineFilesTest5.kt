package ru.yandex.autotests.mobile.disk.android.offlinefiles

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images

@Feature("Offline files")
@UserTags("offlineFiles")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OfflineFilesTest5 : OfflineFilesTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1000")
    @UploadFiles(
        filePaths = [FilesAndFolders.BIG_FILE, FilesAndFolders.BIG_IMAGE],
        targetFolder = FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldDownloadOfflineFileWhenContainerFolderMovedToOfflineFolderOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.BIG_FILE)
        onFiles.waitDownloadComplete()
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onBasePage.switchNetworkSpeedToEDGE()
        onDiskApi.moveFileToFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER, true)
        onFiles.updateFileList()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSeeOfflineQueuedMarkers(2)
    }

    @Test
    @TmsLink("1002")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteOfflineFileWhenContainerFolderMovedToNonOfflineFolderOverWeb() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onDiskApi.moveFileToFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER, true)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.PHOTO)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        onSettings.closeSettings()
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.PHOTO,
            String.format(
                DeviceFilesAndFolders.DISK_CACHE,
                PackageResolver.resolveAppPackage()
            ) + FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
        )
    }

    @Test
    @TmsLink("1003")
    @UploadFiles(
        filePaths = [FilesAndFolders.PHOTO],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteOfflineFileWhenParentOfContainerFolderMovedToNonOfflineFolder() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.navigateToRoot()
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onFiles.shouldMoveFileOrFolderAddedToOffline(FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.PHOTO)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.PHOTO,
            String.format(
                DeviceFilesAndFolders.DISK_CACHE,
                PackageResolver.resolveAppPackage()
            ) + FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
        )
    }

    @Test
    @TmsLink("1025")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldCleanOfflineOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        MatcherAssert.assertThat(offlineCacheSizeBeforeOperation, Matchers.not(Matchers.equalTo(0L)))
        onBasePage.switchToAirplaneMode()
        onSettings.clearOfflineFilesCompletely()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.equalTo(0L))
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openOffline()
        onOffline.shouldSeeOfflineStub()
        onBasePage.switchToData()
        onOffline.updateFileList()
        onOffline.shouldSeeOfflineStub()
    }

    @Test
    @TmsLink("1039")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.PHOTO, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldAddIdenticalFilesToOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onFiles.shouldFileListHasSize(2)
    }

    @Test
    @TmsLink("1041")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldNotDownloadFileWhenAddActuallyCachedFileToOffline() {
        onBasePage.openFiles()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onPreview.openEditor() //add file to cache
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.shouldNotSeeQueuedMark()
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.PHOTO)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
    }

    @Test
    @TmsLink("1042")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldDownloadFileWhenAddOutdatedCachedFileToOffline() {
        onBasePage.openFiles()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onPreview.openEditor() //add file to cache
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO) //avoid scrolling
        onDiskApi.updateFile(FilesAndFolders.PHOTO, FilesAndFolders.DISK_ROOT, FilesAndFolders.BIG_IMAGE)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.PHOTO)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onOffline.openFileIntoViewer(FilesAndFolders.PHOTO)
        onPreview.shouldCurrentImageBe(Images.SECOND)
    }
}
