package ru.yandex.autotests.mobile.disk.android.geoalbums

import ru.yandex.autotests.mobile.disk.android.rules.annotations.albums.DeleteAlbums
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.CleanTrash
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.SavePublicResource
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteMediaOnDevice
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@DeleteFiles(
    files = [
        FilesAndFolders.GEO_WORK_FOLDER,
        FilesAndFolders.TARGET_FOLDER,
        FilesAndFolders.PHOTOUNLUM_ROOT + FilesAndFolders.GEO_PHOTO_AUTO_UPLOADED,
        FilesAndFolders.CAMERA_UPLOADS + "/" + FilesAndFolders.GEO_PHOTO_AUTO_UPLOADED,
        FilesAndFolders.PHOTOUNLUM_ROOT + FilesAndFolders.GEO_VIDEO_AUTO_UPLOADED,
        FilesAndFolders.CAMERA_UPLOADS + "/" + FilesAndFolders.GEO_VIDEO_AUTO_UPLOADED,
        FilesAndFolders.DOWNLOADS, FilesAndFolders.DOWNLOADS_RU
    ]
)
@SavePublicResource(url = FilesAndFolders.GEO_ZURICH_FOLDER_PUBLIC_URL)
@DeleteMediaOnDevice(
    files = [
        DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.GEO_CLOUDY_LOCAL_FILE,
        DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.GEO_PHOTO_TO_UPLOAD,
        DeviceFilesAndFolders.DCIM_FULL_PATH + FilesAndFolders.GEO_VIDEO_TO_UPLOAD
    ]
)
@CleanTrash
@DeleteAlbums
annotation class PrepareGeoAlbums
