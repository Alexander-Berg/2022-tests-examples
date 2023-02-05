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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages
import java.util.*
import java.util.concurrent.TimeUnit

@Feature("Move files and folders")
@UserTags("moving")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class MoveFileAndFolderTest : MoveFileAndFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1646")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldFileBeMovedToFolder() {
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FILE
        )
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1645")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldFolderBeMovedToFolder() {
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER
        )
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1656")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeErrorToastWhenMoveFolderToFolderContainedFolderWithSameName() {
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER
        )
        onMobile.shouldSeeToastWithMessage(
            String.format(
                ToastMessages.MOVE_ERROR_ALREADY_EXIST_TEMPLATE,
                FilesAndFolders.ORIGINAL_FOLDER
            )
        )
        onBasePage.shouldBeNotOnGroupMode()
        //Check checkbox not presented instead checkbox not selected because we have no checkbox at this time
        onGroupMode.shouldFileOrFolderHasNoCheckbox(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE) // check folder was not replaced
    }

    @Test
    @TmsLink("1657")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFolderToFolderContainedFolderWithSameUpperCaseName() {
        val folderUpperCase = FilesAndFolders.ORIGINAL_FOLDER.uppercase(Locale.getDefault())
        onUserDiskApi.createFolders(FilesAndFolders.TARGET_FOLDER + "/" + folderUpperCase)
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER
        )
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER, folderUpperCase)
    }

    @Test
    @TmsLink("1671")
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE],
        targetFolder = FilesAndFolders.ORIGINAL_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.ORIGINAL_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldDirectoryOnMoveObjectWindowBeCurrentDirectory() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.shouldCopyOrMoveDialogBePresented(FileActions.MOVE)
        //Should folder on move dialog be current folder
        onFiles.shouldFilesOrFoldersExist(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.TARGET_FOLDER
        )
    }

    @Test
    @TmsLink("1672")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFileToFolderContainedFileWithSameUpperCaseName() {
        val fileUpperCase = FilesAndFolders.ORIGINAL_FILE.uppercase(Locale.getDefault())
        onUserDiskApi.rename(
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.TARGET_FOLDER + "/" + fileUpperCase,
            true
        )
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.MOVE,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FILE
        )
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, fileUpperCase)
    }

    @Test
    @TmsLink("1673")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.PHOTO, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFileToFolderContainedFileWithSameNameButOtherCaseInExtension() {
        onUserDiskApi.rename(
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.PHOTO,
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.PHOTO_BIG_CASE_EXTENSION,
            true
        )
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.MOVE, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.PHOTO)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO, FilesAndFolders.PHOTO_BIG_CASE_EXTENSION)
    }

    @Test
    @TmsLink("1675")
    @SharedFolder
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldMoveFullAccessDirToNormalDir() {
        val sharedFolderName = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.MOVE, FilesAndFolders.TARGET_FOLDER, sharedFolderName)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(sharedFolderName)
    }

    @Test
    @TmsLink("2167")
    @SharedFolder
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @Category(FullRegress::class)
    fun shouldNotMoveReadOnlyDirToAnotherSharedFolder() {
        val names = nameHolder.names
        val readOnlyDir = names[0]
        val targetDir = names[1]
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.MOVE, targetDir, readOnlyDir)
        onFiles.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_MOVE_SOME_FILE_TOAST)
        onFiles.openFolder(targetDir)
        onFiles.shouldNotExistFilesOrFolders(readOnlyDir)
    }

    @Test
    @TmsLink("6255")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST])
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.CAMERA_UPLOADS])
    fun shouldMoveFilesFromAndIntoCameraFolder() {
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.MOVE, FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.FIRST)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FIRST)
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FIRST)
        onFiles.shouldMoveOrCopyFilesToDiskRoot(FileActions.MOVE, FilesAndFolders.FIRST)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FIRST)
        onFiles.pressHardBack()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FIRST)
    }

    @Test
    @TmsLink("6084")
    @Category(BusinessLogic::class)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    fun shouldMovePhotoFromFilePreview() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openImage(FilesAndFolders.FIRST)
        onPreview.openOptionsMenu()
        onGroupMode.applyAction(FileActions.MOVE)
        onFiles.shouldFilesListBeNotEmpty()
        onFiles.pressHardBack()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onFiles.wait(5, TimeUnit.SECONDS)
        onPreview.shouldNotBeOnPreview()
    }
}
