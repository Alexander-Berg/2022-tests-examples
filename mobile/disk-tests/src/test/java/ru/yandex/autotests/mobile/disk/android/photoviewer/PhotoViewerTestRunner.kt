package ru.yandex.autotests.mobile.disk.android.photoviewer

import com.google.inject.Inject
import ru.yandex.autotests.mobile.disk.android.props.AndroidConfig
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

open class PhotoViewerTestRunner {
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.RUNTIME)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST])
    @DeleteFiles(files = [FilesAndFolders.FIRST])
    internal annotation class SingleCloudFile

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onPhotos: PhotosSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onMobile: CommonsSteps

    @Inject
    lateinit var onFeed: FeedSteps

    @Inject
    lateinit var onContentBlock: ContentBlockSteps

    @Inject
    lateinit var onOffline: OfflineSteps

    @Inject
    lateinit var onAdb: AdbSteps

    @Inject
    lateinit var config: AndroidConfig

    protected fun shouldControlsHideAndDisplayByPreviewTap() {
        onPreview.shouldSeePreviewControls()
        onPreview.tapOnPhotosPreview()
        onPreview.closeBannerIfPresented()
        onPreview.shouldNotSeePreviewControls()
        onPreview.tapOnPhotosPreview()
        onPreview.shouldSeePreviewControls()
    }

    protected fun shouldSaveOnDeviceFileFromPreview() {
        onPreview.saveOnDeviceCurrentImage()
        onPreview.wait(10, TimeUnit.SECONDS)
        val folder =
            if (config.browserVersion() == AndroidConfig.ANDROID_8) DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH else DeviceFilesAndFolders.YANDEX_DISK_FULL_PATH_11
        onAdb.shouldFileExistInFolderOnDevice(FilesAndFolders.FIRST, folder)
    }

    protected fun openFileFromFiles(filename: String) {
        onBasePage.openFiles()
        onBasePage.shouldBeOnFiles()
        onFiles.openImage(filename)
    }

    protected fun openFirstImageFromFeed() {
        onBasePage.openFeed()
        onFeed.openFirstImage()
        onPreview.shouldBeOnPreview()
    }

    protected fun shouldPhotoViewerBeRestoredFromAppSwitcher() {
        onPreview.sendApplicationToBackground()
        onPreview.openAppSwitcher()
        onPreview.wait(1, TimeUnit.SECONDS)
        onPreview.openAppSwitcher() //если тапнуть два раза, то открывается последнее приложение
        onPreview.shouldBeOnPreview()
    }
}
