package ru.yandex.autotests.mobile.disk.android.filesrenaming

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.apache.commons.lang3.StringUtils
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

@Feature("Rename files and folders")
@UserTags("renaming")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class RenameFileAndFolderTest5 : RenameFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1594")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(FullRegress::class)
    fun shouldTrimSpacesOnBeginOfTargetFileName() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.RENAMED_FILE)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, StringUtils.SPACE + FilesAndFolders.RENAMED_FILE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FILE)
        onFiles.shouldNotExistFilesOrFolders(StringUtils.SPACE + FilesAndFolders.RENAMED_FILE)
    }

    @Test
    @TmsLink("1595")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldTrimSpacesOnBeginOfTargetFolderName() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.RENAMED_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, StringUtils.SPACE + FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(StringUtils.SPACE + FilesAndFolders.RENAMED_FOLDER)
    }

    @Test
    @TmsLink("1596")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldTrimSpacesOnEndOfTargetFolderName() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.RENAMED_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER + StringUtils.SPACE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.RENAMED_FOLDER + StringUtils.SPACE)
    }

    @Test
    @TmsLink("1597")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldTrimSpacesOnBeginAndEndOfTargetFolderName() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.RENAMED_FOLDER)
        onFiles.renameFileOrFolder(
            FilesAndFolders.ORIGINAL_FOLDER,
            StringUtils.SPACE + FilesAndFolders.RENAMED_FOLDER + StringUtils.SPACE
        )
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(StringUtils.SPACE + FilesAndFolders.RENAMED_FOLDER + StringUtils.SPACE)
    }

    @Test
    @TmsLink("1599")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldRenameButtonBeDisabledWhenTargetNameHasOnlySpace() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldFillNewFileName(FilesAndFolders.ORIGINAL_FOLDER, StringUtils.SPACE)
        onFiles.shouldRenameButtonBeDisabled()
    }

    @Test
    @TmsLink("1583")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.NAME_255_SYMBOLS])
    @Category(FullRegress::class)
    fun shouldRenameFolderToMaxLengthName() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.NAME_255_SYMBOLS)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.NAME_255_SYMBOLS)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.NAME_255_SYMBOLS)
    }
}
