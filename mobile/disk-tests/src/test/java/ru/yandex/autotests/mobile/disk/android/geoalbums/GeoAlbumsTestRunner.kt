package ru.yandex.autotests.mobile.disk.android.geoalbums

import com.google.inject.Inject
import com.google.inject.name.Named
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

open class GeoAlbumsTestRunner {
    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onAlbumsPage: GeoAlbumsSteps

    @Inject
    lateinit var onPhotosPage: PhotosSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onShare: ShareMenuSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var onDiskApi: DiskApiSteps

    @Inject
    lateinit var onAdb: AdbSteps

    protected fun shouldAutoUploadMedia(localName: String, uploadedName: String, unlimited: Boolean) {
        onBasePage.openAlbums()
        onAlbumsPage.shouldDisplayGeoMetaAlbum()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.shouldDisplayAnyItemsButNot(uploadedName)
        onBasePage.openAlbums()
        onBasePage.openAlbums()
        onBasePage.openSettings()
        onSettings.openAutouploadSettings()
        if (unlimited) {
            onSettings.enableUnlimitedPhotoAndVideoAutoupload()
        } else {
            onSettings.enableLimitedPhotoAndVideoAutoupload()
        }
        onBasePage.pressHardBackNTimes(3)
        onAdb.pushMediaToDeviceDCIM(localName)
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.sendPushesUntilAppeared(uploadedName)
    }

    protected fun shouldUploadMediaToDisk(localName: String) {
        onBasePage.openAlbums()
        onAlbumsPage.shouldDisplayGeoMetaAlbum()
        onAlbumsPage.openGeoAlbumWithDailyLayout()
        onAlbumsPage.shouldDisplayAnyItemsButNot(localName)
        onDiskApi.uploadFileToFolder(localName, FilesAndFolders.GEO_WORK_FOLDER)
        onAlbumsPage.sendPushesUntilAppeared(localName)
    }
}
