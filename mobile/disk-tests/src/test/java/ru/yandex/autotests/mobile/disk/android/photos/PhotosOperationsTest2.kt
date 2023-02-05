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

@Feature("Photos operations")
@UserTags("photosOperations")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotosOperationsTest2 : PhotosOperationsRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("4976")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhotos
    fun shouldServerLocalPhotosOptionsBePresented() {
        preparePhotosOperationsTest()
        onPhotos.selectNPhoto(2)
        onPhotos.openOptionsMenu()
        onPhotos.shouldServerLocalPhotosOptionsBePresented()
    }

    @Test
    @TmsLink("4977")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhotos
    fun shouldServerAndServerLocalPhotosOptionsBePresented() {
        preparePhotosOperationsTest()
        prepareServerItem(FilesAndFolders.FIRST)
        onPhotos.selectNPhoto(2)
        onPhotos.openOptionsMenu()
        onPhotos.shouldServerAndServerLocalPhotosOptionsBePresented()
    }

    @Test
    @TmsLink("4978")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldLocalAndServerLocalPhotosOptionsBePresented() {
        preparePhotosOperationsTest()
        onBasePage.switchToAirplaneMode()
        onBasePage.shouldPushMediaToDevice(FilesAndFolders.SECOND, DeviceFilesAndFolders.DCIM_FULL_PATH)
        onBasePage.wait(5, TimeUnit.SECONDS) //wait for sync
        onPhotos.selectItems(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onPhotos.openOptionsMenu()
        onPhotos.shouldLocalAndServerLocalPhotosOptionsBePresented()
        onBasePage.switchToWifi()
    }

    @Test
    @TmsLink("4979")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhotos
    fun shouldServerLocalAndMissedLocalPhotosOptionsBePresented() {
        preparePhotosOperationsTest()
        prepareLocalItem(FilesAndFolders.FIRST)
        onPhotos.selectItems(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onPhotos.openOptionsMenu()
        onPhotos.shouldLocalAndServerLocalPhotosOptionsBePresented()
    }

    @Test
    @TmsLink("4980")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhotos
    fun shouldLocalAndServerPhotosOptionsBePresented() {
        preparePhotosOperationsTest()
        prepareServerItem(FilesAndFolders.FIRST)
        prepareLocalItem(FilesAndFolders.SECOND)
        onPhotos.selectNPhoto(2)
        onPhotos.openOptionsMenu()
        onPhotos.shouldLocalAndServerPhotosOptionsBePresented()
    }

    @Test
    @TmsLink("4362")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldMissedLocalPhotoDeleteDialogOptionsBePresented() {
        preparePhotosOperationsTest()
        prepareLocalItem(FilesAndFolders.FIRST)
        onPhotos.selectItems(FilesAndFolders.FIRST)
        onPhotos.shouldClickDeleteButton()
        onPhotos.shouldLocalDeleteOptionsBePresented()
        onPhotos.pressHardBack()
        onPhotos.shouldGroupModeBeActivated()
        onPhotos.openOptionsMenu()
        onPhotos.shouldLocalDeleteOptionNotBePresented()
    }
}
