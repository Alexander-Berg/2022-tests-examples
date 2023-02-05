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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.PersonalAlbums
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Personal_Albums")
@UserTags("personal-album")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class CreatePersonalAlbumTests2 : PersonalAlbumsTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7210")
    @Category(Quarantine::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CreateAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME, PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldCreateNewAlbumFromAlbumPartition() {
        onBasePage.openAlbums()
        onAlbums.clickNewAlbum()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onPhotos.finishPhotosPicking()
        onPersonalAlbum.shouldOpenedAlbumHasTitle(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7217")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldCreateAlbumFromFeedContentBlock() {
        onBasePage.openFeed()
        onFeed.expandFeedBlock()
        onContentBlock.selectImages(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onGroupMode.startCreateNewAlbumWithSelectedFiles()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onBasePage.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7306")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteAlbums
    fun shouldCreateAlbumOnShareFromFeedContentBlock() {
        onBasePage.openFeed()
        onFeed.expandFeedBlock()
        onContentBlock.selectImages(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        groupModeSteps.clickShareMenu()
        onGroupMode.clickOnShareAlbum()
        onGroupMode.shouldGroupModeNotBeActivated()
        onContentBlock.pressHardBack()
        onContentBlock.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(FilesAndFolders.JPGS_ALBUM_GENERATED_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onDiskApi.shouldAlbumExist(FilesAndFolders.JPGS_ALBUM_GENERATED_NAME)
    }

    @Test
    @TmsLink("7218")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldCreateAlbumFromFilesPartition() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onGroupMode.startCreateNewAlbumWithSelectedFiles()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onBasePage.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7220")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldCreateAlbumFromViewer() {
        onBasePage.openPhotos()
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.startCreateNewAlbumWithOpenedFile()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onPreview.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7262")
    @Category(Quarantine::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldCreateAlbumFromPhotos() {
        onBasePage.openPhotos()
        onPhotos.selectNPhoto(3)
        onPhotos.startCreateNewPersonalAlbum()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7227")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldCreateAlbumFromOfflineFolder() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onBasePage.openOffline()
        onOfflineFolder.shouldSelectFilesOrFolders(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onGroupMode.startCreateNewAlbumWithSelectedFiles()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onBasePage.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }
}
