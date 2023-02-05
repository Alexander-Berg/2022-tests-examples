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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Photos operations")
@UserTags("photosOperations")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotoOperationsTest : PhotosOperationsRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("4007")
    @Category(BusinessLogic::class)
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST])
    fun shouldLocalPhotoViewerOptionsBePresented() {
        preparePhotosOperationsTest()
        onBasePage.switchToAirplaneMode()
        onBasePage.shouldPushMediaToDevice(FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH)
        onBasePage.wait(5, TimeUnit.SECONDS) //wait for sync
        onPhotos.clickItem(FilesAndFolders.FIRST)
        onPreview.openOptionsMenu()
        onPreview.shouldLocalPhotoOptionsBePresented()
        onBasePage.switchToWifi()
    }

    @Test
    @TmsLink("4009")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldMissedLocalPhotoViewerOptionsBePresented() {
        preparePhotosOperationsTest()
        prepareLocalItem(FilesAndFolders.FIRST)
        onPhotos.clickItem(FilesAndFolders.FIRST)
        onPreview.openOptionsMenu()
        onPreview.shouldMissedLocalPhotoOptionsBePresented()
    }

    @Test
    @TmsLink("4970")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldServerPhotoOptionsBePresented() {
        preparePhotosOperationsTest()
        prepareServerItem(FilesAndFolders.FIRST)
        onPhotos.selectNPhoto(1)
        onPhotos.openOptionsMenu()
        onPhotos.shouldServerPhotoOptionsBePresented()
    }

    @Test
    @TmsLink("4971")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldServerLocalPhotoOptionsBePresented() {
        preparePhotosOperationsTest()
        onPhotos.selectItems(FilesAndFolders.FIRST)
        onPhotos.openOptionsMenu()
        onPhotos.shouldServerLocalPhotoOptionsBePresented()
    }

    @Test
    @TmsLink("4972")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldMissedLocalPhotoOptionsBePresented() {
        preparePhotosOperationsTest()
        prepareLocalItem(FilesAndFolders.FIRST)
        onPhotos.selectItems(FilesAndFolders.FIRST)
        onPhotos.openOptionsMenu()
        onPhotos.shouldMissedLocalPhotoOptionsBePresented()
    }

    @Test
    @TmsLink("5784")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldServerFeedPhotoViewerOptionsBePresented() {
        onBasePage.openFeed()
        onFeed.openFirstImage()
        onPreview.openOptionsMenu()
        onPreview.shouldServerFeedPhotoOptionsBePresented()
    }
}
