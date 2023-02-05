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
import ru.yandex.autotests.mobile.disk.android.blocks.MediaItem
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbumWithFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushMediaToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.CommonConstants
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.PersonalAlbums
import java.util.concurrent.TimeUnit

@Feature("Personal_Albums")
@UserTags("personal-album")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class AddToPersonalAlbumTest : PersonalAlbumsTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7235")
    @Category(Regression::class)
    @AuthorizationTest
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddFilesToPersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPersonalAlbum.startAddFiles()
        onPhotos.selectItems(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onPhotos.finishPhotosPicking()
        onPersonalAlbum.shouldOpenedAlbumHasTitle(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7314")
    @Category(Quarantine::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddFilesToPersonalAlbumWithoutInternetConnection() {
        onBasePage.openPhotos()
        onPhotos.shouldDisplayItems(
            CommonConstants.CONNECTION_TIMEOUT,
            FilesAndFolders.JPG_1,
            FilesAndFolders.JPG_2,
            FilesAndFolders.JPG_3
        )
        onBasePage.switchToAirplaneMode()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPersonalAlbum.startAddFiles()
        onPhotos.selectItems(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onPhotos.finishPhotosPicking()
        onPersonalAlbum.shouldOpenedAlbumHasTitle(PersonalAlbums.NEW_ALBUM_NAME)
        onPersonalAlbum.pressHardBack()
        onBasePage.openFiles()
        onBasePage.switchToData()
        onBasePage.wait(CommonConstants.CONNECTION_DELAY, TimeUnit.SECONDS)
        onFiles.updateFileList()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayItems(
            CommonConstants.CONNECTION_TIMEOUT,
            FilesAndFolders.JPG_1,
            FilesAndFolders.JPG_2,
            FilesAndFolders.JPG_3
        )
    }

    @Test
    @TmsLink("7240")
    @Category(Quarantine::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddRemoteLocalFileToPersonalAlbumFromPhotos() {
        onBasePage.openPhotos()
        onPhotos.longTapCloudyLocalItem(FilesAndFolders.JPG_1)
        onGroupMode.addSelectedMediaItemsToAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7250")
    @Category(Quarantine::class)
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_2])
    @UploadFiles(filePaths = [FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS + '/' + FilesAndFolders.JPG_1_AUTOUPLOADED, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddDifferentFilesToPersonalAlbumFromPhotos() {
        onBasePage.openPhotos()
        onPhotos.longTapLocalItem(FilesAndFolders.JPG_1)
        onPhotos.longTapCloudyItem(FilesAndFolders.JPG_3)
        onPhotos.longTapCloudyLocalItem(FilesAndFolders.JPG_2)
        onGroupMode.addSelectedMediaItemsToAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1_AUTOUPLOADED, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7246")
    @Category(Regression::class)
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS + '/' + FilesAndFolders.JPG_1_AUTOUPLOADED])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddLocalFileToPersonalAlbumFromViewer() {
        onBasePage.openPhotos()
        onPhotos.shouldDisplayItemWithStatus(FilesAndFolders.JPG_1, MediaItem.Status.LOCAL)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.addOpenedFileToAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPreview.shouldBeOnPreview()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1_AUTOUPLOADED)
    }

    @Test
    @TmsLink("7242")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddFilesToPersonalAlbumFromOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onBasePage.openOffline()
        onOfflineFolder.shouldSelectFilesOrFolders(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onGroupMode.addSelectedFilesToAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7243")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddFilesToPersonalAlbumFromSearchResults() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile(FilesAndFolders.JPG_SEARCH_REQUEST)
        onFiles.shouldSelectFilesOrFolders(*FilesAndFolders.JPG_SEARCH_RESULT)
        onGroupMode.addSelectedFilesToAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.shouldGroupModeNotBeActivated()
        onFiles.shouldCloseSearch()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(*FilesAndFolders.JPG_SEARCH_RESULT)
    }

    @Test
    @TmsLink("7237")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.EXISTING_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME, PersonalAlbums.EXISTING_ALBUM_NAME])
    fun shouldAddFileToPersonalAlbumFromExistingAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.EXISTING_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
        onGroupMode.addSelectedFilesToAnotherPersonalAlbumViaMoreOption(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.shouldGroupModeNotBeActivated()
        onPhotos.pressHardBack()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7273")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.CAMERA_UPLOADS])
    @UploadFiles(
        filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3],
        targetFolder = FilesAndFolders.CAMERA_UPLOADS
    )
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddFilesToPersonalAlbumFromCameraFolder() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.CAMERA_UPLOADS)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
        onGroupMode.addSelectedFilesToAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
    }
}
