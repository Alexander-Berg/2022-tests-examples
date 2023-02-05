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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
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
class ExcludeFromPersonalAlbumTest : PersonalAlbumsTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7253")
    @Category(Regression::class)
    @CreateAlbumWithFiles(targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME, filesNames = [FilesAndFolders.JPG_1])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    fun shouldExcludeSingleFileFromViewer() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayItemWithStatus(FilesAndFolders.JPG_1, MediaItem.Status.CLOUDY)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.shouldBeOnPreview()
        onPreview.excludeFromAlbum()
        onPersonalAlbum.shouldDisplayEmptyAlbumStub()
    }

    @Test
    @TmsLink("7254")
    @Category(Quarantine::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    fun shouldExcludeRemoteLocalFileFromAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.longTapCloudyLocalItem(FilesAndFolders.JPG_1)
        onGroupMode.excludeSelectedItemsFromAlbum()
        onGroupMode.shouldGroupModeNotBeActivated()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7255")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    fun shouldExcludeDifferentFilesFromAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
        onGroupMode.excludeSelectedItemsFromAlbum()
        onGroupMode.shouldGroupModeNotBeActivated()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7139")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeRemoteFileFromAlbumWithDisplayingInPhotos() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_1)
        onGroupMode.excludeSelectedItemsFromAlbum()
        onGroupMode.shouldGroupModeNotBeActivated()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
        onPhotos.pressHardBack()
        onBasePage.openPhotos()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7256")
    @Category(Quarantine::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    fun shouldExcludeRemoteLocalFileFromAlbumWithDisplayingInPhotos() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.longTapCloudyLocalItem(FilesAndFolders.JPG_1)
        onGroupMode.excludeSelectedItemsFromAlbum()
        onGroupMode.shouldGroupModeNotBeActivated()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
        onPhotos.pressHardBack()
        onBasePage.openPhotos()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7316")
    @Category(Quarantine::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_2])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_2])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldExcludeRemoteLocalFileFromAlbumWithoutInternetConnection() {
        onBasePage.openAlbums()
        onBasePage.switchToAirplaneMode()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayItemWithStatus(FilesAndFolders.JPG_2, MediaItem.Status.CLOUDY_LOCAL)
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.excludeFromAlbum()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.JPG_3)
        onPreview.pressHardBack()
        onPreview.pressHardBack()
        onPersonalAlbum.pressHardBack()
        onBasePage.switchToData()
        onBasePage.openFiles()
        onFiles.updateFileList()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
    }
}
