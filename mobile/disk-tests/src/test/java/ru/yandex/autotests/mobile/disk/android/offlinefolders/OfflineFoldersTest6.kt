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
class OfflineFoldersTest6 : OfflineFoldersTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1093")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotAddFolderToOfflineWhenAllFilesFromFolderAddedToOffline() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.addFilesOrFoldersToOffline(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.FILE_FOR_GO
        )
        onFiles.waitDownloadComplete()
        onFiles.navigateUp()
        onFiles.shouldNotSeeOfflineFileMarker(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.FILE_FOR_GO
        )
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("1097")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteSubfolderOfOfflineFolderFromOfflineWhenDeletedOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.CONTAINER_FOLDER)
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //exit to container folder
        onUserDiskApi.removeFiles(FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER)
        //check folder exist without updating file list
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.wait(30, TimeUnit.SECONDS)
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.wait(30, TimeUnit.SECONDS)
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.wait(30, TimeUnit.SECONDS)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        //check folder not exist after update
        onOffline.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onFiles.navigateToRoot() //escape to files
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
    }

    @Test
    @TmsLink("1098")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFolderFromOfflineWhenMovedOnClient() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onFiles.shouldMoveFileOrFolderAddedToOffline(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
    }

    @Test
    @TmsLink("1099")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFolderFromOfflineWhenMovedOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onUserDiskApi.moveFileToFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER, true)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
        val cachedFileFolder = String.format(
            DeviceFilesAndFolders.DISK_CACHE,
            PackageResolver.resolveAppPackage()
        ) + FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.ORIGINAL_FILE, cachedFileFolder)
    }

    @Test
    @TmsLink("1100")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFolderFromOfflineWhenContainerFolderMovedOnClient() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onFiles.shouldMoveFileOrFolderAddedToOffline(FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
        val cachedFileFolder = (String.format(
            DeviceFilesAndFolders.DISK_CACHE,
            PackageResolver.resolveAppPackage()
        ) + FilesAndFolders.TARGET_FOLDER + "/"
                + FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.ORIGINAL_FILE, cachedFileFolder)
    }

    @Test
    @TmsLink("1101")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldDeleteFolderFromOfflineWhenContainerFolderMovedOverWeb() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val offlineSizeBeforeOperation = onSettings.currentOfflineSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onUserDiskApi.moveFileToFolder(FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER, true)
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openSettings()
        val currentOfflineSize = onSettings.currentOfflineSize
        MatcherAssert.assertThat(currentOfflineSize, Matchers.lessThan(offlineSizeBeforeOperation))
        val cachedFileFolder = (String.format(
            DeviceFilesAndFolders.DISK_CACHE,
            PackageResolver.resolveAppPackage()
        ) + FilesAndFolders.TARGET_FOLDER + "/"
                + FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.ORIGINAL_FILE, cachedFileFolder)
    }

    @Test
    @TmsLink("1102")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldDownloadFolderMovedToOfflineFolderOnClient() {
        onBasePage.openFiles()
        onBasePage.switchNetworkSpeedToEDGE()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotSeeOfflineFileMarker(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER
        )
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSeeUploadProgressBar()
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.BIG_FILE)
    }
}
