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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Offline files")
@UserTags("offlineFiles")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OfflineFilesTest : OfflineFilesTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1048")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.BIG_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotDownLoadOfflineFileWhenMovedToSubfolderOfOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.BIG_FILE, FilesAndFolders.TARGET_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.BIG_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.shouldMoveOfflineAlertMessageBeDisplayed()
        onFiles.approveMovingOrRenamingOfflineFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.createFolderOnMoveDialog(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotSeeOfflineQueuedMarkForFile(FilesAndFolders.BIG_FILE)
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.BIG_FILE)
    }

    @Test
    @TmsLink("1049")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.CONTAINER_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.BIG_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotDownLoadOfflineFolderWhenMovedToSubfolderOfOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.navigateUp()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CONTAINER_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.shouldMoveOfflineAlertMessageBeDisplayed()
        onFiles.approveMovingOrRenamingOfflineFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.createFolderOnMoveDialog(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldNotSeeOfflineQueuedMarkForFile(FilesAndFolders.BIG_FILE)
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.BIG_FILE)
    }

    @Test
    @TmsLink("1046")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteOfflineFileWhenContainerFolderDeleted() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onFiles.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.PHOTO)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
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
    @TmsLink("3477")
    @UploadFiles(
        filePaths = [FilesAndFolders.PHOTO],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteOfflineFileWhenParentOfContainerFolderDeleted() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.navigateToRoot()
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onFiles.deleteFilesOrFolders(FilesAndFolders.CONTAINER_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.PHOTO)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.PHOTO,
            String.format(
                DeviceFilesAndFolders.DISK_CACHE,
                PackageResolver.resolveAppPackage()
            ) + FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
        )
    }

    @Test
    @TmsLink("1047")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteOfflineFileWhenContainerFolderDeletedOverWeb() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onDiskApi.removeFiles(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.PHOTO)
        onFiles.navigateUp() //escape from offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
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
    @TmsLink("3478")
    @UploadFiles(
        filePaths = [FilesAndFolders.PHOTO],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteOfflineFileWhenParentOfContainerFolderDeletedOverWeb() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.navigateToRoot()
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onDiskApi.removeFiles(FilesAndFolders.CONTAINER_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.PHOTO)
        onFiles.navigateUp() //escape from offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
        onAdb.shouldFileNotExistInFolderOnDevice(
            FilesAndFolders.PHOTO,
            String.format(
                DeviceFilesAndFolders.DISK_CACHE,
                PackageResolver.resolveAppPackage()
            ) + FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
        )
    }

    @Test
    @TmsLink("3489")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.PHOTO, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldRemoveFileFromOfflineWhenMovedToNonOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.waitDownloadComplete()
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onFiles.shouldMoveFileOrFolderAddedToOffline(FilesAndFolders.PHOTO, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.PHOTO)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.PHOTO,
            String.format(
                DeviceFilesAndFolders.DISK_CACHE,
                PackageResolver.resolveAppPackage()
            ) + FilesAndFolders.TARGET_FOLDER
        )
    }

    @Test
    @TmsLink("5611")
    @Category(Regression::class)
    fun shouldSearchBeNotDisplayedIfNoOfflineFiles() {
        onBasePage.openOffline()
        onFiles.shouldSearchBeNotDisplayed()
    }
}
