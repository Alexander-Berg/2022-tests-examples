package ru.yandex.autotests.mobile.disk.android.delete

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.hamcrest.Matchers
import org.hamcrest.junit.MatcherAssert
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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbumWithFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CleanTrash
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.DeleteSharedFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushMediaToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.*
import java.util.concurrent.TimeUnit

@Feature("Delete files and folders")
@UserTags("deleting")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class DeleteFilesAndFoldersTest : DeleteFilesAndFoldersTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2452")
    @CleanTrash
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(BusinessLogic::class)
    fun shouldDeleteFilesOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("2453")
    @CleanTrash
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(BusinessLogic::class)
    fun shouldDeleteFoldersOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openOffline()
        onOffline.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
    }

    @Test
    @TmsLink("1550")
    @CleanTrash
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(BusinessLogic::class)
    fun shouldDeleteCameraUploadsFolder() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CAMERA_UPLOADS)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE)
        onFiles.shouldDeleteCameraUploadsAlertMessageBeDisplayed()
        onFiles.approveDeletingSpecialFolder()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.CAMERA_UPLOADS)
    }

    @Test
    @TmsLink("2421")
    @CleanTrash
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.CAMERA_UPLOADS
    )
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(BusinessLogic::class)
    fun shouldRemoveSeveralFilesAsGroupOnCameraUploads() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onBasePage.enableGroupOperationMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILES)
    }

    @Test
    @TmsLink("2416")
    @CleanTrash
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.CAMERA_UPLOADS
    )
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(BusinessLogic::class)
    fun shouldDeleteFilesFromViewerOnCameraUploads() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.closePreview()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("2448")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.CAMERA_UPLOADS)
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(BusinessLogic::class)
    fun shouldClosePreviewWhenAloneFileBeDeleted() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onFiles.shouldSeeEmptyFolderStub()
        onFiles.navigateUp()
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("1556")
    @AuthorizationTest
    @CleanTrash
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER])
    @DeleteSharedFolders
    @Category(BusinessLogic::class)
    fun shouldRestoreSharedFoldersByOwner() {
        onUserDiskApi.shareFolder(FilesAndFolders.ORIGINAL_FOLDER)
        onUserDiskApi.inviteUser(FilesAndFolders.ORIGINAL_FOLDER, sharedAccount, Rights.RW)
        onUserDiskApi.shareFolder(FilesAndFolders.TARGET_FOLDER)
        onUserDiskApi.inviteUser(FilesAndFolders.TARGET_FOLDER, sharedAccount, Rights.RO)
        onShareDiskApi.activateInvite() //activate all invites
        onBasePage.openFiles()
        onFiles.updateFileList()
        onFiles.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onFiles.wait(5, TimeUnit.SECONDS) // explicit wait for folders deleting
        onBasePage.openTrash()
        onTrash.shouldRestoreFilesFromTrash(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onTrash.navigateUp()
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.TARGET_FOLDER)
        onUserDiskApi.shouldFolderBeNotShared(FilesAndFolders.ORIGINAL_FOLDER)
        onUserDiskApi.shouldFolderBeNotShared(FilesAndFolders.TARGET_FOLDER)
    }

    @Test
    @TmsLink("1524")
    @CleanTrash
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RW))
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @Category(BusinessLogic::class)
    fun shouldRestoreSharedFolderByParticipant() {
        val names = nameHolder.names
        onBasePage.openFiles()
        onFiles.updateFileList()
        onFiles.deleteFilesOrFolders(*names)
        onBasePage.openTrash()
        onTrash.shouldTrashBeEmpty()
        for (name in names) {
            onShareDiskApi.shouldFolderBeShared(name)
        }
    }

    @Test
    @TmsLink("1515")
    @CleanTrash
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @Category(Regression::class)
    fun shouldDeleteCachedFile() {
        onBasePage.openSettings()
        val cacheSizeBeforeOperation = onSettings.currentCacheSize
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack()
        onBasePage.swipeUpToDownNTimes(2) //scroll up to file list top
        onBasePage.openSettings()
        val currentCacheSize = onSettings.currentCacheSize
        MatcherAssert.assertThat(currentCacheSize, Matchers.greaterThan(cacheSizeBeforeOperation))
        onSettings.closeSettings()
        onSettings.closeProfile()
        onFiles.deleteFilesOrFolders(FilesAndFolders.PHOTO)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.PHOTO)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
    }

    @Test
    @TmsLink("1559")
    @CleanTrash
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE])
    @DeleteFiles(files = [FilesAndFolders.BIG_FILE])
    @Category(Quarantine::class)
    fun shouldDeleteFileFromDiskWhileAddingToOffline() {
        onBasePage.openFiles()
        onMobile.switchNetworkSpeedToEDGE()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.BIG_FILE)
        onFiles.shouldSeeUploadProgressBar()
        onFiles.deleteFilesOrFolders(FilesAndFolders.BIG_FILE)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.BIG_FILE)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.BIG_FILE)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.BIG_FILE)
    }

    @Test
    @TmsLink("2351")
    @CleanTrash
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE], targetFolder = FilesAndFolders.ORIGINAL_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FOLDER])
    @Category(Quarantine::class)
    fun shouldDeleteFolderFromDiskWhileAddingToOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSeeUploadProgressBar()
        onFiles.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER, FilesAndFolders.BIG_FILE)
    }

    @Test
    @TmsLink("1545")
    @CleanTrash
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @Category(Quarantine::class)
    fun shouldDeleteFileFromSearchResultsOnOfflineTab() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onBasePage.openOffline()
        onOffline.shouldOpenSearch()
        onFiles.shouldSearchFile(FilesAndFolders.ORIGINAL_FILE)
        onFiles.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldSeedMoveToTrashSnackbar(MoveFilesToTrashSnackBarMessages.FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onMobile.hideKeyboardIfShown()
        onSearch.closeSearch()
        onOffline.navigateUp() //escape from offline
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("7102")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CleanTrash
    fun shouldDeleteRemoteFileInPersonalAlbumFromDisk() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.longTapCloudyItem(FilesAndFolders.JPG_1)
        onGroupMode.applyButtonAction(FileActions.DELETE)
        onGroupMode.applyPhotosAction(FileActions.DELETE_FROM_DISK)
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
        onPhotos.pressHardBack()
        onBasePage.openPhotos()
        onPhotos.shouldNotDisplayItems(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7116")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_2])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_2])
    @CleanTrash
    fun shouldTotallyDeleteRemoteLocalFileInPersonalAlbumFromViewer() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.JPG_2)
        onPreview.pressHardBack()
        onPreview.pressHardBack()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
    }
}
