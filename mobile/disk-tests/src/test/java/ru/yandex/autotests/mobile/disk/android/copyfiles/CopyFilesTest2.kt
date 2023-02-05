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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadToSharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
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
class CopyFilesTest2 : CopyFilesTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2354")
    @SharedFolder
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldCopySharedFolder() {
        val shareFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(shareFolder, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(shareFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE) //check current folder content
        onFiles.navigateUp()
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.COPY, FilesAndFolders.TARGET_FOLDER, shareFolder)
        onFiles.shouldFilesOrFoldersExist(shareFolder, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(shareFolder) //TODO: Add checking folder icon
        onFiles.openFolder(shareFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
    }

    @Test
    @TmsLink("2355")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @SharedFolder
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldCopyFolderIntoSharedFolder() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(sharedFolder, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.COPY, sharedFolder, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(sharedFolder, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(sharedFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("2356")
    @SharedFolder
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldCopyFileIntoSharedFolder() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(sharedFolder, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.COPY, sharedFolder, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldFilesOrFoldersExist(sharedFolder, FilesAndFolders.ORIGINAL_FILE)
        onFiles.openFolder(sharedFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("2361")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotCopyFolderIfFolderWithGivenNameExists() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.COPY)
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("2360")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldNotCopyFileIfFileWithGivenNameExists() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.pressButton(FileActions.COPY)
        onFiles.shouldSeeToastWithMessage(
            String.format(
                ToastMessages.COPY_ERROR_OBJECT_ALREADY_EXISTS_TEMPLATE,
                FilesAndFolders.ORIGINAL_FILE
            )
        )
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFileListHasSize(1) //no duplicate
    }

    @Test
    @TmsLink("2357")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotCopyFilesWhenCopyingWasCanceled() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.COPY)
        onFiles.cancelCopyingOrMoving()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.closeGroupMode()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_TEXT_FILE)
    }
}
