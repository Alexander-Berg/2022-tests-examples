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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Photos delete operations")
@UserTags("photosDeleteOperations")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotosDeleteOperationsTest2 : PhotosOperationsRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("3987")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldDeleteLocalViewerPhotoFromPhone() {
        preparePhotosOperationsTest()
        prepareLocalItem(FilesAndFolders.FIRST)
        onPhotos.shouldOpenFirstMediaItem()
        onPreview.shouldDeleteLocalFileFromPhone()
        onPreview.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onBasePage.shouldBeOnPhotos()
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("3988")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldDeleteServerViewerPhotoFromServer() {
        preparePhotosOperationsTest()
        prepareServerItem(FilesAndFolders.FIRST)
        onPhotos.shouldOpenFirstMediaItem()
        onPreview.shouldDeleteServerFileFromDisk()
        onPreview.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onDiskApi.shouldFileNotExist(FilesAndFolders.FIRST)
    }

    @Test
    @TmsLink("3989")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldDeleteServerLocalViewerPhotoFromServer() {
        preparePhotosOperationsTest()
        onPhotos.shouldOpenFirstMediaItem()
        onPreview.shouldDeleteServerLocalFileFromDisk()
        onPreview.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onPreview.shouldBeOnPreview()
        onDiskApi.shouldFileNotExist(FilesAndFolders.FIRST)
    }

    @Test
    @TmsLink("3990")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldDeleteServerLocalViewerPhotoFromPhone() {
        preparePhotosOperationsTest()
        onPhotos.shouldOpenFirstMediaItem()
        onPreview.shouldDeleteCurrentFileFromPhone()
        onPreview.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onPreview.shouldBeOnPreview()
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("3974")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldDeleteServerLocalPhotoFromServerAndPhone() {
        preparePhotosOperationsTest()
        onPhotos.selectItems(FilesAndFolders.FIRST)
        onPhotos.shouldDeleteCheckedPhotosFromServerAndPhone()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onDiskApi.shouldFileNotExist(FilesAndFolders.FIRST)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("3993")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhotos
    fun shouldDeleteServerLocalPhotosFromServerAndPhone() {
        preparePhotosOperationsTest()
        onPhotos.selectItems(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onPhotos.shouldDeleteCheckedPhotosFromServerAndPhone()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onDiskApi.shouldFileNotExist(FilesAndFolders.FIRST)
        onDiskApi.shouldFileNotExist(FilesAndFolders.SECOND)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.SECOND, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }
}
