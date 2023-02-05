package ru.yandex.autotests.mobile.disk.android.copyfiles

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages
import java.util.*
import java.util.concurrent.TimeUnit

@Feature("Copy files and folders")
@UserTags("copyFiles")
@RunWith(GuiceTestRunner::class)
@GuiceModules(
    AndroidModule::class
)
class CopyFilesTest : CopyFilesTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2352")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldCopyFolder() {
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.COPY,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER
        )
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.openFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
    }

    @Test
    @TmsLink("2353")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldCopyFiles() {
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.COPY,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_TEXT_FILE
        )
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
    }

    @Test
    @TmsLink("2369")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.PHOTO, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldCopyFileToFolderWhereExistFileWithSameNameButOtherCaseInExtension() {
        onDiskApi.rename(
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.PHOTO,
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.PHOTO_BIG_CASE_EXTENSION,
            true
        )
        onBasePage.openFiles()
        onFiles.shouldMoveOrCopyFilesToFolder(FileActions.COPY, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.PHOTO)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO, FilesAndFolders.PHOTO_BIG_CASE_EXTENSION)
    }

    @Test
    @TmsLink("2362")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldCopyFolderToFolderWhereExistFileWithSameNameButOtherCaseInName() {
        onBasePage.openFiles()
        val folderUpperCase = FilesAndFolders.ORIGINAL_FOLDER.uppercase(Locale.getDefault())
        onDiskApi.rename(
            FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.TARGET_FOLDER + "/" + folderUpperCase,
            true
        )
        onFiles.shouldMoveOrCopyFilesToFolder(
            FileActions.COPY,
            FilesAndFolders.TARGET_FOLDER,
            FilesAndFolders.ORIGINAL_FOLDER
        )
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER, folderUpperCase)
    }

    @Test
    @TmsLink("2365")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeErrorWhenCopyFileDeletedOnServer() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onDiskApi.removeFiles(FilesAndFolders.ORIGINAL_FILE)
        onFiles.pressButton(FileActions.COPY)
        onFiles.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_COPY_CERTAIN_FILES_TOAST)
    }

    @Test
    @TmsLink("2366")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeErrorWhenCopyFolderDeletedOnServer() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onDiskApi.removeFiles(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.pressButton(FileActions.COPY)
        onFiles.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_COPY_CERTAIN_FILES_TOAST)
    }

    @Test
    @TmsLink("2367")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeErrorWhenCopyToFolderDeletedOnServer() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.COPY)
        onFiles.wait(1, TimeUnit.SECONDS)
        onFiles.waitForFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onDiskApi.removeFiles(FilesAndFolders.TARGET_FOLDER)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSeeToastWithMessage(ToastMessages.FOLDER_WAS_DELETED_TOAST)
        onFiles.shouldUnableToFileListStubBePresented()
        onFiles.pressButton(FileActions.COPY)
        onFiles.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_COPY_CERTAIN_FILES_TOAST)
    }

    @Test
    @TmsLink("2433")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.PHOTO, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldCopyPhotoFromAllPhotos() {
        onBasePage.openPhotos()
        onAllPhotos.selectPhoto()
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.COPY)
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
    }

    @Test
    @TmsLink("2434")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldCopyFileOnOfflineTab() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.COPY)
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("2435")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldCopyFolderOnOfflineTab() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.COPY)
        onOffline.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
    }
}
