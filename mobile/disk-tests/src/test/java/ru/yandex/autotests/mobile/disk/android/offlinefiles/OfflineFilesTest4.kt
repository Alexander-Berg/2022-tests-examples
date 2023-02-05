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

@Feature("Offline files")
@UserTags("offlineFiles")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OfflineFilesTest4 : OfflineFilesTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("993")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(FullRegress::class)
    fun shouldDeleteFileFromOfflineWhenFileRenamedOnFileList() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onFiles.shouldRenameFileOrFolderAddedToOffline(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
    }

    @Test
    @TmsLink("3540")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFileFromOfflineWhenContainerFolderRenamedOnFileList() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onFiles.shouldRenameFileOrFolderAddedToOffline(FilesAndFolders.TARGET_FOLDER, FilesAndFolders.RENAMED_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
    }

    @Test
    @TmsLink("3542")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFileFromOfflineWhenParentOfContainerFolderRenamedOnFileList() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from offline
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onFiles.shouldRenameFileOrFolderAddedToOffline(FilesAndFolders.TARGET_FOLDER, FilesAndFolders.RENAMED_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from offline
        onBasePage.openSettings()
        val currentOfflineCacheSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineCacheSize, Matchers.lessThan(offlineCacheSizeBeforeOperation))
    }

    @Test
    @TmsLink("994")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(FullRegress::class)
    fun shouldDeleteFileFromOfflineWhenRenamedOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        val cacheBeforeOperation = onSettings.currentCacheSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onDiskApi.rename(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE, true)
        onBasePage.openOffline()
        onOffline.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        val currentCacheSize = onSettings.currentCacheSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
        MatcherAssert.assertThat(currentCacheSize, Matchers.equalTo(cacheBeforeOperation))
    }

    @Test
    @TmsLink("3543")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFileFromOfflineWhenContainerFolderRenamedOverWeb() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from offline
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        val cacheBeforeOperation = onSettings.currentCacheSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openOffline()
        onDiskApi.rename(FilesAndFolders.TARGET_FOLDER, FilesAndFolders.RENAMED_FOLDER, true)
        onOffline.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        val currentCacheSize = onSettings.currentCacheSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
        MatcherAssert.assertThat(currentCacheSize, Matchers.equalTo(cacheBeforeOperation))
    }

    @Test
    @TmsLink("3544")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFileFromOfflineWhenParentOfContainerFolderRenamedOverWeb() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from Offline
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        val cacheBeforeOperation = onSettings.currentCacheSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openOffline()
        onDiskApi.rename(FilesAndFolders.TARGET_FOLDER, FilesAndFolders.RENAMED_FOLDER, true)
        onOffline.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        val currentCacheSize = onSettings.currentCacheSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
        MatcherAssert.assertThat(currentCacheSize, Matchers.equalTo(cacheBeforeOperation))
    }

    @Test
    @TmsLink("996")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.BIG_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveOfflineFileWhenMovedToOfflineFolderOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.BIG_FILE, FilesAndFolders.TARGET_FOLDER)
        onFiles.waitDownloadComplete()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.BIG_FILE, FilesAndFolders.TARGET_FOLDER)
        onOffline.openFolder(FilesAndFolders.TARGET_FOLDER)
        onOffline.switchNetworkSpeedToEDGE() //decrease network speed for handling offline progress bar
        onDiskApi.moveFileToFolder(FilesAndFolders.BIG_FILE, FilesAndFolders.TARGET_FOLDER, true)
        onOffline.updateFileList()
        onFiles.shouldSeeUploadProgressBar()
        onFiles.navigateUp()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.BIG_FILE)
    }

    @Test
    @TmsLink("998")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.PHOTO, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldRemoveFileFromOfflineWhenMovedToNonOfflineFolderOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.waitDownloadComplete()
        onBasePage.openSettings()
        val offlineCacheSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onDiskApi.moveFileToFolder(FilesAndFolders.PHOTO, FilesAndFolders.TARGET_FOLDER, true)
        onFiles.updateFileList()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onFiles.navigateUp()
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
            ) + FilesAndFolders.TARGET_FOLDER
        )
    }
}
