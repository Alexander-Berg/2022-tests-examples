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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.ToastMessages

@Feature("Download files and folders")
@UserTags("download")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class DownloadTest2 : DownloadTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2835")
    @UploadFiles(filePaths = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.FILE_FOR_GO])
    @DeleteFiles(files = [FilesAndFolders.ORIGINAL_FILE, FilesAndFolders.ORIGINAL_TEXT_FILE, FilesAndFolders.FILE_FOR_GO])
    @Category(FullRegress::class)
    fun shouldKeepGroupOperationModeWhenDownloadWasCanceled() {
        onBasePage.openFiles()
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FILE)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.cancelDownLoading()
        onBasePage.shouldBeOnGroupMode()
        onBasePage.closeGroupMode()
        onFiles.shouldSelectFilesOrFolders(
            FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_TEXT_FILE,
            FilesAndFolders.FILE_FOR_GO
        )
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.cancelDownLoading()
        onBasePage.shouldBeOnGroupMode()
    }

    @Test
    @TmsLink("2446")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldSaveFileFromOfflineToDevice() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.PHOTO)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO,
            FilesAndFolders.PHOTO
        )
    }

    @Test
    @TmsLink("2447")
    @UploadFiles(
        filePaths = [FilesAndFolders.PHOTO, FilesAndFolders.ORIGINAL_FILE],
        targetFolder = FilesAndFolders.TARGET_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.TARGET_FOLDER])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH])
    @Category(FullRegress::class)
    fun shouldSaveFolderFromOfflineToDevice() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.TARGET_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.TARGET_FOLDER)
        onGroupMode.clickMoreOption()
        onGroupMode.applyAction(FileActions.DOWNLOAD)
        onFiles.pressSaveHereButton()
        onFiles.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.TARGET_FOLDER, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH)
        onAdb.shouldFileExistInFolderOnDevice(
            FilesAndFolders.ORIGINAL_FILE,
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH
        )
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH + FilesAndFolders.PHOTO,
            FilesAndFolders.PHOTO
        )
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.TARGET_FOLDER_FULL_PATH + FilesAndFolders.ORIGINAL_FILE,
            FilesAndFolders.ORIGINAL_FILE
        )
    }

    @Test
    @TmsLink("2732")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldDownloadPhotoToDeviceFromPreviewOnFiles() {
        onBasePage.openFiles()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onPreview.downloadCurrentImage()
        onFiles.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO,
            FilesAndFolders.PHOTO
        )
    }

    @Test
    @TmsLink("2724")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldDownloadPhotoToDeviceFromPreviewOnFeed() {
        onBasePage.openFeed()
        onFeed.openFirstImage()
        onPreview.downloadCurrentImage()
        onFiles.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO,
            FilesAndFolders.PHOTO
        )
    }

    @Test
    @TmsLink("2735")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldDownloadPhotoToDeviceFromPreviewOnAllPhotos() {
        onBasePage.openPhotos()
        onAllPhotos.shouldOpenFirstMediaItem()
        onPreview.saveOnDeviceCurrentImage()
        onFiles.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH)
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH + FilesAndFolders.PHOTO,
            FilesAndFolders.PHOTO
        )
    }

    @Test
    @TmsLink("2736")
    @UploadFiles(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFiles(files = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO])
    @Category(FullRegress::class)
    fun shouldDownloadPhotoToDeviceFromPreviewOnOffline() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.PHOTO)
        onBasePage.openOffline()
        onOffline.openFileIntoViewer(FilesAndFolders.PHOTO)
        onPreview.downloadCurrentImage()
        onFiles.shouldSeeToastContainedText(ToastMessages.ALL_FILES_SAVED_TO_TOAST)
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.PHOTO, DeviceFilesAndFolders.STORAGE_ROOT)
        onAdb.shouldFileOnDeviceBeEqualToTestFile(
            DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.PHOTO,
            FilesAndFolders.PHOTO
        )
    }
}
