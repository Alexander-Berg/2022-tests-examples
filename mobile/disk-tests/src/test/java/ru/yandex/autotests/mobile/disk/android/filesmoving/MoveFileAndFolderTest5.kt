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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images
import ru.yandex.autotests.mobile.disk.data.ToastMessages
import java.util.concurrent.TimeUnit

@Feature("Move files and folders")
@UserTags("moving")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class MoveFileAndFolderTest5 : MoveFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1670")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeErrorToastWhenMoveFileToDeletedFolder() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldOpenCopyOrMoveDialogOnSelectedItems(FileActions.MOVE)
        onUserDiskApi.removeFiles(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldUnableToFileListStubBePresented()
        onFiles.shouldSeeToastWithMessage(ToastMessages.FOLDER_WAS_DELETED_TOAST)
        onFiles.pressButton(FileActions.MOVE)
        onFiles.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_MOVE_SOME_FILE_TOAST)
    }

    @Test
    @TmsLink("1643")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE],
        targetFolder = FilesAndFolders.ORIGINAL_FOLDER
    )
    @UploadFiles(
        filePaths = [FilesAndFolders.PHOTO],
        targetFolder = FilesAndFolders.ORIGINAL_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotOpenGroupModeByLongTapOnFileOnMoveDialog() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.shouldCopyOrMoveDialogBePresented(FileActions.MOVE)
        onFiles.longTapOnFile(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.longTapOnFile(FilesAndFolders.TARGET_FOLDER)
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.tapOnFile(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldFilesOrFoldersExist(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.TARGET_FOLDER
        )
        onFiles.shouldCopyOrMoveDialogBePresented(FileActions.MOVE)
        onFiles.tapOnFile(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
    }

    @Test
    @TmsLink("1649")
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(FullRegress::class)
    fun shouldKeepGoWhenCancelMovingCameraUploadsFolder() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CAMERA_UPLOADS)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.shouldMoveCameraUploadsAlertMessageBeDisplayed()
        onFiles.pressButton(FileActions.MOVE)
        onFiles.cancelCopyingOrMoving()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.CAMERA_UPLOADS)
        onBasePage.shouldBeOnGroupMode()
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.CAMERA_UPLOADS)
    }

    @Test
    @TmsLink("1647")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeErrorToastWhenMoveOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER)
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.wait(5, TimeUnit.SECONDS) //explicit wait for skipping 'Unable to update file list' toast
        onFiles.pressButton(FileActions.MOVE)
        onMobile.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_MOVE_SOME_FILE_TOAST)
        onFiles.shouldCopyOrMoveDialogBeNotPresented(FileActions.MOVE)
    }

    @Test
    @TmsLink("1650")
    @UploadFiles(filePaths = [FilesAndFolders.FIRST], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.CAMERA_UPLOADS])
    @Category(FullRegress::class)
    fun shouldSeeThumbForMovedFile() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldPreviewForFileHasSomeColorAs(FilesAndFolders.FIRST, Images.FIRST)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FIRST)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.pressButton(FileActions.MOVE)
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldPreviewForImageOnCameraUploadsBe(Images.FIRST)
        onFiles.shouldPreviewForFileHasSomeColorAs(FilesAndFolders.FIRST, Images.FIRST)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FIRST)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.pressButton(FileActions.MOVE)
        onFiles.navigateUp()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldPreviewForFileHasSomeColorAs(FilesAndFolders.FIRST, Images.FIRST)
    }

    @Test
    @TmsLink("1662")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldNotOpenNavigationDrawerBySwipeOnMoveDialog() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.shouldCopyOrMoveDialogBePresented(FileActions.MOVE)
        onFiles.swipeLeftToRight()
    }
}
