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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadAndAddToFavorites
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.*
import java.util.concurrent.TimeUnit

@Feature("Favorites_Album")
@UserTags("personal-album")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FavoritesAlbumTest : FavoritesAlbumTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7444")
    @Category(Regression::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1])
    @UploadFiles(filePaths = [FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldAddFilesToFavoritesAlbumInViewerFromSearchResults() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile(FilesAndFolders.JPG_SEARCH_REQUEST)
        onFiles.openImage(FilesAndFolders.JPG_2)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onFiles.shouldCloseSearch()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7448")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldAddFilesToFavoritesAlbumFromOtherAlbumOnGroupMode() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_2)
        onGroupMode.chooseAlbumForAddingViaMoreOption()
        onAlbums.clickFavoritesAlbum()
        onGroupMode.shouldGroupModeNotBeActivated()
        onPhotos.pressHardBack()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7457")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    fun shouldAddToFavoritesFromPreviewInOffline() {
        onBasePage.openPhotos()
        onPhotos.shouldDisplayItems(CommonConstants.CONNECTION_TIMEOUT, FilesAndFolders.JPG_1)
        onPhotos.switchToAirplaneMode()
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.switchToData()
        onBasePage.wait(CommonConstants.CONNECTION_DELAY, TimeUnit.SECONDS)
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayItems(CommonConstants.CONNECTION_TIMEOUT, FilesAndFolders.JPG_1)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
    }

    @Test
    @TmsLink("7458")
    @Category(Quarantine::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    fun shouldAddToFavoritesFromGroupModeInOffline() {
        onBasePage.openPhotos()
        onPhotos.shouldDisplayItems(CommonConstants.CONNECTION_TIMEOUT, FilesAndFolders.JPG_1)
        onPhotos.switchToAirplaneMode()
        onPhotos.selectItems(FilesAndFolders.JPG_1)
        onGroupMode.clickAddMediaItemToAlbumButton()
        onAlbums.clickFavoritesAlbum()
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.switchToData()
        onBasePage.wait(CommonConstants.CONNECTION_DELAY, TimeUnit.SECONDS)
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayItems(CommonConstants.CONNECTION_TIMEOUT, FilesAndFolders.JPG_1)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
    }

    @Test
    @TmsLink("7459")
    @Category(Regression::class)
    @AuthorizationTest
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2])
    fun shouldRemoveFromFavoritesInOffline() {
        onBasePage.openPhotos()
        onPhotos.shouldDisplayItems(CommonConstants.CONNECTION_TIMEOUT, FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
        onPhotos.switchToAirplaneMode()
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.clickRemoveFromFavoritesButton()
        onPreview.shouldAddToFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.switchToData()
        onBasePage.wait(CommonConstants.CONNECTION_DELAY, TimeUnit.SECONDS)
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7460")
    @Category(Regression::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2])
    fun shouldRemoveFromFavoritesFromGroupModeInOffline() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayItems(CommonConstants.CONNECTION_TIMEOUT, FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
        onPhotos.switchToAirplaneMode()
        onPhotos.selectItems(FilesAndFolders.JPG_1)
        onGroupMode.excludeSelectedItemsFromAlbum()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
        onPhotos.pressHardBack()
        onAlbums.switchToData()
        onAlbums.wait(CommonConstants.CONNECTION_DELAY, TimeUnit.SECONDS)
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7462")
    @Category(Quarantine::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldDeleteItemFromFavoritesAlbumOnGroupMode() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.selectItems(FilesAndFolders.JPG_1)
        onGroupMode.clickDeleteAndChooseFromDisk()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
        onPhotos.pressHardBack()
        onBasePage.openPhotos()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7317")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @CreateAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    fun shouldCreateFavoritesAlbumFromFeed() {
        onBasePage.openAlbums()
        onAlbums.shouldDisplayAlbum(PersonalAlbums.MOCK_ALBUM_NAME)
        onAlbums.shouldNotDisplayFavoritesAlbum()
        onBasePage.openFeed()
        onFeed.openFirstImage()
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.shouldDisplayFavoritesAlbum()
    }

    @Test
    @TmsLink("7461")
    @Category(Regression::class)
    @AuthorizationTest
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2])
    fun shouldDeleteFromFavoritesAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.wait(5, TimeUnit.SECONDS) //wait for deletion
        onPreview.pressHardBack()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
        onPhotos.pressHardBack()
        onBasePage.openPhotos()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7453")
    @Category(Regression::class)
    @AuthorizationTest
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeFilesFromFavoritesAlbumInViewerFromPhotos() {
        onBasePage.openPhotos()
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.clickRemoveFromFavoritesButton()
        onPreview.shouldAddToFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7482")
    @Category(FullRegress::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.TARGET_FOLDER])
    fun shouldCopyFileFromFavoriteAlbumOnGroupMode() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.selectItems(FilesAndFolders.JPG_2)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.COPY)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7483")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1_RENAMED, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldRenameFileInFavoriteAlbumOnGroupMode() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.selectItems(FilesAndFolders.JPG_1)
        onPhotos.shouldRenamePhoto(FilesAndFolders.JPG_1_RENAMED)
        onPhotos.wait(5, TimeUnit.SECONDS)
        onPhotos.pressHardBack()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1_RENAMED)
    }

    @Test
    @TmsLink("7468")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldShareFileLinkInViewerFromFavoriteAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.applyPhotoActionInPreview(FileActions.SHARE_LINK)
        onShare.shouldSeeShareMenu()
        onShare.pressHardBackNTimes(2)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7335")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.EXISTING_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.EXISTING_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldCreateFavoriteAlbumFromPersonal() {
        onBasePage.openAlbums()
        onAlbums.shouldNotDisplayFavoritesAlbum()
        onAlbums.clickAlbum(PersonalAlbums.EXISTING_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBackNTimes(2)
        onAlbums.shouldDisplayFavoritesAlbum()
    }

    @Test
    @TmsLink("7480")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldDownloadFileToDeviceFromFavoritesOnGroupMode() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.selectItems(FilesAndFolders.JPG_2)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.SAVE_ON_DEVICE)
        onPhotos.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
    }

    @Test
    @TmsLink("7475")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldRemoveFilesInViewerFromFavoritesAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.excludeFromAlbum()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.JPG_1)
        onPreview.pressHardBackNTimes(2)
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
    }
}
