package ru.yandex.autotests.mobile.disk.android.offlinefolders

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.Flaky
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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CleanTrash
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.SortFiles

@Feature("Offline folders")
@UserTags("offlineFolders")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OfflineFoldersTest4 : OfflineFoldersTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1073")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldUpdateOfflineFolderWhenUpdateOverWeb() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.navigateUp()
        onBasePage.openFiles()
        onUserDiskApi.removeFiles(FilesAndFolders.TARGET_FOLDER)
        onUserDiskApi.createFolders(FilesAndFolders.TARGET_FOLDER)
        onUserDiskApi.uploadFileToFolder(FilesAndFolders.FILE_FOR_GO, FilesAndFolders.TARGET_FOLDER)
        onUserDiskApi.uploadFileToFolder(FilesAndFolders.FILE_FOR_SEARCH, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH, FilesAndFolders.FILE_FOR_GO)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
    }

    @Test
    @TmsLink("1080")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotAddToOfflineFilesInOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO)
        onGroupMode.shouldMarkAsOfflineButtonBeDisabled()
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.ADD_TO_OFFLINE)
    }

    @Test
    @TmsLink("1081")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.FILE_FOR_GO],
        targetFolder = FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotAddToOfflineFoldersInOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.shouldMarkAsOfflineButtonBeDisabled()
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.ADD_TO_OFFLINE)
        onBasePage.pressHardBack()
        onBasePage.closeGroupMode()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.shouldMarkAsOfflineButtonBeDisabled()
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.ADD_TO_OFFLINE)
    }

    @Test
    @TmsLink("1085")
    @CreateFolders(folders = [SortFiles.BBBB, SortFiles.CC01, SortFiles.CC02])
    @DeleteFiles(files = [SortFiles.BBBB, SortFiles.CC01, SortFiles.CC02])
    @Category(FullRegress::class)
    fun shouldFolderWasSortedOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(SortFiles.CC01, SortFiles.CC02, SortFiles.BBBB)
        onBasePage.openOffline()
        onOffline.shouldFilesBeSortedInOrder(SortFiles.BBBB, SortFiles.CC01, SortFiles.CC02)
    }

    @Test
    @TmsLink("1086")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @CleanTrash
    @Category(FullRegress::class)
    fun shouldNotDeleteFolderFromOfflineWhenRestoreContainedFile() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.waitDownloadComplete()
        onFiles.deleteFilesOrFolders(FilesAndFolders.BIG_FILE)
        onFiles.navigateUp()
        onBasePage.openTrash()
        onTrash.shouldRestoreFilesFromTrash(FilesAndFolders.BIG_FILE)
        onTrash.navigateUp() //escape from Trash
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeeUploadProgressBar()
        onFiles.navigateUp()
        onFiles.shouldSeeOfflineFileMarker(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @Flaky
    @TmsLink("1092")
    @UploadFiles(
        filePaths = [SortFiles.TERMINATOR, SortFiles.AAAA, SortFiles.ABBB, SortFiles.ACCC],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + SortFiles.BBBB, FilesAndFolders.TARGET_FOLDER + "/" + SortFiles.CC01, FilesAndFolders.TARGET_FOLDER + "/" + SortFiles.CC02])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSwitchFileSorting() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onBasePage.openOffline()
        onOffline.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesBeSortedInOrder(
            SortFiles.TERMINATOR,
            SortFiles.BBBB,
            SortFiles.CC01,
            SortFiles.CC02,
            SortFiles.AAAA,
            SortFiles.ABBB,
            SortFiles.ACCC
        )
    }
}
