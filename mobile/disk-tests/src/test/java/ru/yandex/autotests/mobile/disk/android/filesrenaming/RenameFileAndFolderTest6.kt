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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadToSharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Rename files and folders")
@UserTags("renaming")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class RenameFileAndFolderTest6 : RenameFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1610")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE])
    @Category(FullRegress::class)
    fun shouldRenameActionBeDisableWhenSelectedMoreThanOneObject() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.RENAME)
    }

    @Test
    @TmsLink("1615")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(FullRegress::class)
    fun shouldNotRenameFileWhenCancelRenamingToNonEmptyName() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldFillNewFileName(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.cancelRenaming()
        onFiles.shouldNotSeeRenameDialog()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.RENAMED_FILE)
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.shouldBeOnGroupMode()
    }

    @Test
    @TmsLink("1614")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotRenameFolderWhenCancelRenamingToNonEmptyName() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldFillNewFileName(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER)
        onFiles.cancelRenaming()
        onFiles.shouldNotSeeRenameDialog()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.RENAMED_FOLDER)
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.shouldBeOnGroupMode()
    }

    @Test
    @TmsLink("1611")
    @CreateFolders(folders = [FilesAndFolders.SOCIAL_NETWORKS])
    @DeleteFiles(files = [FilesAndFolders.SOCIAL_NETWORKS, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeAlertWhenSocialNetworksFolderBeRenamed() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.SOCIAL_NETWORKS)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.SOCIAL_NETWORKS)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldRenameSocialNetworksAlertMessageBeDisplayed()
    }

    @Test
    @TmsLink("1629")
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldRenameFileIntoFullAccessDir() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FILE)
        //TODO: Add checking of file will be renamed for all users.
    }

    @Test
    @TmsLink("2746")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER])
    @Category(FullRegress::class)
    fun shouldSuccessfullyRenameEmptyFolder() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.RENAMED_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.RENAMED_FOLDER)
    }
}
