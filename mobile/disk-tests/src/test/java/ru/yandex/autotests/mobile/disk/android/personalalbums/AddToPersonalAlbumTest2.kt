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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushMediaToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.PersonalAlbums

@Feature("Personal_Albums")
@UserTags("personal-album")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class AddToPersonalAlbumTest2 : PersonalAlbumsTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7238")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddFilesToPersonalAlbumFromFeedContentBlock() {
        onBasePage.openFeed()
        onFeed.expandFeedBlock()
        onContentBlock.selectImages(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onGroupMode.addSelectedFilesToAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7251")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddRemoteFileToPersonalAlbumFromPhotos() {
        onBasePage.openPhotos()
        onPhotos.longTapCloudyItem(FilesAndFolders.JPG_1)
        onGroupMode.addSelectedMediaItemsToAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7245")
    @Category(Regression::class)
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS + '/' + FilesAndFolders.JPG_1_AUTOUPLOADED])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddLocalFileToPersonalAlbumFromPhotos() {
        onBasePage.openPhotos()
        onPhotos.longTapLocalItem(FilesAndFolders.JPG_1)
        onGroupMode.addSelectedMediaItemsToAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1_AUTOUPLOADED)
    }

    @Test
    @TmsLink("7241")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddFileToPersonalAlbumFromViewer() {
        onBasePage.openPhotos()
        onPhotos.shouldOpenFirstMediaItem()
        onPreview.addOpenedFileToAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPreview.shouldBeOnPreview()
        onPreview.pressHardBack()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7239")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldAddFileToPersonalAlbumFromFiles() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onGroupMode.addSelectedFilesToAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.shouldGroupModeNotBeActivated()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
    }
}
