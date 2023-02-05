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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.DeleteSharedFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Move files and folders")
@UserTags("moving")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class MoveFileAndFolderTest4 : MoveFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1666")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RW))
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotMoveReadOnlyDirToForeignFullAccessDir() {
        val targetFolder = nameHolder.name
        onUserDiskApi.shareFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onUserDiskApi.inviteUser(FilesAndFolders.ORIGINAL_FOLDER, sharedAccount, Rights.RO)
        onShareDiskApi.activateInvite() //activate all invites
        onBasePage.openFiles()
        onUserDiskApi.shouldFolderBeShared(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.MOVE, targetFolder, FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(targetFolder)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1674")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RW))
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotMoveFullAccessDirToForeignFullAccessDir() {
        val targetFolder = nameHolder.name
        onUserDiskApi.shareFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onUserDiskApi.inviteUser(FilesAndFolders.ORIGINAL_FOLDER, sharedAccount, Rights.RW)
        onShareDiskApi.activateInvite() //activate all invites
        onBasePage.openFiles()
        onUserDiskApi.shouldFolderBeShared(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.MOVE, targetFolder, FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(targetFolder)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1676")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RW))
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldSubDirOfFullAccessDirToForeignFullAccessDir() {
        val targetFolder = nameHolder.name
        onUserDiskApi.shareFolder(FilesAndFolders.CONTAINER_FOLDER)
        onUserDiskApi.inviteUser(FilesAndFolders.CONTAINER_FOLDER, sharedAccount, Rights.RW)
        onShareDiskApi.activateInvite() //activate all invites
        onUserDiskApi.shouldFolderBeShared(FilesAndFolders.CONTAINER_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.MOVE)
        onFiles.navigateUp()
        onFiles.openFolder(targetFolder)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onFiles.openFolder(targetFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1653")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveReadOnlyDir() {
        val readOnlyDir = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.MOVE, FilesAndFolders.TARGET_FOLDER, readOnlyDir)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(readOnlyDir)
        onUserDiskApi.shouldFolderBeShared(FilesAndFolders.TARGET_FOLDER + "/" + readOnlyDir)
    }

    @Test
    @TmsLink("1660")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteSharedFolders
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldNotMoveFullAccessDirToFullAccessDirTogetherWithSomeFile() {
        val foreignFolder = nameHolder.generateName().name
        onShareDiskApi.createFolders(foreignFolder)
        onShareDiskApi.shareFolder(foreignFolder)
        onShareDiskApi.inviteUser(foreignFolder, testAccount, Rights.RW)
        onUserDiskApi.activateInvite()
        onSettings.closeInvitationListIfDisplayed()
        val ownedFolder = nameHolder.generateName().name
        onUserDiskApi.createFolders(ownedFolder)
        onUserDiskApi.shareFolder(ownedFolder)
        onUserDiskApi.inviteUser(ownedFolder, sharedAccount, Rights.RW)
        onShareDiskApi.activateInvite()
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            foreignFolder,
            FilesAndFolders.ORIGINAL_FILE,
            ownedFolder
        )
        onFiles.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_MOVE_SOME_FILE_TOAST)
        onFiles.shouldFilesOrFoldersExist(ownedFolder)
        onFiles.openFolder(foreignFolder)
        onFiles.shouldNotExistFilesOrFolders(ownedFolder)
    }

    @Test
    @TmsLink("2442")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFileFromOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.shouldMoveFileOrFolderAddedToOffline(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openFiles()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("2443")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFolderFromOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldMoveFileOrFolderAddedToOffline(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFiles()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
    }
}
