package ru.yandex.autotests.mobile.disk.android.filesrenaming

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages
import java.util.concurrent.TimeUnit

@Feature("Rename files and folders")
@UserTags("renaming")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class RenameFileAndFolderTest : RenameFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1590")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(Regression::class)
    fun shouldSuccessfullyRenameFile() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldRenameDialogBeDisplayed()
        onBasePage.shouldKeyboardShown()
        onFiles.shouldFillNewFileNameFieldAndRenameFile(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FILE)
    }

    @Test
    @TmsLink("1589")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(Regression::class)
    fun shouldSuccessfullyRenameNonEmptyFolder() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldRenameDialogBeDisplayed()
        onBasePage.shouldKeyboardShown()
        onFiles.shouldFillNewFileNameFieldAndRenameFile(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FOLDER)
        onFiles.wait(3, TimeUnit.SECONDS) //wait for unlocking folder on backend
    }

    @Test
    @TmsLink("1585")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_SEARCH])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_SEARCH, FilesAndFolders.FILE_FOR_SEARCH_PNG])
    @Category(FullRegress::class)
    fun shouldAllowChangeFileExtension() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH)
        onFiles.renameFileOrFolder(FilesAndFolders.FILE_FOR_SEARCH, FilesAndFolders.FILE_FOR_SEARCH_PNG)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_SEARCH_PNG)
    }

    @Test
    @TmsLink("1591")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(FullRegress::class)
    fun shouldNotRenameFileOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.switchToAirplaneMode()
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.shouldSeeToastWithMessage(
            String.format(
                ToastMessages.ERROR_OCCURED_WHEN_RENAME_TEMPLATE,
                FilesAndFolders.ORIGINAL_FILE
            )
        )
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.RENAMED_FILE)
        onMobDiskApi.shouldFileExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1593")
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(FullRegress::class)
    fun shouldNotRenameCameraUploadsFolderWhenCanceled() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CAMERA_UPLOADS)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldRenameCameraUploadsAlertMessageBeDisplayed()
        onFiles.approveRenamingSpecialFolder()
        onFiles.cancelRenaming()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.CAMERA_UPLOADS)
    }

    @Test
    @TmsLink("1605")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(FullRegress::class)
    fun shouldUpdateFileNameWhenFileRenamedOverWeb() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.RENAMED_FILE)
        onMobDiskApi.rename(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE, true)
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FILE)
    }

    @Test
    @TmsLink("1604")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldUpdateFolderNameWhenFolderRenamedOverWeb() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.RENAMED_FOLDER)
        onMobDiskApi.rename(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER, true)
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FOLDER)
    }

    @Test
    @TmsLink("1607")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(FullRegress::class)
    fun shouldSeeErrorOccuredToastWhenRenameAlreadyDeletedFile() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE) //scroll to file
        onMobDiskApi.removeFiles(FilesAndFolders.ORIGINAL_FILE)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.shouldSeeToastWithMessage(
            String.format(
                ToastMessages.ERROR_OCCURED_WHEN_RENAME_TEMPLATE,
                FilesAndFolders.ORIGINAL_FILE
            )
        )
    }

    @Test
    @TmsLink("1608")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeErrorOccuredToastWhenRenameAlreadyDeletedFolder() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER) //scroll to folder
        onMobDiskApi.removeFiles(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldSeeToastWithMessage(
            String.format(
                ToastMessages.ERROR_OCCURED_WHEN_RENAME_TEMPLATE,
                FilesAndFolders.ORIGINAL_FOLDER
            )
        )
    }

    @Test
    @TmsLink("2320")
    @CreateFolders(folders = [FilesAndFolders.SYMBOL_FOLDER, FilesAndFolders.SYMBOL_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.SYMBOL_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.SYMBOL_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.SYMBOL_FOLDER])
    @Category(FullRegress::class)
    fun shouldRenameFileOrFolderOnSymbolFolder() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.SYMBOL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FOLDER, FilesAndFolders.RENAMED_FILE)
        onFiles.openFolder(FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onFiles.navigateUp()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.RENAMED_FILE)
    }

    @Test
    @TmsLink("6259")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.RENAMED_FOLDER])
    fun shouldRenameCameraFolder() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CAMERA_UPLOADS)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldRenameCameraUploadsAlertMessageBeDisplayed()
        onFiles.approveRenamingSpecialFolder()
        onFiles.shouldFillNewFileNameFieldAndRenameFile(FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FOLDER)
    }
}
