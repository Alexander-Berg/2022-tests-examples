package ru.yandex.autotests.mobile.disk.android.filesmoving

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileWithName
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Move files and folders")
@UserTags("moving")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class MoveFileAndFolderTest2 : MoveFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1636")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeveralFoldersBeMovedToFolder() {
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.CONTAINER_FOLDER
        )
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.CONTAINER_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.CONTAINER_FOLDER)
    }

    @Test
    @TmsLink("1637")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeveralFilesBeMovedToFolder() {
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE
        )
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
    }

    @Test
    @TmsLink("1659")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFilesAndFolders() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1661")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFileToFolderFromSearchDialog() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.MOVE)
        onSearch.shouldOpenFileOrFolderFromSearchOnMoveDialog(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1682")
    @UploadFileWithName(filePath = FilesAndFolders.ORIGINAL_FILE, name = FilesAndFolders.SYMBOL_ORIGINAL_FILE)
    @CreateFolders(folders = [FilesAndFolders.SYMBOL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.SYMBOL_ORIGINAL_FILE, FilesAndFolders.SYMBOL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFilesAndFoldersWithSpecialSymbols() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.SYMBOL_FOLDER, FilesAndFolders.SYMBOL_ORIGINAL_FILE)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.SYMBOL_ORIGINAL_FILE, FilesAndFolders.SYMBOL_FOLDER)
    }

    @Test
    @TmsLink("1683")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.SYMBOL_TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.SYMBOL_TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFilesAndFoldersToFolderWithSpecialSymbols() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.SYMBOL_TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.SYMBOL_TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1664")
    @SharedFolder
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFolderIntoFullAccessDir() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.MOVE, sharedFolder, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(sharedFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
    }
}
