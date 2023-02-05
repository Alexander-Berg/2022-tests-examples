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
import ru.yandex.autotests.mobile.disk.data.Images
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Move files and folders")
@UserTags("moving")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class MoveFileAndFolderTest6 : MoveFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1654")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotCloseMoveDialogWhenMovedtoSourceDirectory() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.shouldCopyOrMoveDialogBePresented(FileActions.MOVE)
        onFiles.pressButton(FileActions.MOVE)
        onMobile.shouldSeeToastWithMessage(ToastMessages.SELECT_DIFFERENT_FOLDER_TOAST)
        onFiles.shouldCopyOrMoveDialogBePresented(FileActions.MOVE)
    }

    @Test
    @TmsLink("1668")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeErrorToastWhenMoveADeletedFile() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onUserDiskApi.removeFiles(FilesAndFolders.ORIGINAL_FILE)
        onFiles.pressButton(FileActions.MOVE)
        onFiles.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_MOVE_SOME_FILE_TOAST)
    }

    @Test
    @TmsLink("1669")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeErrorToastWhenMoveADeletedFolder() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onUserDiskApi.removeFiles(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.pressButton(FileActions.MOVE)
        onFiles.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_MOVE_SOME_FILE_TOAST)
    }

    @Test
    @TmsLink("1635")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RW))
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFullAccessDir() {
        val name = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.MOVE, FilesAndFolders.TARGET_FOLDER, name)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(name)
    }

    @Test
    @TmsLink("1665")
    @SharedFolder
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldFileBeMovedToFullAccessDir() {
        val sharedFolderName = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.MOVE, sharedFolderName, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.openFolder(sharedFolderName)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1644")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotMoveFilesWhenMovingWasCanceled() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.MOVE)
        onFiles.cancelCopyingOrMoving()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.shouldBeOnGroupMode()
        onBasePage.closeGroupMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.cancelCopyingOrMoving()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.shouldBeOnGroupMode()
        onBasePage.closeGroupMode()
    }

    @Test
    @TmsLink("1655")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeErrorToastWhenMoveFileToFolderContainedFileWithSameName() {
        //change content of target file
        onUserDiskApi.updateFile(
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.FIFTH
        )
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FILE
        )
        onFiles.shouldSeeToastWithMessage(
            String.format(
                ToastMessages.MOVE_ERROR_ALREADY_EXIST_TEMPLATE,
                FilesAndFolders.ORIGINAL_FILE
            )
        )
        onFiles.shouldCopyOrMoveDialogBeNotPresented(FileActions.MOVE)
        onBasePage.shouldBeNotOnGroupMode()
        //Check checkbox not presented instead checkbox not selected because we have no checkbox at this time
        onGroupMode.shouldFileOrFolderHasNoCheckbox(FilesAndFolders.ORIGINAL_FILE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.updateFileList()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.ORIGINAL_FILE)
        onPreview.shouldCurrentImageBe(Images.FIFTH) // check file was not replaced
    }
}
