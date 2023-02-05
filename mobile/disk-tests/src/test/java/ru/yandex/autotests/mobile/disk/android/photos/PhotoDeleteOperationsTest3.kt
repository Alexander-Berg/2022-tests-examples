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
import ru.yandex.autotests.mobile.disk.android.blocks.MediaItem
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
class PhotoDeleteOperationsTest3 : PhotosOperationsRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain : RuleChain

    @Test
    @TmsLink("3981")
    @Category(BusinessLogic::class)
    @PrepareMixedPhotos
    fun shouldDeleteMixedPhotosFromServerAndPhone() {
        preparePhotosOperationsTest()
        prepareMixedItems()
        onPhotos.selectNPhoto(3)
        onPhotos.shouldDeleteCheckedPhotosFromServerAndPhone()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onDiskApi.shouldFileNotExist(FilesAndFolders.FIRST)
        onDiskApi.shouldFileNotExist(FilesAndFolders.SECOND)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.THIRD, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("3982")
    @Category(BusinessLogic::class)
    @PrepareMixedPhotos
    fun shouldDeleteMixedPhotosFromServer() {
        preparePhotosOperationsTest()
        prepareMixedItems()
        onPhotos.selectNPhoto(3)
        onPhotos.shouldDeleteCheckedPhotosFromServer()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onDiskApi.shouldFileNotExist(FilesAndFolders.FIRST)
        onDiskApi.shouldFileNotExist(FilesAndFolders.SECOND)
        onPhotos.shouldDisplayItemWithStatus(FilesAndFolders.FIRST, MediaItem.Status.LOCAL)
        onPhotos.shouldDisplayItemWithStatus(FilesAndFolders.THIRD, MediaItem.Status.LOCAL)
    }

    @Test
    @TmsLink("3983")
    @Category(BusinessLogic::class)
    @PrepareMixedPhotos
    fun shouldDeleteMixedPhotosFromPhone() {
        preparePhotosOperationsTest()
        prepareMixedItems()
        onPhotos.selectNPhoto(3)
        onPhotos.shouldDeleteCheckedPhotosFromPhone()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
        onDiskApi.shouldFileExist(FilesAndFolders.FIRST)
        onDiskApi.shouldFileExist(FilesAndFolders.SECOND)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.THIRD, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }

    @Test
    @TmsLink("3986")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldDeleteServerLocalViewerPhotoFromServerAndPhone() {
        preparePhotosOperationsTest()
        onPhotos.shouldOpenFirstMediaItem()
        onPreview.shouldDeleteCurrentFileFromDiskAndDevice()
        onPreview.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS) //wait for delete
        onBasePage.shouldBeOnPhotos()
        onDiskApi.shouldFileNotExist(FilesAndFolders.FIRST)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
    }
}
