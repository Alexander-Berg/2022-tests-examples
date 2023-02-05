package ru.yandex.autotests.mobile.disk.android.feed

import com.google.inject.Inject
import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.core.util.WaiterUtil
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFilesByUploadType
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileSpec
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.android.steps.ContentBlockSteps.GridType
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.NameHolder
import java.util.concurrent.TimeUnit

open class FeedTestRunner {
    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onMobile: CommonsSteps

    @Inject
    lateinit var onFeed: FeedSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onContentBlock: ContentBlockSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var diskApiSteps: DiskApiSteps

    @Inject
    lateinit var nameHolder: NameHolder

    @Inject
    lateinit var onPush: PushSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onLogin: LoginSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.RUNTIME)
    @DeleteFiles(files = [IMAGE_BLOCK_DIR, VIDEO_BLOCK_DIR, TEXT_BLOCK_DIR, FilesAndFolders.CAMERA_UPLOADS])
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.DCIM_FULL_PATH])
    @DeleteFilesByUploadType(
        targetFolder = FilesAndFolders.PHOTOUNLUM_ROOT,
        fileSpecs = [UploadFileSpec(
            type = UploadFileType.IMAGE,
            count = 9,
            namePattern = "2014-10-29 12-00-0%d.JPG"
        ), UploadFileSpec(type = UploadFileType.VIDEO, count = 9, namePattern = "2014-10-29 13-00-0%d.MP4")]
    )
    annotation class CleanFeedFiles

    protected fun enableUnlimitedPhotoAutoupload() {
        onBasePage.openSettings()
        onSettings.openAutouploadSettings()
        onSettings.enableUnlimitedPhotoAutoupload()
        onMobile.pressHardBack()
        onSettings.closeSettings()
        onSettings.closeProfile()
    }

    protected fun enableUnlimitedVideoAutoupload() {
        onBasePage.openSettings()
        onSettings.openAutouploadSettings()
        onSettings.enableUnlimitedVideoAutoupload()
        onMobile.pressHardBack()
        onSettings.closeSettings()
        onSettings.closeProfile()
    }

    protected fun enableLimitedPhotoAndVideoAutoupload() {
        onBasePage.openSettings()
        onSettings.openAutouploadSettings()
        onSettings.enableLimitedPhotoAndVideoAutoupload()
        onMobile.pressHardBack()
        onSettings.closeSettings()
        onSettings.closeProfile()
    }

    protected fun shouldDeleteFirstBlockItemFromViewer(gridType: GridType) {
        onContentBlock.shouldOpenFirstItem(gridType)
        onPreview.shouldDeleteCurrentFileFromDisk()
        WaiterUtil.wait(1, TimeUnit.SECONDS)
        onPreview.pressHardBack()
    }

    companion object {
        const val IMAGE_BLOCK_DIR = "image_block_dir"
        const val VIDEO_BLOCK_DIR = "video_block_dir"
        const val TEXT_BLOCK_DIR = "text_block_dir"
    }
}
