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
import ru.yandex.autotests.mobile.disk.android.blocks.MediaItem
import ru.yandex.autotests.mobile.disk.android.core.api.data.shared.Rights
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbumWithFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.*
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushMediaToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.*
import java.util.concurrent.TimeUnit

@Feature("Delete files and folders")
@UserTags("deleting")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class DeleteFilesAndFoldersTest2 : DeleteFilesAndFoldersTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2422")
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE])
    @CleanTrash
    @Category(BusinessLogic::class)
    fun shouldRestoreRemovedFileByOneInFullAccessDir() {
        val faDir = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(faDir)
        onFiles.deleteFilesOrFolders(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp() //escape from FA dir
        onBasePage.openTrash()
        onTrash.shouldRestoreFilesFromTrash(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onTrash.navigateUp()
        onBasePage.openFiles()
        onFiles.openFolder(faDir)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("2420")
    @CleanTrash
    @SharedFolder
    @UploadToSharedFolder(filePaths = [FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE])
    @Category(BusinessLogic::class)
    fun shouldRemoveSeveralFilesAsGroupInFullAccessDir() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onBasePage.enableGroupOperationMode()
        onFiles.deleteFilesOrFolders(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_RESTORING, FilesAndFolders.ORIGINAL_FILE)
    }

    @Test
    @TmsLink("1521")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(BusinessLogic::class)
    fun shouldNotDeleteFileFromReadOnlyDirectory() {
        val roDir = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(roDir)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.DELETE)
    }

    @Test
    @TmsLink("1538")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE_1538])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE_1538])
    @Category(BusinessLogic::class)
    fun shouldDeleteFilesFromSearch() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE_1538)
        onFiles.shouldOpenSearch()
        onFiles.shouldSearchFile(FilesAndFolders.ORIGINAL_TEXT_FILE_1538)
        onFiles.wait(1, TimeUnit.SECONDS) //wait search animation complete
        onFiles.deleteFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE_1538)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE_1538)
    }

    @Test
    @TmsLink("1540")
    @CreateFolders(folders = [FilesAndFolders.UNIQUE_FOLDER_1540])
    @DeleteFiles(files = [FilesAndFolders.UNIQUE_FOLDER_1540])
    @Category(BusinessLogic::class)
    fun shouldDeleteFolderFromSearch() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.UNIQUE_FOLDER_1540)
        onFiles.shouldOpenSearch()
        onSearch.searchFilesOrFolders(FilesAndFolders.UNIQUE_FOLDER_POSTFIX)
        onFiles.deleteFilesOrFolders(FilesAndFolders.UNIQUE_FOLDER_1540)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.UNIQUE_FOLDER_1540)
        onSearch.shouldSeeNoResultsFoundStub()
    }

    @Test
    @TmsLink("1557")
    @UploadFiles(filePaths = [FilesAndFolders.BIG_FILE])
    @DeleteFiles(files = [FilesAndFolders.BIG_FILE])
    @Category(BusinessLogic::class)
    fun shouldRemoveFileOnDiskWhenDiskSwitchedToBackground() {
        onBasePage.openFiles()
        onFiles.deleteFilesOrFolders(FilesAndFolders.BIG_FILE)
        onFiles.runInBackground(10, TimeUnit.SECONDS) //switch to background
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.BIG_FILE)
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.BIG_FILE)
    }

    @Test
    @TmsLink("1534")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(BusinessLogic::class)
    fun shouldRemoveButtonBeDisabledOnPreviewWhenSeeFileFromReadOnlyDir() {
        val sharedFolder = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(sharedFolder)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.ORIGINAL_FILE)
        onPreview.shouldDeleteButtonNotExists()
    }

    @Test
    @TmsLink("1527")
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @UploadToSharedFolder(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @Category(BusinessLogic::class)
    fun shouldRemoveButtonBeDisabledOnPreviewWhenSeeFileFromReadOnlyDirOnOfflineTab() {
        val folderName = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(folderName)
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_FILE)
        onFiles.navigateUp()
        onBasePage.openOffline()
        onOffline.shouldOpenImageIntoViewer(FilesAndFolders.ORIGINAL_FILE)
        onPreview.shouldDeleteButtonNotExists()
    }

    @Test
    @TmsLink("1532")
    @UploadFiles(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldSwitchToPreviousPhotoWhenLastPhotoDeletedOnPreview() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.SECOND)
        onPreview.shouldCurrentImageBe(Images.SECOND)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.shouldCurrentImageBe(Images.FIRST)
    }

    @Test
    @TmsLink("1549")
    @CleanTrash
    @DeleteFiles(files = [FilesAndFolders.DOWNLOADS])
    @Category(Regression::class)
    fun shouldDeleteSomeFormatFromPreview() {
        onUserDiskApi.savePublicResourceToDisk(
            FilesAndFolders.DIFFERENT_FORMATS_FOLDER_URL,
            FilesAndFolders.TARGET_FOLDER
        )
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.DOWNLOADS)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        for (name in FilesAndFolders.DIFFERENT_FORMAT_IMAGES_NAMES) {
            onFiles.openImage(name!!)
            onPreview.shouldDeleteCurrentFileFromDisk()
            onPreview.closePreview()
        }
        onFiles.navigateToRoot()
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(*FilesAndFolders.DIFFERENT_FORMAT_IMAGES_NAMES)
    }

    @Test
    @TmsLink("1551")
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @Category(Regression::class)
    fun shouldCancelCameraUploadsDeleting() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.CAMERA_UPLOADS)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE)
        onFiles.shouldDeleteCameraUploadsAlertMessageBeDisplayed()
        onFiles.cancelDeletingCameraUploads()
        onBasePage.shouldBeOnGroupMode()
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.CAMERA_UPLOADS)
    }

    @Test
    @TmsLink("1561")
    @CleanTrash
    @CreateFolders(folders = [FilesAndFolders.SOCIAL_NETWORKS])
    @DeleteFiles(files = [FilesAndFolders.SOCIAL_NETWORKS])
    @Category(Regression::class)
    fun shouldDeleteSocialNetworkFolder() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.SOCIAL_NETWORKS)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DELETE)
        onFiles.shouldDeleteSocialNetworksAlertMessageBeDisplayed()
        onFiles.approveDeletingSpecialFolder()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.SOCIAL_NETWORKS)
    }

    @Test
    @TmsLink("7247")
    @Category(Regression::class)
    @CreateAlbumWithFiles(targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME, filesNames = [FilesAndFolders.JPG_1])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @CleanTrash
    fun shouldDeleteSingleFileFromViewer() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayItemWithStatus(FilesAndFolders.JPG_1, MediaItem.Status.CLOUDY)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPersonalAlbum.shouldDisplayEmptyAlbumStub()
    }

    @Test
    @TmsLink("7117")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_3])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_3])
    @CleanTrash
    fun shouldDeleteLastFileInPersonalAlbumFromViewer() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_3)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.JPG_2)
        onPreview.pressHardBack()
        onPreview.pressHardBack()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_3)
    }
}
