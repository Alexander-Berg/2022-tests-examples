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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.PhotosSteps
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Rename files and folders")
@UserTags("renaming")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class RenameFileAndFolderTest2 : RenameFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    override lateinit var onAllPhotos: PhotosSteps

    @Test
    @TmsLink("1584")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.CYRILLIC_RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldSuccessfulRenameCyrillicFileAndFolder() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.CYRILLIC_RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.CYRILLIC_RENAMED_FOLDER)
        onFiles.openFolder(FilesAndFolders.CYRILLIC_RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.CYRILLIC_RENAMED_FILE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.CYRILLIC_RENAMED_FILE)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.CYRILLIC_RENAMED_FILE)
    }

    @Test
    @TmsLink("2319")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldSuccessfulRenameLatinFileAndFolder() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FOLDER)
        onFiles.openFolder(FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FILE)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.RENAMED_FILE)
    }

    @Test
    @TmsLink("1601")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(FullRegress::class)
    fun shouldWorkWithFileAfterRename() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FILE)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.RENAMED_FILE)
    }

    @Test
    @TmsLink("1602")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldWorkWithFolderAfterRename() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FOLDER)
        onFiles.openFolder(FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("2318")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.DIGIT_RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldSuccessfulRenameDigitFileAndFolder() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.DIGIT_RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.DIGIT_RENAMED_FOLDER)
        onFiles.openFolder(FilesAndFolders.DIGIT_RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.DIGIT_RENAMED_FILE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.DIGIT_RENAMED_FILE)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.DIGIT_RENAMED_FILE)
    }

    @Test
    @TmsLink("1632")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER
    )
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSuccessfulRenameSymbolFileAndFolder() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER) //switch to custom directory
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.SYMBOL_RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.SYMBOL_RENAMED_FOLDER)
        onFiles.openFolder(FilesAndFolders.SYMBOL_RENAMED_FOLDER)
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.SYMBOL_RENAMED_FILE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.SYMBOL_RENAMED_FILE)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.SYMBOL_RENAMED_FILE)
    }

    @Test
    @TmsLink("1582")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE_WITH_SLASH])
    @Category(FullRegress::class)
    fun shouldNotRenameFileToNameWithSlash() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotRenameFileOrFolderToIncorrectName(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.RENAMED_FILE_WITH_SLASH
        )
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1581")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER_WITH_SLAH])
    @Category(FullRegress::class)
    fun shouldNotRenameFolderToNameWithSlash() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotRenameFileOrFolderToIncorrectName(
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.RENAMED_FOLDER_WITH_SLAH
        )
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
    }
}
