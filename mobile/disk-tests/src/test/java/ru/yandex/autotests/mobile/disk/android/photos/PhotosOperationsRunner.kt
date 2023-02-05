package ru.yandex.autotests.mobile.disk.android.photos

import com.google.inject.Inject
import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.NameHolder
import java.util.concurrent.TimeUnit

abstract class PhotosOperationsRunner {
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.RUNTIME)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST])
    @PushFileToDevice(filePaths = [FilesAndFolders.FIRST], targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH)
    @DeleteFiles(files = [FilesAndFolders.FIRST])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.SECOND])
    annotation class PrepareServerLocalPhoto

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.RUNTIME)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.SECOND])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.SECOND])
    annotation class PrepareServerLocalPhotos

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.RUNTIME)
    @UploadFiles(filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD])
    @PushFileToDevice(
        filePaths = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD],
        targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH
    )
    @DeleteFiles(files = [FilesAndFolders.FIRST, FilesAndFolders.SECOND, FilesAndFolders.THIRD])
    @DeleteMediaOnDevice(files = [DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.FIRST, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.SECOND, DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.THIRD])
    annotation class PrepareMixedPhotos

    companion object {
        const val DELETE_WAITING_TIME_IN_SEC = 30
    }

    @Inject
    lateinit var onCommon: CommonsSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onPhotos: PhotosSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onDiskApi: DiskApiSteps

    @Inject
    lateinit var onAdb: AdbSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onAlbums: AlbumsSteps

    @Inject
    lateinit var onFeed: FeedSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var nameHolder: NameHolder
    fun preparePhotosOperationsTest() {
        onCommon.switchToWifi()
        onBasePage.enablePhotoAutoupload()
        onBasePage.openPhotos()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS) //wait for sync
        onPhotos.shouldAutouploadStatusBeNotDisplayed()
    }

    //FIRST = server-local
    //SECOND = server
    //THIRD = local
    fun prepareMixedItems() {
        prepareServerItem(FilesAndFolders.SECOND)
        prepareLocalItem(FilesAndFolders.THIRD)
    }

    fun prepareLocalItem(filename: String?) {
        onPhotos.selectItems(filename)
        onPhotos.shouldDeleteCheckedPhotosFromServer()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
    }

    fun prepareServerItem(filename: String?) {
        onPhotos.selectItems(filename)
        onPhotos.shouldDeleteCheckedPhotosFromPhone()
        onPhotos.wait(DELETE_WAITING_TIME_IN_SEC.toLong(), TimeUnit.SECONDS)
    }
}
