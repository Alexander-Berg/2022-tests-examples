package ru.yandex.disk.gallery.utils

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.yandex.disk.gallery.data.database.*
import ru.yandex.disk.provider.DH

@Database(entities = [
    Header::class, MediaItemModel::class,
    MediaDownload::class, MediaHashes::class, DownloadedPreview::class],
    version = DH.DATABASE_VERSION,
    exportSchema = false)
@TypeConverters(value = [AlbumIdConverter::class, AlbumSetConverter::class, StringListConverter::class])
abstract class TestGalleryDatabase : RoomDatabase() {

    abstract fun galleryDao(): GalleryDao

    abstract fun rawQueriesDao(): RawQueriesDao

    abstract fun previewsDao(): PreviewsDaoInternal
}
