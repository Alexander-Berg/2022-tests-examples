package ru.yandex.autotests.mobile.disk.android.deletelink

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.PublishFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Delete link")
@UserTags("deleteLink")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class DeleteLinkTest : DeleteLinkTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2475")
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(Regression::class)
    fun shouldNotSeePublicMarkerForUnpublishedFolder() {
        onBasePage.openFiles()
        deletePublicLink(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("2133")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.CAMERA_UPLOADS)
    @PublishFiles(files = [FilesAndFolders.CAMERA_UPLOADS + "/" + FilesAndFolders.FILE_FOR_VIEWING_1])
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(FullRegress::class)
    fun shouldRemovePublicLinkOnFileOnCameraUploads() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldSeePublicFileMarkOnGridMode()
        onFiles.shouldSelectPhotoOnGridMode()
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("2759")
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS, FilesAndFolders.CAMERA_UPLOADS + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @PublishFiles(files = [FilesAndFolders.CAMERA_UPLOADS + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(FullRegress::class)
    fun shouldRemovePublicLinkOnFolderOnCameraUploads() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("2141")
    @SharedFolder
    @Category(FullRegress::class)
    fun shouldNotDeletePublicLinkWhenFolderRightChangedAfterUpdatingFileList() {
        val folderName = nameHolder.name
        onShareDiskApi.publishFile(folderName)
        onBasePage.openFiles()
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(folderName)
        onShareDiskApi.changeRights(folderName, account, Rights.RO)
        deletePublicLink(folderName)
        onFiles.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_DELETE_LINK_TOAST)
        onFiles.shouldSeePublicFileMark(folderName)
    }

    @Test
    @TmsLink("2142")
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(FullRegress::class)
    fun shouldNotDeletePublicLinkOnReadOnlyDirWhenDeletedWithAnotherFiles() {
        val folderName = nameHolder.name
        onShareDiskApi.publishFile(folderName)
        onBasePage.openFiles()
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(folderName)
        onFiles.shouldSeePublicFileMark(folderName, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(folderName, FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
        onFiles.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_DELETE_LINK_TOAST)
        onFiles.shouldSeePublicFileMark(folderName)
        onFiles.shouldNotSeePublicFileMark(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("2126")
    @CreateFolders(folders = [FilesAndFolders.CONTAINER_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER])
    @UploadFiles(
        filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.CONTAINER_FOLDER
    )
    @PublishFiles(files = [FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.CONTAINER_FOLDER + "/" + FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.CONTAINER_FOLDER])
    @Category(FullRegress::class)
    fun shouldRemovePublicLinkFromFilesAndFolders() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CONTAINER_FOLDER)
        onFiles.shouldSeePublicFileMark(
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.TARGET_FOLDER
        )
        onFiles.shouldSelectFilesOrFolders(
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.TARGET_FOLDER
        )
        onGroupMode.shouldNCheckboxesBeChecked(4)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
        onBasePage.shouldBeNotOnGroupMode()
        onFiles.shouldNotSeePublicFileMark(
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_FOLDER,
            FilesAndFolders.TARGET_FOLDER
        )
    }

    @Test
    @TmsLink("2139")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER])
    @Category(FullRegress::class)
    fun shouldSeeErrorToastWhenDeleteLinkOnMovedFile() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
        onUserDiskApi.moveFileToFolder(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.TARGET_FOLDER, true)
        onUserDiskApi.shouldFileExist(FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
        onGroupMode.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_DELETE_LINK_TOAST)
    }

    @Test
    @TmsLink("2140")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE])
    @Category(FullRegress::class)
    fun shouldSeeErrorToastWhenDeleteLinkOnRenamedFile() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(FilesAndFolders.ORIGINAL_FILE)
        onUserDiskApi.rename(FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.RENAMED_FILE, true)
        onUserDiskApi.shouldFileExist(FilesAndFolders.RENAMED_FILE)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
        onGroupMode.shouldSeeToastWithMessage(ToastMessages.UNABLE_TO_DELETE_LINK_TOAST)
    }

    @Test
    @TmsLink("3422")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.FILE_FOR_SEARCH, FilesAndFolders.FILE_FOR_GO, FilesAndFolders.PHOTO])
    @PublishFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.FILE_FOR_SEARCH, FilesAndFolders.FILE_FOR_GO, FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.FILE_FOR_SEARCH, FilesAndFolders.FILE_FOR_GO, FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldSeeDeleteLinkAlertWhenDeleteLinkOnSeveralFiles() {
        onBasePage.openFiles()
        onFiles.shouldSeePublicFileMark(
            FilesAndFolders.FILE_FOR_GO,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.FILE_FOR_SEARCH,
            FilesAndFolders.PHOTO
        )
        onFiles.shouldSelectFilesOrFolders(
            FilesAndFolders.FILE_FOR_GO,
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.FILE_FOR_SEARCH,
            FilesAndFolders.PHOTO
        )
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE_LINK)
        onFiles.shouldSeeDeletePublicLinkAlert()
        onFiles.shouldNotSeePublicFileMark(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.FILE_FOR_SEARCH,
            FilesAndFolders.FILE_FOR_GO,
            FilesAndFolders.PHOTO
        )
    }
}
