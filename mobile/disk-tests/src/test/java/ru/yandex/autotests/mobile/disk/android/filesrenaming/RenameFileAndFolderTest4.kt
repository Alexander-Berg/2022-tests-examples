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
import ru.yandex.autotests.mobile.disk.android.core.api.data.shared.Rights
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Rename files and folders")
@UserTags("renaming")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class RenameFileAndFolderTest4 : RenameFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1627")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @Category(FullRegress::class)
    fun shouldNotRenameSubDirOfReadOnlyDir() {
        val folderName = nameHolder.name
        onShareDiskApi.createFolders(folderName + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(folderName)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.RENAME)
    }

    @Test
    @TmsLink("1626")
    @SharedFolder
    @Category(FullRegress::class)
    fun shouldRenameSubDirOfFullAccessDir() {
        val folderName = nameHolder.name
        onShareDiskApi.createFolders(folderName + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(folderName)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.TARGET_FOLDER)
        onShareDiskApi.shouldFolderExist(folderName + "/" + FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("1617")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldNotRenameFileWhenCancelRenamingToEmptyName() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldFillNewFileName(FilesAndFolders.ORIGINAL_FILE, StringUtils.EMPTY)
        onFiles.cancelRenaming()
        onFiles.shouldNotSeeRenameDialog()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.shouldBeOnGroupMode()
    }

    @Test
    @TmsLink("1618")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotRenameFolderToExistedFolder() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeeToastWithMessage(
            String.format(
                ToastMessages.OBJECT_ALREADY_EXIST_TEMPLATE,
                FilesAndFolders.TARGET_FOLDER
            )
        )
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldOnlyOneObjectWithSpecificNameExist(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("1619")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotRenameFileToExistedFile() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.renameFileOrFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.shouldSeeToastWithMessage(
            String.format(
                ToastMessages.OBJECT_ALREADY_EXIST_TEMPLATE,
                FilesAndFolders.ORIGINAL_TEXT_FILE
            )
        )
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.shouldOnlyOneObjectWithSpecificNameExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
    }

    @Test
    @TmsLink("2317")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotRenameFolderToEmptyName() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotRenameFileOrFolderToIncorrectName(FilesAndFolders.ORIGINAL_FOLDER, StringUtils.EMPTY)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1616")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotRenameFolderWhenCancelRenamingToEmptyName() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.RENAME)
        onFiles.shouldFillNewFileName(FilesAndFolders.ORIGINAL_FOLDER, StringUtils.EMPTY)
        onFiles.cancelRenaming()
        onFiles.shouldNotSeeRenameDialog()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.shouldBeOnGroupMode()
    }
}
