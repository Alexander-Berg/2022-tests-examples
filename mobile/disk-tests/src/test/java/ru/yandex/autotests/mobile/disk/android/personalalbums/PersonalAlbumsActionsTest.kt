package ru.yandex.autotests.mobile.disk.android.personalalbums

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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbumWithFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushMediaToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.*
import java.util.concurrent.TimeUnit

@Feature("Personal_Albums")
@UserTags("personal-album")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PersonalAlbumsActionsTest : PersonalAlbumsTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7230")
    @Category(Regression::class)
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME, PersonalAlbums.MOCK_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME, PersonalAlbums.MOCK_ALBUM_NAME])
    fun shouldDeletePersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.REMOVE_ALBUM)
        onAlert.shouldDisplayMessage(AlertMessages.DELETE_ALBUM_ALERT_MESSAGE)
        onAlert.clickPositiveButton()
        onAlbums.shouldDisplayAlbum(PersonalAlbums.MOCK_ALBUM_NAME)
        onAlbums.shouldNotDisplayAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onDiskApi.shouldAlbumNotExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7311")
    @Category(FullRegress::class)
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME, PersonalAlbums.MOCK_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME, PersonalAlbums.MOCK_ALBUM_NAME])
    fun shouldNotDeleteAlbumWithoutInternetConnection() {
        onBasePage.openAlbums()
        onBasePage.switchToAirplaneMode()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.REMOVE_ALBUM)
        onAlert.shouldDisplayMessage(AlertMessages.DELETE_ALBUM_ALERT_MESSAGE)
        onAlert.clickPositiveButton()
        onAlert.shouldDisplayMessage(AlertMessages.DELETE_ALBUM_ERROR_ALERT_MESSAGE, CommonConstants.CONNECTION_TIMEOUT)
        onAlert.clickNegativeButton()
        onAlert.waitUntilClose()
        onPersonalAlbum.pressHardBack()
        onAlbums.shouldDisplayAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7338")
    @Category(FullRegress::class)
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME, PersonalAlbums.MOCK_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME, PersonalAlbums.MOCK_ALBUM_NAME])
    fun shouldDeleteAlbumAfterEnableNetwork() {
        onBasePage.openAlbums()
        onBasePage.switchToAirplaneMode()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.REMOVE_ALBUM)
        onAlert.shouldDisplayMessage(AlertMessages.DELETE_ALBUM_ALERT_MESSAGE)
        onAlert.clickPositiveButton()
        onAlert.shouldDisplayMessage(AlertMessages.DELETE_ALBUM_ERROR_ALERT_MESSAGE, CommonConstants.CONNECTION_TIMEOUT)
        onAlert.switchToData()
        onAlert.wait(CommonConstants.CONNECTION_DELAY, TimeUnit.SECONDS)
        onAlert.clickPositiveButton()
        onAlbums.shouldDisplayAlbum(PersonalAlbums.MOCK_ALBUM_NAME)
        onAlbums.shouldNotDisplayAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onDiskApi.shouldAlbumNotExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7110")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1_RENAMED, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    fun shouldRenameFileInPersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_1)
        onPhotos.shouldRenamePhoto(FilesAndFolders.JPG_1_RENAMED)
        onPersonalAlbum.shouldOpenedAlbumHasTitle(PersonalAlbums.NEW_ALBUM_NAME)
        onPersonalAlbum.pressHardBack()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1_RENAMED)
    }

    @Test
    @TmsLink("7249")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldSaveMediaItemOnDeviceFromViewerInPersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.saveOnDeviceCurrentImage()
        onPreview.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.JPG_2, DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH)
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2,
            FilesAndFolders.JPG_2
        )
    }

    @Test
    @TmsLink("7134")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldRestoreAlbumItemFromTrash() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_2)
        onGroupMode.applyButtonAction(FileActions.DELETE)
        onGroupMode.applyPhotosAction(FileActions.DELETE_FROM_DISK)
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
        onBasePage.pressHardBack()
        onBasePage.openTrash()
        onTrash.shouldFilesOrFoldersExist(FilesAndFolders.JPG_2)
        onTrash.shouldRestoreFilesFromTrash(FilesAndFolders.JPG_2)
        onTrash.shouldSeeToastWithMessage(String.format(ToastMessages.FILE_RESTORED, FilesAndFolders.JPG_2))
        onBasePage.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7137")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldGoToPersonalAlbumFromViewerInfoPanel() {
        onBasePage.openPhotos()
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.openPhotoInformation()
        onPreview.openAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPersonalAlbum.shouldOpenedAlbumHasTitle(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7108")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.TARGET_FOLDER])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    fun shouldMoveFileFromPersonalAlbumToFolder() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_1)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7112")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldShareLinkFromTiles() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.longTapCloudyItem(FilesAndFolders.JPG_3)
        onPhotos.openOptionsMenu()
        onGroupMode.clickOnShareSingleFileLink()
        onShareMenu.shouldSeeShareMenu()
    }

    @Test
    @TmsLink("7122")
    @Category(FullRegress::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.TARGET_FOLDER])
    fun shouldCopyFileFromViewer() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_3)
        onPreview.openOptionsMenu()
        onGroupMode.applyPhotosAction(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.COPY)
        onPreview.openPhotoInformation()
        onPreview.shouldClickLocationInfo()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7105")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.TARGET_FOLDER])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldCopyMediaItemsOnGroupModeInPersonalAlmum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_2)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.COPY)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2)
    }
}
