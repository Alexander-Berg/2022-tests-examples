package ru.yandex.autotests.mobile.disk.android.offlinefolders

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Offline folders")
@UserTags("offlineFolders")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OfflineFoldersTest3 : OfflineFoldersTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1094")
    @UploadFiles(
        filePaths = [FilesAndFolders.BIG_FILE, FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(Regression::class)
    fun shouldNotDownloadOfflineFilesFromFolderWhenParentFolderBeAddedToOffline() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.navigateToRoot()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.swipeUpToDownNTimes(1)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotSeeQueuedMark()
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(
            FilesAndFolders.BIG_FILE,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.FILE_FOR_VIEWING_2,
            FilesAndFolders.FILE_FOR_VIEWING_1
        )
    }

    @Test
    @TmsLink("1107")
    @UploadFiles(
        filePaths = [FilesAndFolders.BIG_FILE, FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(Regression::class)
    fun shouldNotDownloadOfflineFilesWhenParentFolderBeRenamed() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.swipeUpToDownNTimes(1)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.navigateUp()
        onFiles.shouldRenameFileOrFolderAddedToOffline(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotSeeQueuedMark()
        onFiles.navigateToRoot()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(
            FilesAndFolders.BIG_FILE,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.FILE_FOR_VIEWING_2,
            FilesAndFolders.FILE_FOR_VIEWING_1
        )
    }

    @Test
    @TmsLink("1106")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.CONTAINER_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(Regression::class)
    fun shouldNotDownloadOfflineFileWhenRenamedIntoOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.renameFileOrFolder(FilesAndFolders.BIG_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.shouldNotSeeQueuedMark()
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FILE)
    }

    @Test
    @TmsLink("1074")
    @SharedFolder
    @Category(Quarantine::class)
    fun shouldDeleteSharedFoldersFromOfflineWhenFolderBecomeUnshared() {
        val name = nameHolder.name
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(name)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(name)
        onShareDiskApi.kickFromGroup(name, testAccount)
        onBasePage.openFiles()
        onFiles.updateFileList()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(name)
    }

    @Test
    @TmsLink("1075")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(Regression::class)
    fun shouldDeleteOfflineFolderWhenAviaModeEnabled() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onBasePage.switchToAirplaneMode()
        onOffline.deleteFromOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("3393")
    @DeleteFiles(files = [FilesAndFolders.TEST_FOLDER_NAME])
    @Category(FullRegress::class)
    fun shouldAddToOfflineANewlyCreatedFolder() {
        onBasePage.openFiles()
        onFiles.createFolder(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.shouldSeeOfflineFileMarker()
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TEST_FOLDER_NAME)
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.TEST_FOLDER_NAME)
    }

    @Test
    @TmsLink("1055")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldEmptyFolderToOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.TARGET_FOLDER)
    }
}
