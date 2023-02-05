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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadToSharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Move files and folders")
@UserTags("moving")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class MoveFileAndFolderTest3 : MoveFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1677")
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldFileBeMovedFromFullAccessDir() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldMoveOrCopyFilesToDiskRoot(FileActions.MOVE, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1679")
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldMoveFileIntoSubFolderFullAccessDir() {
        val sharedFolder = nameHolder.name
        onShareDiskApi.createFolders(sharedFolder + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.ORIGINAL_FILE
        )
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1678")
    @SharedFolder
    @SharedFolder
    @Category(FullRegress::class)
    fun shouldMoveFolderIntoNotMineFullAccessDir() {
        val folders = nameHolder.names
        val originalFolder = folders[0]
        val targetFolder = folders[1]
        onShareDiskApi.createFolders(originalFolder + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(originalFolder)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.MOVE)
        onFiles.navigateUp() //escape into disk root on Move dialog
        onFiles.openFolder(targetFolder)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp() //escape into disk root on file list
        onFiles.openFolder(targetFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1633")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldMoveActionBeDisabledForFilesInReadOnlyFolder() {
        val sharedFolder = nameHolder.name
        onShareDiskApi.createFolders(sharedFolder + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldSelectFilesOrFolders(
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.ORIGINAL_FILE
        ) //preload file
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.MOVE)
    }

    @Test
    @TmsLink("1648")
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeAlertWhenCameraUploadsFolderMoved() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CAMERA_UPLOADS)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.shouldDeleteCameraUploadsAlertMessageBeDisplayed()
        onFiles.approveMovingSpecialFolders()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
    }

    @Test
    @TmsLink("2414")
    @CreateFolders(folders = [FilesAndFolders.SOCIAL_NETWORKS, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.SOCIAL_NETWORKS, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeAlertWhenSocialNetworksFolderMoved() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.SOCIAL_NETWORKS)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.shouldMoveSocialNetworksAlertMessageBeDisplayed()
        onFiles.approveMovingSpecialFolders()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.SOCIAL_NETWORKS)
        //TODO: Add checking Social Networks dir icon when Image Matchers be implemented.
    }

    @Test
    @TmsLink("1681")
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.SOCIAL_NETWORKS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.SOCIAL_NETWORKS])
    @Category(FullRegress::class)
    fun shouldSeeAlertWhenSpecialFoldersBeMoved() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.SOCIAL_NETWORKS)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.shouldMoveSpecialFoldersAlertMessageBeDisplayed()
        onFiles.cancelMovingSpecialFolders()
        onBasePage.shouldBeOnGroupMode()
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.SOCIAL_NETWORKS)
    }
}
