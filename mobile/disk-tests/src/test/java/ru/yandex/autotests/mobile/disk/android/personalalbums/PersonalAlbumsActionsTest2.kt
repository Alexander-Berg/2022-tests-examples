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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadAndAddToFavorites
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileSpec
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushMediaToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.UploadFileType
import ru.yandex.autotests.mobile.disk.data.*
import java.util.*
import java.util.concurrent.TimeUnit

@Feature("Personal_Albums")
@UserTags("personal-album")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PersonalAlbumsActionsTest2 : PersonalAlbumsTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7113")
    @Category(Regression::class)
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldSharePersonalAlbumLink() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPersonalAlbum.shareOpenedAlbum()
        onShareMenu.shouldSeeSystemShareMenu()
    }

    @Test
    @TmsLink("7232")
    @Category(Regression::class)
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME], arePublic = true)
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldDeletePersonalAlbumLink() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPersonalAlbum.makeOpenedAlbumPrivate()
        onPersonalAlbum.shouldOpenedAlbumBePrivate()
    }

    @Test
    @TmsLink("7233")
    @Category(Regression::class)
    @CreateAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.RENAMED_ALBUM_NAME])
    fun shouldRenamePersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPersonalAlbum.shouldOpenedAlbumHasTitle(PersonalAlbums.NEW_ALBUM_NAME)
        onPersonalAlbum.startRenameOpenedAlbum()
        onEditTextDialog.applyWithText(PersonalAlbums.RENAMED_ALBUM_NAME)
        onPersonalAlbum.shouldOpenedAlbumHasTitle(PersonalAlbums.RENAMED_ALBUM_NAME)
    }

    @Test
    @TmsLink("7106")
    @Category(Regression::class)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_2])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.TARGET_FOLDER])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldCopyFileFromPersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_2)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.COPY)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.COPY)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7111")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_2])
    @DeleteAlbums
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldShareFileLinkFromPersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_2)
        onGroupMode.clickPhotosShareMenu()
        onShare.shouldSeeShareMenu()
    }

    @Test
    @TmsLink("7126")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldSaveMediaItemOnDeviceFromPersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_2)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.SAVE_ON_DEVICE)
        onGroupMode.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.JPG_2, DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH)
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2,
            FilesAndFolders.JPG_2
        )
    }

    @Test
    @TmsLink("7123")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.TARGET_FOLDER])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldMoveMediaItemsInPersonalAlmum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.applyPhotoActionInPreview(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onPreview.pressHardBack()
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7109")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldRenameMediaItemsOnGroupModeInPersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_2)
        onPhotos.shouldRenamePhoto(FilesAndFolders.JPG_3)
        onPhotos.wait(5, TimeUnit.SECONDS)
        onPhotos.pressHardBack()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_3)
    }

    @Test
    @TmsLink("7124")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_3, FilesAndFolders.JPG_2]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldRenameMediaItemsInPersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.applyPhotoActionInPreview(FileActions.RENAME)
        onPreview.shouldRenamePhotoInViewer(FilesAndFolders.JPG_1)
        onPreview.wait(3, TimeUnit.SECONDS)
        onPreview.pressHardBack()
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.checkFileInfo(
            FilesAndFolders.JPG_1,
            Arrays.asList(FilesAndFolders.JPG_2_PHOTO_PROPERTIES, FilesAndFolders.JPG_2_PHOTO_PROPERTIES_LAND),
            FilesAndFolders.JPGS_REMOTE_PATH
        )
    }

    @Test
    @TmsLink("7125")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.TARGET_FOLDER])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldCopyLinkMediaItemsInPersonalAlmum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_2)
        onPreview.tryShareLink()
        onShare.shouldSeeShareMenu()
    }

    @Test
    @TmsLink("7121")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.TARGET_FOLDER])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldUseAsMediaItemsInPersonalAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.clickItem(FilesAndFolders.JPG_1)
        onPreview.applyPhotoActionInPreview(FileActions.USE_AS)
        onGroupMode.shouldSeeUseAsProgrammControl()
    }

    @Test
    @TmsLink("7107")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3, FilesAndFolders.TARGET_FOLDER])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.JPG_2])
    fun shouldMoveMediaItemsOnGroupModeInPersonalAlmum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_2)
        onGroupMode.clickPhotosMoreOption()
        onGroupMode.applyPhotosAction(FileActions.MOVE)
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.approveCopyingOrMoving(FileActions.MOVE)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("5625")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldOpenAlbumAfterLongtap() {
        onBasePage.openAlbums()
        onAlbums.longtapAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPersonalAlbum.shouldOpenedAlbumHasTitle(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("5671")
    @Category(FullRegress::class)
    fun shouldBeOnAlbumsPageAfterBackground() {
        onBasePage.openAlbums()
        onBasePage.sendApplicationToBackground()
        onBasePage.wait(2, TimeUnit.SECONDS)
        onBasePage.returnAppFromBackground()
        onAlbums.shouldBeOnAlbumsPage()
    }

    @Test
    @TmsLink("7584")
    @Category(FullRegress::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.NEW_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldSeeLayoutOptionsMenu() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(FileActions.CHANGE_LAYOUT)
        onGroupMode.applyAction(FileActions.CHANGE_LAYOUT)
        onGroupMode.shouldActionBePresented(FileActions.STANDARD)
        onGroupMode.shouldActionBePresented(FileActions.SMART_TILES)
    }

    @Test
    @TmsLink("7585")
    @Category(FullRegress::class)
    @UploadAndAddToFavorites(filesPaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    fun shouldSeeLayoutOptionsMenuInFavourites() {
        onBasePage.openAlbums()
        onAlbums.clickFavoritesAlbum()
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(FileActions.CHANGE_LAYOUT)
        onGroupMode.applyAction(FileActions.CHANGE_LAYOUT)
        onGroupMode.shouldActionBePresented(FileActions.STANDARD)
        onGroupMode.shouldActionBePresented(FileActions.SMART_TILES)
    }

    @Test
    @TmsLink("7586")
    @Category(FullRegress::class)
    @PushFileToDevice(
        fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 3)],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteFiles(files = [DeviceFilesAndFolders.DCIM_FULL_PATH])
    fun shouldSeeLayoutOptionsMenuInAutoalbums() {
        onBasePage.enableUnlimitedPhotoAutoupload()
        onBasePage.openAlbums()
        onBasePage.wait(5, TimeUnit.SECONDS)
        onAlbums.clickCameraAlbum()
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(FileActions.CHANGE_LAYOUT)
        onGroupMode.applyAction(FileActions.CHANGE_LAYOUT)
        onGroupMode.shouldActionBePresented(FileActions.STANDARD)
        onGroupMode.shouldActionBePresented(FileActions.SMART_TILES)
    }
}
