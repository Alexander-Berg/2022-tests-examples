package ru.yandex.autotests.mobile.disk.android.delete

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.*
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images
import ru.yandex.autotests.mobile.disk.data.MoveFilesToTrashSnackBarMessages
import java.util.concurrent.TimeUnit

@Feature("Delete files and folders")
@UserTags("deleting")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class DeleteFilesAndFoldersTest3 : DeleteFilesAndFoldersTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1519")
    @CleanTrash
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER])
    @Category(BusinessLogic::class)
    fun shouldDeleteSeveralFolders() {
        onBasePage.openFiles()
        onFiles.deleteFilesOrFolders(
            FilesAndFolders.CONTAINER_FOLDER,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER
        )
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FOLDERS)
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(
            FilesAndFolders.CONTAINER_FOLDER,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER
        )
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(
            FilesAndFolders.CONTAINER_FOLDER,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER
        )
    }

    @Test
    @TmsLink("1525")
    @CleanTrash
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @Category(BusinessLogic::class)
    fun shouldDeleteFilesFromViewerOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2)
        onBasePage.openOffline()
        onOffline.openFileIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.closePreview()
        onOffline.navigateUp()
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("1554")
    @CleanTrash
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE])
    @Category(BusinessLogic::class)
    fun shouldRestoreRemovedFileByOneOnDiskRoot() {
        onBasePage.openFiles()
        onFiles.deleteFilesOrFolders(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onFiles.wait(3, TimeUnit.SECONDS) //explicit wait for deleting file to trash
        onBasePage.openTrash()
        onTrash.shouldRestoreFilesFromTrash(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onTrash.navigateUp()
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1517")
    @CleanTrash
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE])
    @Category(BusinessLogic::class)
    fun shouldRemoveSeveralFilesAsGroupOnDiskRoot() {
        onBasePage.openFiles()
        onBasePage.enableGroupOperationMode()
        onFiles.deleteFilesOrFolders(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILES)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1555")
    @CleanTrash
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE])
    @Category(BusinessLogic::class)
    fun shouldRestoreRemovedFilesTogetherOnDiskRoot() {
        onBasePage.openFiles()
        onFiles.deleteFilesOrFolders(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onFiles.wait(5, TimeUnit.SECONDS)
        onBasePage.openTrash()
        onTrash.shouldRestoreFilesFromTrash(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onTrash.navigateUp()
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1533")
    @CleanTrash
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @Category(BusinessLogic::class)
    fun shouldDeleteFilesFromViewerOnDiskRoot() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.closePreview()
        onBasePage.swipeUpToDownNTimes(1)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("2417")
    @AuthorizationTest
    @CleanTrash
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @Category(BusinessLogic::class)
    fun shouldDeleteFilesFromViewerInFullAccessDir() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.closePreview()
        onFiles.navigateUp() //escape from FA dir
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("2418")
    @AuthorizationTest
    @CleanTrash
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE])
    @Category(BusinessLogic::class)
    fun shouldRestoreRemovedFilesTogetherInFullAccessDir() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.deleteFilesOrFolders(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from FA dir
        onBasePage.openTrash()
        onTrash.shouldRestoreFilesFromTrash(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onTrash.navigateUp()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("3071")
    @CleanTrash
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Regression::class)
    fun shouldSwitchToTrashOverSnackbarWhenRemoveFileOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILE)
        onFiles.wait(2, TimeUnit.SECONDS)
        onFiles.shouldOpenTrashFromSnackbar()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("3076")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Regression::class)
    fun shouldSwitchToTrashOverSnackbarWhenRemoveFileOnSearch() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onFiles.shouldSearchFile(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILE)
        onFiles.wait(2, TimeUnit.SECONDS)
        onFiles.shouldOpenTrashFromSnackbar()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("6260")
    @CleanTrash
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(Regression::class)
    fun shouldDeleteCameraUploadFolder() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CAMERA_UPLOADS)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE)
        onFiles.shouldDeleteCameraUploadsAlertMessageBeDisplayed()
        onFiles.approveDeletingSpecialFolder()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.CAMERA_UPLOADS)
    }

    @Test
    @TmsLink("1528")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @CleanTrash
    @Category(Regression::class)
    fun shouldNotDeleteFileOnAirplaneMode() {
        onBasePage.openFiles()
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILE)
        onMobile.switchToData()
        onBasePage.openTrash()
        onTrash.shouldFileBePresentedInTrashAfterLongWaiting(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1516")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO], targetFolder = FilesAndFolders.CAMERA_UPLOADS)
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @SharedFolder
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @CleanTrash
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.CAMERA_UPLOADS])
    @Category(BusinessLogic::class) //Acceptance
    fun shouldOpenTrashOverDeleteToTrashSnackbar() {
        onBasePage.openFiles()
        val folderName = nameHolder.name
        onFiles.switchToListLayout()
        onFiles.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILE)
        onFiles.shouldOpenTrashFromSnackbar()
        onFiles.wait(10, TimeUnit.SECONDS) //wait for deleting file on backend
        onFiles.updateFileList()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.pressHardBack() //escape from Trash
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.deleteFilesOrFolders(FilesAndFolders.PHOTO)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILE)
        onFiles.shouldOpenTrashFromSnackbar()
        onFiles.wait(1, TimeUnit.SECONDS)
        onFiles.updateFileList()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onBasePage.pressHardBack() //escape from Trash
        onBasePage.pressHardBack() //escape from previous opened folder (Camera Uploads)
        onFiles.openFolder(folderName)
        onFiles.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILE)
        onFiles.shouldOpenTrashFromSnackbar()
        onFiles.wait(1, TimeUnit.SECONDS)
        onFiles.updateFileList()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
    }
}
