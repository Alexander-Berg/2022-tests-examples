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
import ru.yandex.autotests.mobile.disk.android.feed.FeedTestRunner.CleanFeedFiles
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbumWithFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.*
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.UploadFileType
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.PersonalAlbums
import ru.yandex.autotests.mobile.disk.data.ToastMessages
import java.util.concurrent.TimeUnit

@Feature("Favorites_Album")
@UserTags("personal-album")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FavoritesAlbumTest4 : FavoritesAlbumTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7454")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeFilesFromFavoritesAlbumInViewerFromAlbums() {
        onBasePage.openPhotos()
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.startCreateNewAlbumWithOpenedFile()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onPreview.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.clickRemoveFromFavoritesButton()
        onPreview.shouldAddToFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onPhotos.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7323")
    @Category(FullRegress::class)
    @CreateFolders(folders = [IMAGE_BLOCK_DIR])
    @UploadFiles(fileSpecs = [UploadFileSpec(type = UploadFileType.VIDEO, count = 1)], targetFolder = IMAGE_BLOCK_DIR)
    @CleanFeedFiles
    fun shouldAddFilesFromVideoAutoUploadBlock() {
        onBasePage.openFeed()
        onFeed.openFirstImage()
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        val videoFileName = FilesAndFolders.VIDEO1.replaceFirst("mp4/".toRegex(), "")
        onPhotos.shouldDisplayOnce(videoFileName)
    }

    @Test
    @TmsLink("7455")
    @Category(FullRegress::class)
    @AuthorizationTest
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeFromFavoritesAlbumInViewerFromOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onBasePage.openOffline()
        onOfflineFolder.openImage(FilesAndFolders.JPG_2)
        onPreview.clickRemoveFromFavoritesButton()
        onPreview.shouldAddToFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7443")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1])
    @UploadFiles(filePaths = [FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldAddFileToFavoritesInViewerFromOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onBasePage.openOffline()
        onOfflineFolder.openImage(FilesAndFolders.JPG_2)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7472")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(targetAlbumName = PersonalAlbums.EXISTING_ALBUM_NAME, filesNames = [FilesAndFolders.JPG_1])
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteAlbums(albumsNames = [PersonalAlbums.EXISTING_ALBUM_NAME])
    fun shouldAddFileToExistingAlbumInViewerFromFavorites() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.clickItem(FilesAndFolders.JPG_3)
        onPreview.addOpenedFileToAlbum(PersonalAlbums.EXISTING_ALBUM_NAME)
        onPreview.wait(5, TimeUnit.SECONDS)
        onPreview.pressHardBack()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onPhotos.pressHardBack()
        onAlbums.clickAlbum(PersonalAlbums.EXISTING_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7481")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(targetAlbumName = PersonalAlbums.EXISTING_ALBUM_NAME, filesNames = [FilesAndFolders.JPG_1])
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteAlbums(albumsNames = [PersonalAlbums.EXISTING_ALBUM_NAME])
    fun shouldAddFileToExistingAlbumFromFavoritesOnGroupMode() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.selectItems(FilesAndFolders.JPG_3)
        onGroupMode.addSelectedFilesToAlbumViaMoreOption(PersonalAlbums.EXISTING_ALBUM_NAME)
        onGroupMode.shouldGroupModeNotBeActivated()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onPhotos.pressHardBack()
        onAlbums.clickAlbum(PersonalAlbums.EXISTING_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7464")
    @Category(FullRegress::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldCheckFavoritesStatus() {
        onBasePage.openPhotos()
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
    }
}
