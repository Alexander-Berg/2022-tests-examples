package ru.yandex.autotests.mobile.disk.android.download

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.CreateFoldersOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages
import java.util.concurrent.TimeUnit

@Feature("Download files and folders")
@UserTags("download")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class DownloadTest : DownloadTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1283")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.ORIGINAL_FILE])
    @Category(Regression::class)
    fun shouldDownloadFileToDevice() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.ORIGINAL_FILE, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_FILE
        )
    }

    @Test
    @TmsLink("2373")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE], targetFolder = FilesAndFolders.TARGET_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.TARGET_FOLDER])
    @Category(Regression::class)
    fun shouldDownloadFolderToDevice() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.TARGET_FOLDER, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_FILE,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH + FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_FILE
        )
    }

    @Test
    @TmsLink("1286")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldDownloadEmptyFolder() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.TARGET_FOLDER, DeviceFilesAndFolders.STORAGE_ROOT)
    }

    @Test
    @TmsLink("1287")
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER, FilesAndFolders.TARGET_FOLDER + "/" + FilesAndFolders.ORIGINAL_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldDownloadFolderContaingEmptyFolder() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.TARGET_FOLDER, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_FOLDER,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
    }

    @Test
    @TmsLink("2067")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldDownloadFileFromCacheOnAirplaneMode() {
        onBasePage.openFiles()
        //load file into cache
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack() //Close editor
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.PHOTO)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.approveDownLoading()
        onFiles.wait(5, TimeUnit.SECONDS) //explicit wait for moving files
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.STORAGE_ROOT)
    }

    @Test
    @TmsLink("2881")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO, FilesAndFolders.ORIGINAL_FILE])
    @DeleteFiles(files = [FilesAndFolders.PHOTO, FilesAndFolders.ORIGINAL_FILE])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO, DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.ORIGINAL_FILE])
    @Category(FullRegress::class)
    fun shouldDownloadOnlyCachedFileOnAirplaneMode() {
        onBasePage.openFiles()
        //load file into cache
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onPreview.openEditor()
        onPreview.shouldBeOnEditor()
        onPreview.pressHardBack() //Close editor
        onMobile.switchToAirplaneMode()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.PHOTO, FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldDownloadWithoutNetworkAlertBePresented()
        onFiles.approveDownLoading()
        onFiles.wait(5, TimeUnit.SECONDS) //explicit wait for moving files
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.ORIGINAL_FILE, DeviceFilesAndFolders.STORAGE_ROOT)
    }

    @Test
    @TmsLink("2068")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldNotDownloadNonCachedFileOnAirplaneMode() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.PHOTO)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onMobile.switchToAirplaneMode()
        onFiles.pressSaveHereButton()
        onMobile.shouldSeeToastWithMessage(ToastMessages.CAN_SAVE_ONLY_CACHED_FILES_TOAST)
        onAdb.shouldFileNotExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.STORAGE_ROOT)
        onFiles.shouldFileManagerBeNotPresented()
    }

    @Test
    @TmsLink("2876")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @PushFileToDevice(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldReplaceAlreadyExistedFileOverDownload() {
        onBasePage.openFiles()
        onDiskApi.updateFile(
            FilesAndFolders.PHOTO,
            FilesAndFolders.DISK_ROOT,
            FilesAndFolders.FILE_FOR_VIEWING_1
        ) //replace current content of file
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.PHOTO)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldNameConflictAlertBePresented(DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO)
        onFiles.approveReplacing()
        onFiles.waitDownloadingAlertClosed(10)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO,
            FilesAndFolders.FILE_FOR_VIEWING_1
        )
    }

    @Test
    @TmsLink("2884")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @CreateFoldersOnDevice(folders = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO_WITHOUT_EXTENSION])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO_WITHOUT_EXTENSION])
    @Category(FullRegress::class)
    fun shouldNotReplaceExistedFolderWhenDownloadFileWithSameNameExceptExtension() {
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.PHOTO)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.PHOTO)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.waitDownloadingAlertClosed(10)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.PHOTO_WITHOUT_EXTENSION,
            DeviceFilesAndFolders.STORAGE_ROOT
        )
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO,
            FilesAndFolders.PHOTO
        )
    }

    @Test
    @TmsLink("2882")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @PushFileToDevice(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_1, DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_2])
    @Category(FullRegress::class)
    fun shouldReplaceSeveralAlreadyExistedFileOverDownload() {
        onBasePage.openFiles()
        //replace current content of file
        onDiskApi.updateFile(FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.DISK_ROOT, FilesAndFolders.PHOTO)
        onDiskApi.updateFile(FilesAndFolders.FILE_FOR_VIEWING_2, FilesAndFolders.DISK_ROOT, FilesAndFolders.PHOTO)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldNameConflictAlertBePresented(DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_1)
        onFiles.pressApplyToAll()
        onFiles.approveReplacing()
        onFiles.waitDownloadingAlertClosed(10)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FILE_FOR_VIEWING_1, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FILE_FOR_VIEWING_2, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_1,
            FilesAndFolders.PHOTO
        )
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FILE_FOR_VIEWING_2,
            FilesAndFolders.PHOTO
        )
    }
}
