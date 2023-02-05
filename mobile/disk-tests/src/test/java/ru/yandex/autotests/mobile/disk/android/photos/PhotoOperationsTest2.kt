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
import ru.yandex.autotests.mobile.disk.android.core.api.data.shared.Rights
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadToSharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Photos operations")
@UserTags("photosOperations")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotoOperationsTest2 : PhotosOperationsRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("6098")
    @Category(BusinessLogic::class)
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @UploadToSharedFolder(filePaths = [FilesAndFolders.PNG_IMAGE])
    fun shouldROFolderPhotosViewerOptionsBePresented() {
        val readOnlyDir = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(readOnlyDir)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PNG_IMAGE)
        onPreview.openOptionsMenu()
        onPreview.shouldROFolderPhotosViewerOptionsBePresented()
    }

    @Test
    @TmsLink("4974")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldMissedLocalAndLocalPhotosOptionsBePresented() {
        preparePhotosOperationsTest()
        prepareLocalItem(FilesAndFolders.FIRST)
        onBasePage.switchToAirplaneMode()
        onBasePage.shouldPushMediaToDevice(FilesAndFolders.SECOND, DeviceFilesAndFolders.DCIM_FULL_PATH)
        onBasePage.wait(5, TimeUnit.SECONDS) //wait for sync
        onPhotos.selectItems(FilesAndFolders.FIRST, FilesAndFolders.SECOND)
        onPhotos.openOptionsMenu()
        onPhotos.shouldMissedLocalAndLocalPhotosOptionsBePresented()
        onBasePage.switchToWifi()
    }

    @Test
    @TmsLink("4975")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhotos
    fun shouldServerPhotosOptionsBePresented() {
        preparePhotosOperationsTest()
        prepareServerItem(FilesAndFolders.FIRST)
        prepareServerItem(FilesAndFolders.SECOND)
        onPhotos.selectNPhoto(2)
        onPhotos.openOptionsMenu()
        onPhotos.shouldServerPhotosOptionsBePresented()
    }

    @Test
    @TmsLink("4006")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldServerPhotoViewerOptionsBePresented() {
        preparePhotosOperationsTest()
        prepareServerItem(FilesAndFolders.FIRST)
        onPhotos.shouldOpenFirstMediaItem()
        onPreview.openOptionsMenu()
        onPreview.shouldServerPhotoOptionsBePresented()
    }

    @Test
    @TmsLink("4008")
    @Category(BusinessLogic::class)
    @PrepareServerLocalPhoto
    fun shouldServerLocalPhotoViewerOptionsBePresented() {
        preparePhotosOperationsTest()
        onPhotos.clickItem(FilesAndFolders.FIRST)
        onPreview.openOptionsMenu()
        onPreview.shouldServerLocalPhotoOptionsBePresented()
    }
}
