package ru.yandex.autotests.mobile.disk.android.runtimepermissions

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
import ru.yandex.autotests.mobile.disk.android.core.driver.AndroidVersion
import ru.yandex.autotests.mobile.disk.android.core.driver.DeviceType
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.IgnoreRule.ForbiddenVersions
import ru.yandex.autotests.mobile.disk.android.rules.IgnoreRule.OnlyDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.DoNotGrantPermissionsAutomatically
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.*
import java.util.concurrent.TimeUnit

@Feature("Runtime Permissions")
@UserTags("permissions")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
@DoNotGrantPermissionsAutomatically
class RuntimePermissionsTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onAdb: AdbSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onPermissions: PermissionsSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Test
    @TmsLink("2491")
    @ForbiddenVersions(AndroidVersion._5_1)
    @OnlyDevice(DeviceType.PHONE)
    @Category(FullRegress::class)
    fun shouldNotPermissionBeGrantedAfterDeclineWhenAddFromDevice() {
        onFiles.openAddFileDialog()
        onPermissions.shouldDeclinePermissionRequest()
        onPermissions.shouldDownloadPermissonsSnackbarBeDisplayed(PermissionSnackbarMessages.STORAGE_READ_FOR_UPLOAD)
        onAdb.shouldNotPermissionBeGranted(Permissions.READ_EXTERNAL_STORAGE)
    }

    @Test
    @TmsLink("2493")
    @ForbiddenVersions(AndroidVersion._5_1)
    @OnlyDevice(DeviceType.PHONE)
    @Category(FullRegress::class)
    fun shouldNotPermissionBeGrantedAfterDeclineWhenAEnableAutoupload() {
        onBasePage.openSettings()
        onSettings.openAutouploadSettings()
        onSettings.enablePhotoAutoupload()
        onPermissions.shouldDeclinePermissionRequest()
        onPermissions.shouldAutouploadPermissonsSnackbarBeDisplayed(PermissionSnackbarMessages.ALLOW_ACCESS_IN_SETTINGS)
        onAdb.shouldNotPermissionBeGranted(Permissions.READ_EXTERNAL_STORAGE)
    }

    @Test
    @TmsLink("2494")
    @ForbiddenVersions(AndroidVersion._5_1)
    @OnlyDevice(DeviceType.PHONE)
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @Category(FullRegress::class)
    fun shouldNotPermissionBeGrantedAfterDeclineWhenDownloadFromFiles() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onPermissions.shouldDeclinePermissionRequest()
        onAdb.shouldNotPermissionBeGranted(Permissions.WRITE_EXTERNAL_STORAGE, Permissions.READ_EXTERNAL_STORAGE)
    }

    @Test
    @TmsLink("2692")
    @ForbiddenVersions(AndroidVersion._5_1)
    @OnlyDevice(DeviceType.PHONE)
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @Category(FullRegress::class)
    fun shouldNotPermissionBeGrantedAfterDeclinedWhenDownloadFromOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onBasePage.openOffline()
        onOffline.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onPermissions.shouldDeclinePermissionRequest()
        onAdb.shouldNotPermissionBeGranted(Permissions.WRITE_EXTERNAL_STORAGE, Permissions.READ_EXTERNAL_STORAGE)
    }

    @Test
    @TmsLink("2902")
    @ForbiddenVersions(AndroidVersion._5_1)
    @OnlyDevice(DeviceType.PHONE)
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.DOWNLOAD_FULL_PATH + FilesAndFolders.ORIGINAL_TEXT_FILE])
    @Category(FullRegress::class)
    fun shouldGrantPermissionWhenGrantedOnDownloadFromFilesOperation() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onPermissions.shouldAcceptPermissionRequest()
        onFiles.openFolder(DeviceFilesAndFolders.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.waitDownloadingAlertClosed(10)
        onAdb.shouldPermissionBeGranted(Permissions.WRITE_EXTERNAL_STORAGE, Permissions.READ_EXTERNAL_STORAGE)
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            DeviceFilesAndFolders.DOWNLOAD_FULL_PATH
        )
    }

    @Test
    @TmsLink("2904")
    @ForbiddenVersions(AndroidVersion._5_1)
    @OnlyDevice(DeviceType.PHONE)
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.DOWNLOAD_FULL_PATH + FilesAndFolders.ORIGINAL_TEXT_FILE])
    @Category(FullRegress::class)
    fun shouldGrantPermissionWhenGrantedOnDownloadFromOfflineOperation() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onBasePage.openOffline()
        onOffline.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onPermissions.shouldAcceptPermissionRequest()
        onFiles.openFolder(DeviceFilesAndFolders.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.waitDownloadingAlertClosed(10)
        onAdb.shouldPermissionBeGranted(Permissions.WRITE_EXTERNAL_STORAGE, Permissions.READ_EXTERNAL_STORAGE)
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            DeviceFilesAndFolders.DOWNLOAD_FULL_PATH
        )
    }

    @Test
    @TmsLink("2288")
    @ForbiddenVersions(AndroidVersion._5_1)
    @OnlyDevice(DeviceType.PHONE)
    @PushFileToDevice(filePaths = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_TEXT_FILE])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.ORIGINAL_TEXT_FILE])
    @Category(FullRegress::class)
    fun shouldGrantPermissionWhenUploadFileFromDevice() {
        onBasePage.openFiles()
        onFiles.openAddFileDialog()
        onPermissions.shouldAcceptPermissionRequest()
        onFiles.shouldBeOnUploadToDiskDialog()
        onGroupMode.selectFilesOrFoldersOnGroupMode(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onGroupMode.shouldFilesOrFolderBeSelected(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onFiles.pressUploadToDisk()
        onBasePage.shouldBeOnFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_TEXT_FILE)
        onAdb.shouldPermissionBeGranted(Permissions.READ_EXTERNAL_STORAGE)
    }

    @Test
    @TmsLink("2307")
    @ForbiddenVersions(AndroidVersion._5_1)
    @OnlyDevice(DeviceType.PHONE)
    @Category(FullRegress::class)
    fun shouldSeeAccessToFileOverAppSettingsAlertAfterDeclinePermissionWithCheckedNeverAskAgainCheckbox() {
        onBasePage.openFiles()
        onFiles.openAddFileDialog()
        onPermissions.shouldDeclinePermissionRequest()
        onPermissions.swipeOnSnackbar()
        onPermissions.wait(1, TimeUnit.SECONDS) //wait after swipe
        onFiles.openAddFileDialog()
        onPermissions.enableNeverAskAgainCheckbox()
        onPermissions.shouldDeclinePermissionRequest()
        onPermissions.swipeOnSnackbar()
        onPermissions.wait(1, TimeUnit.SECONDS) //wait after swipe
        onFiles.openAddFileDialog()
        onPermissions.shouldSeeAccessToFilesAlert()
        onPermissions.approveOpeningApplicationSettings()
        onPermissions.shouldBeOnApplicationDeviceSettings()
    }
}
