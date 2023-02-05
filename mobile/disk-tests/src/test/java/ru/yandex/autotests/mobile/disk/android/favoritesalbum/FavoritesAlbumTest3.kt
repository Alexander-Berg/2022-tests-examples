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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadAndAddToFavorites
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.PersonalAlbums
import java.util.concurrent.TimeUnit

@Feature("Favorites_Album")
@UserTags("personal-album")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FavoritesAlbumTest3 : FavoritesAlbumTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7433")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @CreateAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    fun shouldRemoveLastItemFromFavoritesAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.clickRemoveFromFavoritesButton()
        onAlbums.shouldDisplayAlbum(PersonalAlbums.MOCK_ALBUM_NAME)
        onAlbums.shouldNotDisplayFavoritesAlbum()
    }

    @Test
    @TmsLink("7436")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @CreateAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    fun shouldRemoveLastItemFromFavoritesAlbumOnGroupMode() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.selectItems(FilesAndFolders.JPG_1)
        onGroupMode.excludeSelectedItemsFromAlbum()
        onAlbums.shouldDisplayAlbum(PersonalAlbums.MOCK_ALBUM_NAME)
        onAlbums.shouldNotDisplayFavoritesAlbum()
    }

    @Test
    @TmsLink("7446")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1])
    @UploadFiles(filePaths = [FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldAddFilesToFavoritesAlbumFromFilesOnGroupMode() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.JPG_2)
        onGroupMode.shouldAddFilesToFavorites()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7449")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1])
    @UploadFiles(filePaths = [FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldAddFilesToFavoritesAlbumFromOfflineFolderOnGroupMode() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onBasePage.openOffline()
        onOfflineFolder.shouldSelectFilesOrFolders(FilesAndFolders.JPG_2)
        onGroupMode.shouldAddFilesToFavorites()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7450")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1])
    @UploadFiles(filePaths = [FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldAddFilesToFavoritesAlbumFromSearchResultsOnGroupMode() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile(FilesAndFolders.JPG_SEARCH_REQUEST)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.JPG_2)
        onGroupMode.shouldAddFilesToFavorites()
        onFiles.shouldCloseSearch()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7440")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1])
    @UploadFiles(filePaths = [FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldAddFileToFavoritesAlbumInViewerFromFeed() {
        onBasePage.openFeed()
        onFeed.expandFeedBlock()
        onContentBlock.clickImage(FilesAndFolders.JPG_2)
        onPreview.clickAddToFavoritesButton()
        onPreview.shouldRemoveFromFavoritesButtonBeDisplayed()
        onBasePage.pressHardBackNTimes(2)
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7456")
    @Category(FullRegress::class)
    @AuthorizationTest
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeFilesFromFavoritesAlbumInViewerFromSearchResults() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile(FilesAndFolders.JPG_SEARCH_REQUEST)
        onFiles.openImage(FilesAndFolders.JPG_2)
        onPreview.clickRemoveFromFavoritesButton()
        onPreview.shouldAddToFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onFiles.shouldCloseSearch()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7452")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeFromFavoritesAlbumInViewerFromFiles() {
        onBasePage.openAlbums()
        onBasePage.wait(10, TimeUnit.SECONDS) //wait for albums sync
        onAlbums.shouldDisplayFavoritesAlbum()
        onBasePage.openFiles()
        onFiles.openImage(FilesAndFolders.JPG_2)
        onPreview.clickRemoveFromFavoritesButton()
        onPreview.shouldAddToFavoritesButtonBeDisplayed()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7303")
    @Category(Regression::class)
    @AuthorizationTest
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @CreateAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    fun shouldCreateFavoritesAlbumFromGallery() {
        onBasePage.openAlbums()
        onAlbums.shouldDisplayAlbum(PersonalAlbums.MOCK_ALBUM_NAME)
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
    @TmsLink("7445")
    @Category(Regression::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_3])
    @UploadFiles(filePaths = [FilesAndFolders.JPG_2])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldAddFilesToFavoritesAlbumFromFeedOnGroupMode() {
        onBasePage.openFeed()
        onFeed.expandFeedBlock()
        onContentBlock.selectImages(FilesAndFolders.JPG_2)
        onGroupMode.shouldAddFilesToFavorites()
        onContentBlock.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2)
    }
}
