package ru.yandex.autotests.mobile.disk.android.photos

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images
import java.util.concurrent.TimeUnit

@Feature("Photos delete operations")
@UserTags("photosDeleteOperations")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotosDeleteOperationsTest : PhotosOperationsRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("3975")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldDeleteLocalPhotoFromPhone() {
        preparePhotosOperationsTest()
        prepareLocalItem(FilesAndFolders.FIRST)
        onPhotos.selectItems(FilesAndFolders.FIRST)
        onPhotos.shouldDeleteCheckedLocalPhotosFromPhone()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("3976")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldDeleteServerPhotoFromServer() {
        preparePhotosOperationsTest()
        prepareServerItem(FilesAndFolders.FIRST)
        onPhotos.selectNPhoto(1)
        onPhotos.shouldDeleteCheckedPhotosFromServerOnly()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onDiskApi.shouldFileNotExist(FilesAndFolders.FIRST)
    }

    @Test
    @TmsLink("3977")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldDeleteServerLocalPhotoFromServer() {
        preparePhotosOperationsTest()
        onPhotos.selectItems(FilesAndFolders.FIRST)
        onPhotos.shouldDeleteCheckedPhotosFromServer()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onDiskApi.shouldFileNotExist(FilesAndFolders.FIRST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("3978")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldDeleteServerLocalPhotoFromPhone() {
        preparePhotosOperationsTest()
        onPhotos.selectItems(FilesAndFolders.FIRST)
        onPhotos.shouldDeleteCheckedPhotosFromPhone()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onDiskApi.shouldFileExist(FilesAndFolders.FIRST)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("3980")
    @Category(BusinessLogic::class)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.SECOND])
    fun shouldDeleteServerAndLocalPhotosFromServerAndPhone() {
        preparePhotosOperationsTest()
        prepareServerItem(FilesAndFolders.FIRST)
        prepareLocalItem(FilesAndFolders.SECOND)
        onPhotos.selectNPhoto(2)
        onPhotos.shouldDeleteCheckedPhotosFromServerAndPhone()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onDiskApi.shouldFileNotExist(FilesAndFolders.FIRST)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.SECOND, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("4265")
    @Category(Regression::class)
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.SECOND])
    fun shouldEmptyViewBeDisplayed() {
        onBasePage.openPhotos()
        onPhotos.selectItems(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onPhotos.shouldDeleteCheckedLocalPhotosFromPhone()
        onPhotos.shouldEmptyViewBeDisplayed()
    }

    @Test
    @TmsLink("4270")
    @Category(Regression::class)
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.SECOND])
    fun shouldDeleteLocalFileAfterPreviewVisit() {
        onBasePage.openPhotos()
        onPhotos.clickItem(FilesAndFolders.FIRST)
        onPreview.shouldBeOnPreview()
        onPreview.pressHardBack()
        onPhotos.selectItems(FilesAndFolders.FIRST)
        onPhotos.shouldDeleteCheckedLocalPhotosFromPhone()
        onPhotos.shouldDisplayAnyItemsButNot(FilesAndFolders.FIRST)
    }

    @Test
    @TmsLink("4292")
    @Category(Regression::class)
    @PrepareServerLocalPhotos
    fun shouldDeleteLastServerLocalPhotoFromServerAndPhone() {
        preparePhotosOperationsTest()
        onPhotos.shouldOpenLastVisibleMediaItem()
        onPreview.shouldDeleteCurrentFileFromDiskAndDevice()
        onPreview.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onPreview.shouldCurrentPhotoBe(Images.SECOND)
        onDiskApi.shouldFileNotExist(FilesAndFolders.FIRST)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("4293")
    @Category(Regression::class)
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.SECOND])
    fun shouldDeleteLastLocalPhotoFromPhone() {
        onBasePage.openPhotos()
        onPhotos.shouldOpenLastVisibleMediaItem()
        onPreview.shouldDeleteLocalFileFromPhone()
        onPreview.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onPreview.shouldCurrentPhotoBe(Images.SECOND)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("4295")
    @Category(Regression::class)
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.SECOND])
    fun shouldDeleteLastAlbumLocalPhotoFromPhone() {
        onBasePage.openAlbums()
        onAlbums.clickAlbum(DeviceFilesAndFolders.DCIM)
        onPhotos.shouldOpenLastVisibleMediaItem()
        onPreview.shouldDeleteLocalFileFromPhone()
        onPreview.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onPreview.shouldCurrentPhotoBe(Images.SECOND)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("4352")
    @Category(Regression::class)
    @PrepareServerLocalPhoto
    fun shouldDeleteServerLocalPhotoDuringAirplaneMode() {
        preparePhotosOperationsTest()
        onPhotos.clickItem(FilesAndFolders.FIRST)
        onPreview.switchToAirplaneMode()
        onPreview.shouldDeleteCurrentFileFromPhone()
        onPreview.shouldCurrentPhotoBe(Images.FIRST)
        onDiskApi.shouldFileExist(FilesAndFolders.FIRST)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
        onBasePage.switchToWifi()
    }
}
