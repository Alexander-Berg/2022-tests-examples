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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Photos operations")
@UserTags("photosOperations")
@RunWith(GuiceTestRunner::class)
@GuiceModules(
    AndroidModule::class
)
class PhotosOperationsTest : PhotosOperationsRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("4361")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhotos
    fun shouldServerPhotoLocalDeleteDialogOptionsBePresented() {
        preparePhotosOperationsTest()
        onBasePage.openPhotos()
        onPhotos.selectItems(FilesAndFolders.FIRST)
        onPhotos.shouldClickDeleteButton()
        onPhotos.shouldServerLocalDeleteOptionsBePresented()
        onPhotos.pressHardBack()
        onPhotos.shouldGroupModeBeActivated()
        onPhotos.openOptionsMenu()
        onPhotos.shouldLocalDeleteOptionBePresented()
    }

    @Test
    @TmsLink("4360")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldServerPhotoDeleteDialogOptionsBePresented() {
        preparePhotosOperationsTest()
        prepareServerItem(FilesAndFolders.FIRST)
        onPhotos.selectNPhoto(1)
        onPhotos.shouldClickDeleteButton()
        onPhotos.shouldServerDeleteOptionsBePresented()
        onPhotos.pressHardBack()
        onPhotos.shouldGroupModeBeActivated()
        onPhotos.openOptionsMenu()
        onPhotos.shouldLocalDeleteOptionNotBePresented()
    }

    @Test
    @TmsLink("4481")
    @Category(Regression::class)
    @PushFileToDevice(filePaths = [FilesAndFolders.FIRST], targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH)
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST])
    fun shouldNotShareLinkForLocalFile() {
        onBasePage.openPhotos()
        onPhotos.selectItems(FilesAndFolders.FIRST)
        onPhotos.openOptionsMenu()
        onPhotos.shouldNotShareLinkOptionBePresented()
    }

    @Test
    @TmsLink("4364")
    @Category(BusinessLogic::class)
    @PushFileToDevice(filePaths = [FilesAndFolders.FIRST], targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH)
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST])
    fun shouldPhotoViewerControlsBeDisplayed() {
        onBasePage.openPhotos()
        onPhotos.clickItem(FilesAndFolders.FIRST)
        onPreview.shouldSeePreviewControls()
    }

    @Test
    @TmsLink("4973")
    @Category(BusinessLogic::class)
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.SECOND])
    fun shouldOptionsMenuBeDisabledForSyncingPhotos() {
        onBasePage.switchToAirplaneMode()
        onBasePage.enablePhotoAutoupload()
        onBasePage.openPhotos()
        onPhotos.shouldDisplayItemWithStatus(FilesAndFolders.FIRST, MediaItem.Status.UPLOADING)
        onPhotos.shouldDisplayItemWithStatus(FilesAndFolders.SECOND, MediaItem.Status.UPLOADING)
        onPhotos.selectItems(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onPhotos.shouldOptionsMenuBeDisabled()
        onBasePage.switchToWifi()
    }
}
