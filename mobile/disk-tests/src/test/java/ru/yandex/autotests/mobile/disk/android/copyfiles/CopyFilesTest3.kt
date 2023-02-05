package ru.yandex.autotests.mobile.disk.android.copyfiles

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Copy files and folders")
@UserTags("copyFiles")
@RunWith(GuiceTestRunner::class)
@GuiceModules(
    AndroidModule::class
)
class CopyFilesTest3 : CopyFilesTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2358")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotCopyFolderOnAirplaneMode() {
        onBasePage.openFiles()
        onBasePage.switchToAirplaneMode()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.COPY,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER
        )
        onFiles.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_COPY_CERTAIN_FILES_TOAST)
    }

    @Test
    @TmsLink("2359")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotCopyFolderToSourceFolder() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.COPY)
        onGroupMode.pressButton(FileActions.COPY)
        onGroupMode.shouldSeeToastWithMessage(ToastMessages.SELECT_A_FOLDER_DIFFERENT_FROM_THE_SOURCE_FOLDER_TOAST)
        onFiles.shouldCopyOrMoveDialogBePresented(FileActions.COPY)
    }

    @Test
    @TmsLink("2368")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.PHOTO, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNCopyFileToFolderWhereExistFileWithSameNameButOtherCaseInName() {
        onDiskApi.rename(
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.PHOTO,
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.PHOTO_BIG_CASE_NAME,
            true
        )
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.COPY, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.PHOTO)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO, FilesAndFolders.PHOTO_BIG_CASE_NAME)
    }

    @Test
    @TmsLink("2363")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldOpenCopyDialogForSeveralFilesAndFolders() {
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.COPY,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.ORIGINAL_TEXT_FILE
        )
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.TARGET_FOLDER
        )
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_TEXT_FILE)
    }

    @Test
    @TmsLink("2364")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldCopyFileToDirectoryFromSearch() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.COPY)
        onFiles.shouldOpenFolderFromSearchOnCopyDialog(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.COPY)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
    }
}
