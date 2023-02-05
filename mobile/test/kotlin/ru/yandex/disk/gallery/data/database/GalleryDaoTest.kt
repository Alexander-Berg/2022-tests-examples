package ru.yandex.disk.gallery.data.database

import androidx.room.Room
import org.junit.Test
import ru.yandex.disk.gallery.utils.TestGalleryDatabase
import ru.yandex.disk.domain.albums.BucketAlbumId
import ru.yandex.disk.domain.albums.InnerAlbumId
import ru.yandex.disk.provider.DH
import ru.yandex.disk.test.AndroidTestCase2

class GalleryDaoTest : AndroidTestCase2() {

    lateinit var galleryDao: GalleryDao

    override fun setUp() {
        super.setUp()
        galleryDao =
            Room.databaseBuilder(mContext, TestGalleryDatabase::class.java, "0_disk")
                .allowMainThreadQueries()
                .build()
                .galleryDao()
    }

    @Test
    fun `should select albums by batches`() {
        val albumIds = mutableSetOf<InnerAlbumId>()
        for (i in 0..10000) {
            albumIds.add(BucketAlbumId(i.toString()))
        }
        galleryDao.queryOverlappingAlbumsHeaders(albumIds, 0, 10)
    }
}
