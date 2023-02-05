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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbumWithFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.CreateAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CleanTrash
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushMediaToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.*
import java.util.concurrent.TimeUnit

@Feature("Personal_Albums")
@UserTags("personal-album")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class CreatePersonalAlbumTests : PersonalAlbumsTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("7228")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldCreateAlbumFromSearchResults() {
        onBasePage.openFiles()
        onFiles.shouldOpenSearch()
        onFiles.searchFile(FilesAndFolders.JPG_SEARCH_REQUEST)
        onFiles.shouldSelectFilesOrFolders(*FilesAndFolders.JPG_SEARCH_RESULT)
        onGroupMode.startCreateNewAlbumWithSelectedFiles()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onFiles.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onFiles.shouldCloseSearch()
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(*FilesAndFolders.JPG_SEARCH_RESULT)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7257")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldCreateAlbumWithRemoteLocalFile() {
        onBasePage.openPhotos()
        onPhotos.longTapCloudyLocalItem(FilesAndFolders.JPG_1)
        onGroupMode.startCreateNewAlbumWithSelectedMediaItems()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7258")
    @Category(Regression::class)
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.JPG_1])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldCreateAlbumWithRemoteFile() {
        onBasePage.openPhotos()
        onPhotos.longTapCloudyItem(FilesAndFolders.JPG_1)
        onGroupMode.startCreateNewAlbumWithSelectedMediaItems()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7259")
    @Category(Regression::class)
    @AuthorizationTest
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS + '/' + FilesAndFolders.JPG_1_AUTOUPLOADED])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldCreateAlbumWithLocalFile() {
        onBasePage.openPhotos()
        onPhotos.longTapLocalItem(FilesAndFolders.JPG_1)
        onGroupMode.startCreateNewAlbumWithSelectedMediaItems()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1_AUTOUPLOADED)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7261")
    @Category(Quarantine::class)
    @AuthorizationTest
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_2])
    @UploadFiles(filePaths = [FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS + '/' + FilesAndFolders.JPG_1_AUTOUPLOADED, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    fun shouldCreateAlbumWithDifferentFiles() {
        onBasePage.openPhotos()
        onPhotos.longTapLocalItem(FilesAndFolders.JPG_1)
        onPhotos.longTapCloudyItem(FilesAndFolders.JPG_3)
        onPhotos.longTapCloudyLocalItem(FilesAndFolders.JPG_2)
        onGroupMode.startCreateNewAlbumWithSelectedMediaItems()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1_AUTOUPLOADED, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7215")
    @Category(Regression::class)
    @CreateAlbumWithFiles(
        targetAlbumName = PersonalAlbums.EXISTING_ALBUM_NAME,
        filesNames = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3]
    )
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME, PersonalAlbums.EXISTING_ALBUM_NAME])
    fun shouldCreateAlbumFromExistingAlbum() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(PersonalAlbums.EXISTING_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
        onGroupMode.startCreateNewAlbumViaMoreOption()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldSeeToastWithMessage(ToastMessages.ALBUM_CREATED)
        onGroupMode.shouldGroupModeNotBeActivated()
        onPhotos.pressHardBack()
        onAlbums.clickAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.shouldDisplayOnce(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2)
    }

    @Test
    @TmsLink("7310")
    @Category(FullRegress::class)
    @AuthorizationTest
    @UploadFiles(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @CreateAlbums(albumsNames = [PersonalAlbums.MOCK_ALBUM_NAME])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME, PersonalAlbums.MOCK_ALBUM_NAME])
    fun shouldNotCreateAlbumWithoutInternetConnection() {
        onBasePage.openPhotos()
        onPhotos.shouldDisplayItems(
            CommonConstants.CONNECTION_TIMEOUT,
            FilesAndFolders.JPG_1,
            FilesAndFolders.JPG_2,
            FilesAndFolders.JPG_3
        )
        onBasePage.openAlbums()
        onBasePage.switchToAirplaneMode()
        onAlbums.clickNewAlbum()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onPhotos.finishPhotosPicking()
        onAlert.shouldDisplayMessage(AlertMessages.CREATE_ALBUM_ERROR_ALERT_MESSAGE, CommonConstants.CONNECTION_TIMEOUT)
        onAlert.clickNegativeButton()
        onPhotos.pressHardBack()
        onAlbums.shouldNotDisplayAlbum(PersonalAlbums.NEW_ALBUM_NAME)
        onDiskApi.shouldAlbumNotExist(PersonalAlbums.NEW_ALBUM_NAME)
    }

    @Test
    @TmsLink("7339")
    @Category(Quarantine::class)
    @PushMediaToDevice(filePaths = [FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_1, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_2, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.JPG_3])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    @DeleteAlbums(albumsNames = [PersonalAlbums.NEW_ALBUM_NAME])
    @CleanTrash
    fun shouldCreateAlbumAfterEnableNetwork() {
        onBasePage.openPhotos()
        onPhotos.shouldDisplayItems(
            CommonConstants.CONNECTION_TIMEOUT,
            FilesAndFolders.JPG_1,
            FilesAndFolders.JPG_2,
            FilesAndFolders.JPG_3
        )
        onBasePage.openAlbums()
        onBasePage.switchToAirplaneMode()
        onAlbums.clickNewAlbum()
        onEditTextDialog.applyWithText(PersonalAlbums.NEW_ALBUM_NAME)
        onPhotos.selectItems(FilesAndFolders.JPG_1, FilesAndFolders.JPG_2, FilesAndFolders.JPG_3)
        onPhotos.finishPhotosPicking()
        onAlert.shouldDisplayMessage(AlertMessages.CREATE_ALBUM_ERROR_ALERT_MESSAGE, CommonConstants.CONNECTION_TIMEOUT)
        onAlert.switchToData()
        onAlert.wait(CommonConstants.CONNECTION_DELAY, TimeUnit.SECONDS)
        onAlert.clickPositiveButton()
        onPersonalAlbum.shouldOpenedAlbumHasTitle(PersonalAlbums.NEW_ALBUM_NAME)
        onPersonalAlbum.wait(CommonConstants.CONNECTION_DELAY, TimeUnit.SECONDS)
        onDiskApi.shouldAlbumExist(PersonalAlbums.NEW_ALBUM_NAME)
    }
}
