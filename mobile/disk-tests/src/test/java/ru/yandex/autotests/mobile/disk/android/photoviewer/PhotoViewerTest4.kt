package ru.yandex.autotests.mobile.disk.android.photoviewer

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
import org.openqa.selenium.ScreenOrientation
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.props.AndroidConfig
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CreateFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images
import java.util.concurrent.TimeUnit

@Feature("Photo viewer")
@UserTags("photoviewer")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class PhotoViewerTest4 : PhotoViewerTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("5803")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.STORAGE_ROOT + FilesAndFolders.FIRST, DeviceFilesAndFolders.DOWNLOAD_FULL_PATH + FilesAndFolders.FIRST])
    fun shouldDownloadFileFromFeedPreview() {
        openFirstImageFromFeed()
        onPreview.downloadCurrentImage()
        onPreview.wait(5, TimeUnit.SECONDS)
        when (config.browserVersion()) {
            AndroidConfig.ANDROID_8 -> onAdb.shouldFileExistInFolderOnDevice(
                FilesAndFolders.FIRST,
                DeviceFilesAndFolders.STORAGE_ROOT
            )
            AndroidConfig.ANDROID_11 -> onAdb.shouldFileExistInFolderOnDevice(
                FilesAndFolders.FIRST,
                DeviceFilesAndFolders.DOWNLOAD_FULL_PATH
            )
        }
    }

    @Test
    @TmsLink("5804")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH, DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH_11])
    fun shouldSaveOnDeviceFileFromFeedPreview() {
        openFirstImageFromFeed()
        shouldSaveOnDeviceFileFromPreview()
    }

    @Test
    @TmsLink("6076")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH, DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH_11])
    fun shouldSaveOnDeviceFileFromFilePreview() {
        openFileFromFiles(FilesAndFolders.FIRST)
        shouldSaveOnDeviceFileFromPreview()
    }

    @Test
    @TmsLink("6048")
    @Category(BusinessLogic::class)
    @UploadFiles(filePaths = [FilesAndFolders.ONE_HOUR_VIDEO])
    @DeleteFiles(files = [FilesAndFolders.ONE_HOUR_VIDEO])
    fun shouldVideoPreviewControllersHideAndDisplay() {
        onBasePage.openFiles()
        onFiles.switchToListLayout()
        onFiles.shouldOpenVideoIntoViewer(FilesAndFolders.ONE_HOUR_VIDEO)
        onPreview.shouldSeePreviewControls()
        onPreview.shouldClickPlayVideoButton()
        onPreview.wait(10, TimeUnit.SECONDS) //wait for buffering, playing, animations
        onPreview.closeBannerIfPresented()
        onPreview.shouldNotSeePreviewControls()
        onPreview.shouldVideoControllersNotBeDisplayed()
        onPreview.tapOnVideoPlayer()
        onPreview.shouldSeePreviewControls(true)
        onPreview.shouldVideoControllersBeDisplayed()
    }

    @Test
    @TmsLink("5811")
    @Category(BusinessLogic::class)
    @SingleCloudFile
    fun shouldRemoveFileFromOfflineFromFeedViewer() {
        onBasePage.openFiles()
        onFiles.addFilesOrFoldersToOffline(FilesAndFolders.FIRST)
        openFirstImageFromFeed()
        onPreview.deleteCurrentFileFromOffline()
        onPreview.shouldBeOnPreview()
        onBasePage.pressHardBack()
        onBasePage.openOffline()
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FIRST)
    }

    @Test
    @TmsLink("6024")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(BusinessLogic::class)
    fun shouldOpenNextPreviewAfterRightSwipe() {
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldCurrentImageBe(Images.FIRST)
        onPreview.swipeRightToLeft()
        onPreview.shouldCurrentImageBe(Images.SECOND)
    }

    @Test
    @TmsLink("6931")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldOpenInfopanelWithSwipe() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.openImage(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldBeOnPreview()
        onPreview.wait(5, TimeUnit.SECONDS)
        onPreview.swipeDownToUpNTimes(1)
        onPreview.shouldPhotoHasName(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldPhotoHasClickablePath(FilesAndFolders.UPLOAD_FOLDER)
    }

    @Test
    @TmsLink("6933")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldOpenInfopanelWithSwipeInLandscape() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.openImage(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldBeOnPreview()
        onPreview.wait(3, TimeUnit.SECONDS)
        onPreview.rotate(ScreenOrientation.LANDSCAPE)
        onPreview.wait(2, TimeUnit.SECONDS)
        onPreview.swipeDownToUpNTimes(1)
        onPreview.shouldPhotoHasName(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldPhotoHasClickablePath(FilesAndFolders.UPLOAD_FOLDER)
    }

    @Test
    @TmsLink("6069")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldDeletePhotoFromViewer() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.openImage(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldBeOnPreview()
        onPreview.shouldDeleteCurrentFileFromDisk()
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FILE_FOR_VIEWING_1)
    }

    @Test
    @TmsLink("6068")
    @UploadFiles(
        filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2],
        targetFolder = FilesAndFolders.UPLOAD_FOLDER
    )
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldSeeSecondImageInViewerAfterDeletingTheFirstOne() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.openImage(FilesAndFolders.FILE_FOR_VIEWING_2)
        onPreview.shouldBeOnPreview()
        onPreview.shouldDeleteCurrentFileFromDisk()
        onPreview.wait(2, TimeUnit.SECONDS)
        onPreview.openPhotoInformation()
        onPreview.shouldPhotoHasName(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.tapOnPhotosPreview()
        onPreview.closePreview()
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldNotExistFilesOrFolders(FilesAndFolders.FILE_FOR_VIEWING_2)
    }

    @Test
    @TmsLink("6071")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldCancelDeletionAfterTapOnViewer() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.openImage(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldBeOnPreview()
        onPreview.clickDeleteButton()
        onPreview.shouldDeleteFromDiskOptionDisplayed()
        onPreview.wait(2, TimeUnit.SECONDS)
        onPreview.tapAt(100, 100)
        onPreview.wait(1, TimeUnit.SECONDS)
        onPreview.shouldBeOnPreview()
    }

    @Test
    @TmsLink("6072")
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1], targetFolder = FilesAndFolders.UPLOAD_FOLDER)
    @CreateFolders(folders = [FilesAndFolders.UPLOAD_FOLDER])
    @DeleteFiles(files = [FilesAndFolders.UPLOAD_FOLDER])
    @Category(Regression::class)
    fun shouldCancelDeletionAfterSwipe() {
        onBasePage.openFiles()
        onFiles.openFolder(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.shouldFolderBeOpened(FilesAndFolders.UPLOAD_FOLDER)
        onFiles.openImage(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPreview.shouldBeOnPreview()
        onPreview.clickDeleteButton()
        onPreview.shouldDeleteFromDiskOptionDisplayed()
        onPreview.wait(2, TimeUnit.SECONDS)
        onPreview.swipeUpToDownNTimes(1)
        onPreview.wait(1, TimeUnit.SECONDS)
        onPreview.shouldBeOnPreview()
    }
}
