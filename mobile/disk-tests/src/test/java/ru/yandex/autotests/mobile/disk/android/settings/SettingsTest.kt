package ru.yandex.autotests.mobile.disk.android.settings

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import com.google.inject.name.Named
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.core.accounts.Account
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.CreateFoldersOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.LoginSteps
import ru.yandex.autotests.mobile.disk.android.steps.SettingsSteps
import ru.yandex.autotests.mobile.disk.android.steps.SettingsSteps.PhotosCache
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Settings")
@UserTags("settings")
@AuthorizationTest
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class SettingsTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var account: Account

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onLogin: LoginSteps

    @Test
    @TmsLink("4033")
    @Category(FullRegress::class)
    fun shouldSaveCacheSizeAfterRelogin() {
        onBasePage.openSettings()
        changePhotosCacheSizeAndCheckAfterLogin(PhotosCache.MB_300)
        changePhotosCacheSizeAndCheckAfterLogin(PhotosCache.MB_100)
        changePhotosCacheSizeAndCheckAfterLogin(PhotosCache.DO_NOT_CACHE)
    }

    private fun changePhotosCacheSizeAndCheckAfterLogin(value: PhotosCache) {
        onSettings.shouldSelectPhotosCache(value)
        onSettings.shouldPhotosCacheHasSize(value)
        onSettings.closeSettings()
        onSettings.logoutOnProfilePage()
        onLogin.shouldAutologinToAccount(account.login)
        onBasePage.openSettings()
        onSettings.shouldPhotosCacheHasSize(value)
    }

    @Test
    @TmsLink("4917")
    @Category(BusinessLogic::class)
    @CreateFoldersOnDevice(
        folders = [
            DeviceFilesAndFolders.CYMERA_FULL_PATH,
            DeviceFilesAndFolders.PICTURES_FULL_PATH,
            DeviceFilesAndFolders.IMAGES_FULL_PATH,
            DeviceFilesAndFolders.VIDEOS_FULL_PATH,
            DeviceFilesAndFolders.PICTURES_SUBFOLDER_FULL_PATH,
        ]
    )
    @PushFileToDevice(filePaths = [FilesAndFolders.JPEG1], targetFolder = DeviceFilesAndFolders.CYMERA_FULL_PATH)
    @PushFileToDevice(filePaths = [FilesAndFolders.JPEG2], targetFolder = DeviceFilesAndFolders.PICTURES_FULL_PATH)
    @PushFileToDevice(filePaths = [FilesAndFolders.JPEG3], targetFolder = DeviceFilesAndFolders.IMAGES_FULL_PATH)
    @PushFileToDevice(filePaths = [FilesAndFolders.JPEG4], targetFolder = DeviceFilesAndFolders.VIDEOS_FULL_PATH)
    @PushFileToDevice(
        filePaths = [FilesAndFolders.JPEG4],
        targetFolder = DeviceFilesAndFolders.PICTURES_SUBFOLDER_FULL_PATH
    )
    @DeleteFilesOnDevice(
        filesPath = [
            DeviceFilesAndFolders.CYMERA_FULL_PATH,
            DeviceFilesAndFolders.PICTURES_FULL_PATH,
            DeviceFilesAndFolders.IMAGES_FULL_PATH,
            DeviceFilesAndFolders.VIDEOS_FULL_PATH,
        ]
    )
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    fun shouldFoldersBeAvailableForAutoupload() {
        onBasePage.openSettings()
        onSettings.openAutouploadSettings()
        onSettings.enableLimitedPhotoAndVideoAutoupload()
        onSettings.shouldOpenAutouploadFolders()
        onSettings.shouldAutouploadFolderBeDisplayed(DeviceFilesAndFolders.CYMERA)
        onSettings.shouldAutouploadFolderBeDisplayed(DeviceFilesAndFolders.PICTURES)
        onSettings.shouldAutouploadFolderBeDisplayed(DeviceFilesAndFolders.IMAGES)
        onSettings.shouldAutouploadFolderBeDisplayed(DeviceFilesAndFolders.VIDEOS)
        onSettings.shouldAutouploadFolderBeDisplayed(DeviceFilesAndFolders.SUBFOLDER)
    }
}
