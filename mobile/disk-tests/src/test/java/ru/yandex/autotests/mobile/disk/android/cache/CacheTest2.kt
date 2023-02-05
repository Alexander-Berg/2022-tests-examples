package ru.yandex.autotests.mobile.disk.android.cache

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.Issue
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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages
import java.util.concurrent.TimeUnit

@Feature("Cache")
@UserTags("cache")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class CacheTest2 : CacheTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @Issue("MOBDISK-13155")
    @TmsLink("2074")
    @UploadFiles(
        filePaths = [FilesAndFolders.PNG_IMAGE, FilesAndFolders.PHOTO],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldDownloadFolderContentWhenAllFilesWasCachedOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PNG_IMAGE)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack()
        onFiles.navigateUp()
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.approveDownLoading()
        onFiles.wait(5, TimeUnit.SECONDS) //explicit wait for moving files
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.TARGET_FOLDER, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PNG_IMAGE, DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH)
    }

    @Test
    @TmsLink("2070")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_1, DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_2])
    @Category(FullRegress::class)
    fun shouldDownloadTwoFilesFromCacheOnAirplaneMode() {
        onBasePage.openFiles()
        //load file into cache
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack() //Close editor
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_2)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack() //Close editor
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.approveDownLoading()
        onFiles.wait(5, TimeUnit.SECONDS) //explicit wait for moving files
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FILE_FOR_VIEWING_1, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FILE_FOR_VIEWING_2, DeviceFilesAndFolders.STORAGE_ROOT)
    }

    @Test
    @TmsLink("2941")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_1, DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_2])
    @Category(FullRegress::class)
    fun shouldDownloadTwoFilesFromOfflineOnAirplaneMode() {
        onBasePage.openFiles()
        //load file into cache
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.waitDownloadComplete()
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.approveDownLoading()
        onFiles.wait(5, TimeUnit.SECONDS) //explicit wait for moving files
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FILE_FOR_VIEWING_1, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FILE_FOR_VIEWING_2, DeviceFilesAndFolders.STORAGE_ROOT)
    }

    @Test
    @TmsLink("2071")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_1, DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_2])
    @Category(FullRegress::class)
    fun shouldDownloadTwoFilesFromCacheAndOfflineOnAirplaneMode() {
        onBasePage.openFiles()
        //load file into cache
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack() //Close editor
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.waitDownloadComplete()
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.approveDownLoading()
        onFiles.wait(5, TimeUnit.SECONDS) //explicit wait for moving files
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FILE_FOR_VIEWING_1, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FILE_FOR_VIEWING_2, DeviceFilesAndFolders.STORAGE_ROOT)
    }

    @Test
    @TmsLink("2072")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldNotDownloadNotCachedFileOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onFiles.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.PHOTO)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldSeeToastWithMessage(ToastMessages.CAN_DOWNLOAD_ONLY_CACHED_TOAST)
    }
}
