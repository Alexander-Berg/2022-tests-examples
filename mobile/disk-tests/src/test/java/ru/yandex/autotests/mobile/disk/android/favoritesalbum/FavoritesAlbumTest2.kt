package ru.yandex.autotests.mobile.disk.android.favoritesalbum

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbumWithFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.*
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushMediaToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.PersonalAlbums

@Feature("Favorites_Album")
@UserTags("personal-album")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FavoritesAlbumTest2 : FavoritesAlbumTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7447")
    @Category(Quarantine::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_3])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_2])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_2])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.CAMERA_UPLOADS])
    @CleanTrash
    fun shouldAddFilesToFavoritesAlbumFromGalleryOnGroupMode() {
        onBasePage.openPhotos()
        onPhotos.selectItems(FilesAndFolders.JPG_2)
        onGroupMode.clickAddMediaItemToAlbumButton()
        onAlbums.clickFavoritesAlbum()
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldNPhotosBeDisplayed(3)
    }

    @Test
    @TmsLink("7451")
    @Category(Regression::class)
    @AuthorizationTest
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldRemoveFileFromFavoritesAlbumFromFeed() {
        onBasePage.openFeed()
        onFeed.expandFeedBlock()
        onContentBlock.clickImage(FilesAndFolders.JPG_2)
        onPreview.clickRemoveFromFavoritesButton()
        onPreview.shouldAddToFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onContentBlock.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7331")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldCreateFavoritesAlbumInViewerFromFiles() {
        onBasePage.openFiles()
        onFiles.openImage(FilesAndFolders.JPG_2)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.shouldDisplayFavoritesAlbum()
    }

    @Test
    @TmsLink("7333")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldCreateFavoritesAlbumInViewerFromOtherAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onPreview.pressHardBack()
        onAlbums.shouldDisplayFavoritesAlbum()
    }

    @Test
    @TmsLink("7434")
    @Category(Regression::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeFilesInViewerFromFavoritesAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.clickRemoveFromFavoritesButton()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.JPG_1)
        onPreview.pressHardBack()
        onPreview.pressHardBack()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7437")
    @Category(Regression::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeFileFromFavoritesAlbumOnGroupMode() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.selectItems(FilesAndFolders.JPG_1)
        onGroupMode.excludeSelectedItemsFromAlbum()
        onGroupMode.shouldGroupModeNotBeActivated()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7439")
    @Category(Regression::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_3])
    @UploadFiles(filePaths = [FilesAndFolders.JPG_2])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldAddFilesToFavoritesAlbumInViewerFromPhotos() {
        onBasePage.openPhotos()
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7442")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldAddFilesToFavoritesAlbumInViewerFromOtherAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onPreview.pressHardBack()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7441")
    @Category(Regression::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1])
    @UploadFiles(filePaths = [FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldAddFilesToFavoritesAlbumInViewerFromFiles() {
        onBasePage.openFiles()
        onFiles.openImage(FilesAndFolders.JPG_2)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7332")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1])
    @UploadFiles(filePaths = [FilesAndFolders.GEO_VIDEO_TO_UPLOAD])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.GEO_VIDEO_TO_UPLOAD])
    fun shouldAddVideoToFavoritesAlbumInViewerFromFiles() {
        onBasePage.openFiles()
        onFiles.openImage(FilesAndFolders.GEO_VIDEO_TO_UPLOAD)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.GEO_VIDEO_TO_UPLOAD)
    }

    @Test
    @TmsLink("7328")
    @Category(FullRegress::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    fun shouldCreateFavoriteAlbumWithRemoteLocalPhoto() {
        onBasePage.openAlbums()
        onAlbums.shouldNotDisplayFavoritesAlbum()
        onBasePage.openPhotos()
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.shouldDisplayFavoritesAlbum()
    }

    @Test
    @TmsLink("7329")
    @Category(FullRegress::class)
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS + '/' + FilesAndFolders.JPG_1_AUTOUPLOADED])
    fun shouldCreateFavoriteAlbumWithLocalPhoto() {
        onBasePage.openAlbums()
        onAlbums.shouldNotDisplayFavoritesAlbum()
        onBasePage.openPhotos()
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.shouldDisplayFavoritesAlbum()
    }

    @Test
    @TmsLink("7330")
    @Category(FullRegress::class)
    @PushMediaToDevice(filePaths = [FilesAndFolders.GEO_VIDEO_TO_UPLOAD])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.GEO_VIDEO_TO_UPLOAD])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS + '/' + FilesAndFolders.GEO_VIDEO_AUTO_UPLOADED])
    fun shouldCreateFavoriteAlbumWithLocalVideo() {
        onBasePage.openAlbums()
        onAlbums.shouldNotDisplayFavoritesAlbum()
        onBasePage.openPhotos()
        onPhotos.clickItem(FilesAndFolders.GEO_VIDEO_TO_UPLOAD)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.shouldDisplayFavoritesAlbum()
    }

    @Test
    @TmsLink("7334")
    @Category(FullRegress::class)
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS + '/' + FilesAndFolders.JPG_1_AUTOUPLOADED])
    fun shouldCreateFavoriteAlbumFromLocalAlbum() {
        onBasePage.openAlbums()
        onAlbums.shouldNotDisplayFavoritesAlbum()
        onAlbums.clickAlbum(DeviceFilesAndFolders.DCIM)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onPreview.pressHardBack()
        onAlbums.shouldDisplayFavoritesAlbum()
    }

    @Test
    @TmsLink("7325")
    @Category(FullRegress::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CreateAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    fun shouldCreateFavoriteAlbumFromFeed() {
        onBasePage.openAlbums()
        onAlbums.shouldNotDisplayFavoritesAlbum()
        onBasePage.openFeed()
        onFeed.expandFeedBlock()
        onContentBlock.clickImage(FilesAndFolders.JPG_2)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onContentBlock.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7465")
    @Category(FullRegress::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.TARGET_FOLDER])
    fun shouldCopyFileInViewerFromFavoriteAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.applyPhotoActionInPreview(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.COPY)
        onPreview.pressHardBack()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7466")
    @Category(FullRegress::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.TARGET_FOLDER])
    fun shouldMoveFileInViewerFromFavoriteAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.applyPhotoActionInPreview(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onPreview.pressHardBack()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2)
    }
}
