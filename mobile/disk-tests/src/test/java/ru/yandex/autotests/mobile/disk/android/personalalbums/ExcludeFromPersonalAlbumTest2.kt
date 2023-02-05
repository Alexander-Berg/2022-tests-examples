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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CleanTrash
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushMediaToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.PersonalAlbums
import java.util.concurrent.TimeUnit

@Feature("Personal_Albums")
@UserTags("personal-album")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class ExcludeFromPersonalAlbumTest2 : PersonalAlbumsTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7229")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeAllItemsFromAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onGroupMode.excludeSelectedItemsFromAlbum()
        onGroupMode.shouldGroupModeNotBeActivated()
        onPersonalAlbum.shouldDisplayEmptyAlbumStub()
        onPersonalAlbum.pressHardBack()
        onAlbums.shouldDisplayAlbum(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7129")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeRemoteFileFromViewer() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayItemWithStatus(FilesAndFolders.JPG_1, MediaItem.Status.CLOUDY)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.excludeFromAlbum()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.JPG_2)
        onPreview.pressHardBack()
        onPreview.pressHardBack()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7244")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeRemoteFileFromAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.longTapCloudyItem(FilesAndFolders.JPG_1)
        onGroupMode.excludeSelectedItemsFromAlbum()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7252")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    fun shouldExcludeRemoteLocalFileFromViewer() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayItemWithStatus(FilesAndFolders.JPG_1, MediaItem.Status.CLOUDY_LOCAL)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.excludeFromAlbum()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.JPG_2)
        onPreview.pressHardBack()
        onPreview.pressHardBack()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_1)
    }

    @Test
    @TmsLink("7130")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    fun shouldExcludeLastFileFromViewer() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayItemWithStatus(FilesAndFolders.JPG_3, MediaItem.Status.CLOUDY)
        onPhotos.clickItem(FilesAndFolders.JPG_3)
        onPreview.excludeFromAlbum()
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.JPG_2)
        onPreview.pressHardBack()
        onPreview.pressHardBack()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7099")
    @Category(Quarantine::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_2])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_2])
    @CleanTrash
    fun shouldDeleteRemoteLocalFileInPersonalAlbumFromDisk() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.longTapCloudyLocalItem(FilesAndFolders.JPG_2)
        onGroupMode.applyButtonAction(FileActions.DELETE)
        onGroupMode.applyPhotosAction(FileActions.DELETE_ONLY_FROM_DISK)
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
        onPhotos.pressHardBack()
        onBasePage.openPhotos()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7101")
    @Category(Quarantine::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_2])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_2])
    @CleanTrash
    fun shouldDeleteRemoteLocalFileInPersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.longTapCloudyLocalItem(FilesAndFolders.JPG_2)
        onGroupMode.clickDeleteAndChooseFromDiskAndDevice()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
        onPhotos.pressHardBack()
        onBasePage.openPhotos()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7100")
    @Category(Quarantine::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_2])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_2])
    @CleanTrash
    fun shouldDeleteRemoteLocalFileInPersonalAlbumFromDevice() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.longTapCloudyLocalItem(FilesAndFolders.JPG_2)
        onGroupMode.clickDeleteFromDevice()
        onGroupMode.wait(3, TimeUnit.SECONDS)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7135")
    @Category(Quarantine::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CleanTrash
    fun shouldDeletePersonalAlbumFileFromPhotos() {
        onBasePage.openPhotos()
        onPhotos.selectItems(FilesAndFolders.JPG_1)
        onGroupMode.applyButtonAction(FileActions.DELETE)
        onGroupMode.applyPhotosAction(FileActions.DELETE_FROM_DISK)
        onPhotos.shouldNotDisplayItems(FilesAndFolders.JPG_1)
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldNotDisplayItems(FilesAndFolders.JPG_1)
    }
}
