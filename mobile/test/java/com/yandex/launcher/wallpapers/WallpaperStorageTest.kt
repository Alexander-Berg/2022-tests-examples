package com.yandex.launcher.wallpapers

import android.content.Context
import android.content.SharedPreferences
import com.yandex.launcher.common.util.TextUtils
import com.yandex.launcher.BaseRobolectricTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assume
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val WALLPAPER_ID = "wallpaper_id"
private const val WALLPAPER_URL = "wallpaper_url"
private const val COLLECTION_ID = "collection_id"
private const val COLLECTION_TITLE = "collection_title"
private const val FILE_PATH = "collection_title"

private const val FULL_FILLED_METADATA = 0
private const val METADATA_NULL_FILE_PATH = 1
private const val METADATA_EMPTY_FILE_PATH = 2
private const val METADATA_EMPTY_WALLPAPER_URL = 3
private const val METADATA_EMPTY_COLLECTION_ID = 4
private const val METADATA_EMPTY_COLLECTION_TITLE = 5

class WallpaperStorageTest: BaseRobolectricTest() {

    private val bytes = ByteArray(10) { it.toByte() }

    private val wallpaperMetadata = arrayOf(
        WallpaperMetadata(WALLPAPER_ID, WALLPAPER_URL, COLLECTION_ID, COLLECTION_TITLE, FILE_PATH),
        WallpaperMetadata(WALLPAPER_ID, WALLPAPER_URL, COLLECTION_ID, COLLECTION_TITLE, null),
        WallpaperMetadata(WALLPAPER_ID, WALLPAPER_URL, COLLECTION_ID, COLLECTION_TITLE, TextUtils.EMPTY),
        WallpaperMetadata(WALLPAPER_ID, TextUtils.EMPTY, COLLECTION_ID, COLLECTION_TITLE, null),
        WallpaperMetadata(WALLPAPER_ID, WALLPAPER_URL, TextUtils.EMPTY, COLLECTION_TITLE, null),
        WallpaperMetadata(WALLPAPER_ID, WALLPAPER_URL, COLLECTION_ID, TextUtils.EMPTY, null)
    )

    private lateinit var prefs: SharedPreferences
    private lateinit var baseDir: File
    private lateinit var storage: WallpaperStorage

    override fun setUp() {
        super.setUp()

        prefs = appContext.getSharedPreferences(WallpaperStorage.WALLPAPER_STORAGE_PREF_NAME, Context.MODE_PRIVATE)
        baseDir = RuntimeEnvironment.application.applicationContext.filesDir
        WallpaperStorage.overriddenBaseDir = baseDir
        storage = WallpaperStorage(appContext)
    }

    @Test
    fun `check all test metadata are at correct index`() {
        val fillFilledMetadata = wallpaperMetadata[FULL_FILLED_METADATA]
        assertThat(fillFilledMetadata.wallpaperId.isEmpty(), equalTo(false))
        assertThat(fillFilledMetadata.wallpaperUrl.isEmpty(), equalTo(false))
        assertThat(fillFilledMetadata.collectionId.isEmpty(), equalTo(false))
        assertThat(fillFilledMetadata.collectionTitle.isEmpty(), equalTo(false))
        assertThat(fillFilledMetadata.filePath.isNullOrEmpty(), equalTo(false))

        val metadataNullFileName = wallpaperMetadata[METADATA_NULL_FILE_PATH]
        assertThat(metadataNullFileName.wallpaperId.isEmpty(), equalTo(false))
        assertThat(metadataNullFileName.wallpaperUrl.isEmpty(), equalTo(false))
        assertThat(metadataNullFileName.collectionId.isEmpty(), equalTo(false))
        assertThat(metadataNullFileName.collectionTitle.isEmpty(), equalTo(false))
        assertThat(metadataNullFileName.filePath, nullValue())

        val metadataEmptyFileName = wallpaperMetadata[METADATA_EMPTY_FILE_PATH]
        assertThat(metadataEmptyFileName.wallpaperId.isEmpty(), equalTo(false))
        assertThat(metadataEmptyFileName.wallpaperUrl.isEmpty(), equalTo(false))
        assertThat(metadataEmptyFileName.collectionId.isEmpty(), equalTo(false))
        assertThat(metadataEmptyFileName.collectionTitle.isEmpty(), equalTo(false))
        assertThat(metadataEmptyFileName.filePath.isNullOrEmpty(), equalTo(true))

        val metadataEmptyWallpaperUrl = wallpaperMetadata[METADATA_EMPTY_WALLPAPER_URL]
        assertThat(metadataEmptyWallpaperUrl.wallpaperId.isEmpty(), equalTo(false))
        assertThat(metadataEmptyWallpaperUrl.wallpaperUrl.isEmpty(), equalTo(true))
        assertThat(metadataEmptyWallpaperUrl.collectionId.isEmpty(), equalTo(false))
        assertThat(metadataEmptyWallpaperUrl.collectionTitle.isEmpty(), equalTo(false))

        val metadataEmptyCollectionId = wallpaperMetadata[METADATA_EMPTY_COLLECTION_ID]
        assertThat(metadataEmptyCollectionId.wallpaperId.isEmpty(), equalTo(false))
        assertThat(metadataEmptyCollectionId.wallpaperUrl.isEmpty(), equalTo(false))
        assertThat(metadataEmptyCollectionId.collectionId.isEmpty(), equalTo(true))
        assertThat(metadataEmptyCollectionId.collectionTitle.isEmpty(), equalTo(false))

        val metadataEmptyCollectionTitle = wallpaperMetadata[METADATA_EMPTY_COLLECTION_TITLE]
        assertThat(metadataEmptyCollectionTitle.wallpaperId.isEmpty(), equalTo(false))
        assertThat(metadataEmptyCollectionTitle.wallpaperUrl.isEmpty(), equalTo(false))
        assertThat(metadataEmptyCollectionTitle.collectionId.isEmpty(), equalTo(false))
        assertThat(metadataEmptyCollectionTitle.collectionTitle.isEmpty(), equalTo(true))
    }

    @Test
    fun `write metadata to prefs, all required fields set, metadata written`() {
        WallpaperStorage.writeToPrefs(prefs, wallpaperMetadata[FULL_FILLED_METADATA])

        val metadata = WallpaperStorage.readFromPrefs(prefs, wallpaperMetadata[FULL_FILLED_METADATA].wallpaperId)

        assertThat(metadata, notNullValue())
    }

    @Test
    fun `write metadata to prefs, all required fields set, all metadata fields are equal to origin fields`() {
        WallpaperStorage.writeToPrefs(prefs, wallpaperMetadata[FULL_FILLED_METADATA])

        val metadata = WallpaperStorage.readFromPrefs(prefs, wallpaperMetadata[FULL_FILLED_METADATA].wallpaperId)

        assertThat(metadata, equalTo(wallpaperMetadata[FULL_FILLED_METADATA]))
    }

    @Test
    fun `write metadata to prefs, wallpaper url is empty, metadata not written`() {
        WallpaperStorage.writeToPrefs(prefs, wallpaperMetadata[METADATA_EMPTY_WALLPAPER_URL])

        val metadata = WallpaperStorage.readFromPrefs(prefs, wallpaperMetadata[METADATA_EMPTY_WALLPAPER_URL].wallpaperId)

        assertThat(metadata, nullValue())
    }

    @Test
    fun `write metadata to prefs, collection id is empty, metadata not written`() {
        WallpaperStorage.writeToPrefs(prefs, wallpaperMetadata[METADATA_EMPTY_COLLECTION_ID])

        val metadata = WallpaperStorage.readFromPrefs(prefs, wallpaperMetadata[METADATA_EMPTY_COLLECTION_ID].wallpaperId)

        assertThat(metadata, nullValue())
    }

    @Test
    fun `write metadata to prefs, collection title is empty, metadata not written`() {
        WallpaperStorage.writeToPrefs(prefs, wallpaperMetadata[METADATA_EMPTY_COLLECTION_TITLE])

        val metadata = WallpaperStorage.readFromPrefs(prefs, wallpaperMetadata[METADATA_EMPTY_COLLECTION_TITLE].wallpaperId)

        assertThat(metadata, nullValue())
    }

    @Test
    fun `check metadata valid, wallpaper url is empty, metadata not valid`() {
        assertThat(wallpaperMetadata[METADATA_EMPTY_WALLPAPER_URL].isValid(), equalTo(false))
    }

    @Test
    fun `check metadata valid, collection id is empty, metadata not valid`() {
        assertThat(wallpaperMetadata[METADATA_EMPTY_COLLECTION_ID].isValid(), equalTo(false))
    }

    @Test
    fun `check metadata valid, collection title is empty, metadata not valid`() {
        assertThat(wallpaperMetadata[METADATA_EMPTY_COLLECTION_TITLE].isValid(), equalTo(false))
    }

    @Test
    fun `write invalid metadata, file not created`() {
        val storage = WallpaperStorage(appContext)
        storage.put(wallpaperMetadata[METADATA_EMPTY_WALLPAPER_URL], getFakeInputStream())

        assertThat(storage[wallpaperMetadata[METADATA_EMPTY_WALLPAPER_URL].wallpaperId], nullValue())
    }

    @Test
    fun `write valid metadata, file created`() {
        val storage = WallpaperStorage(appContext)
        storage.put(wallpaperMetadata[FULL_FILLED_METADATA], getFakeInputStream())

        assertThat(storage[wallpaperMetadata[FULL_FILLED_METADATA].wallpaperId], notNullValue())
    }

    @Test
    fun `write valid metadata, file has correct content`() {
        storage.put(wallpaperMetadata[FULL_FILLED_METADATA], getFakeInputStream())

        val file: File = storage[wallpaperMetadata[FULL_FILLED_METADATA].wallpaperId]!!.file!!
        Assume.assumeThat(file, notNullValue())
        Assume.assumeThat(file.exists(), equalTo(true))

        assertThat(file.readBytes(), equalTo(bytes))
    }

    @Test
    fun `write valid metadata, name has alphabet symbols and numbers, file has the same name`() {
        val metadata = WallpaperMetadata("meta123", "url", "collection_id", "title", null)
        storage.put(metadata, getFakeInputStream())

        val file = storage[metadata.wallpaperId]!!.file!!

        assertThat(file.name, equalTo(metadata.wallpaperId))
    }

    @Test
    fun `write valid metadata, name has non alphabet symbols and numbers, file has the encoded name`() {
        val metadata = WallpaperMetadata("meta,/ :';123", "url", "collection_id", "title", null)
        storage.put(metadata, getFakeInputStream())

        val file = storage[metadata.wallpaperId]!!.file!!

        assertThat(file.name, equalTo(URLEncoder.encode(metadata.wallpaperId, StandardCharsets.UTF_8.name())))
    }

    @Test
    fun `write valid metadata, get metadata by id, the same metadata returned`() {
        val metadata = WallpaperMetadata("meta,/ :';123", "url", "collection_id", "title", null)
        storage.put(metadata, getFakeInputStream())

        val metadataByFileName =
            WallpaperStorage.getMetadataByFileName(prefs, storage[metadata.wallpaperId]!!.file!!)

        assertThat(metadataByFileName!! == storage[metadata.wallpaperId]!!, equalTo(true))
        assertThat(metadataByFileName.file, equalTo(storage[metadata.wallpaperId]!!.file!!))
    }

    @Test
    fun `write 4 valid metadata, 4 corresponding files created`() {
        val wallpaperIdList = create4MetaDataAndReturnIds()
        val files = getWallpaperFiles()
        files.sort()

        assertFileNamesAreEqualsToWallpaperIds(files, wallpaperIdList.toTypedArray())
    }

    private fun create4MetaDataAndReturnIds(): List<String> {
        var metadata = WallpaperMetadata("meta,/ :';1", "url", "collection_id", "title", null)
        storage.put(metadata, getFakeInputStream())
        metadata = WallpaperMetadata("meta,/ :';2", "url", "collection_id", "title", null)
        storage.put(metadata, getFakeInputStream())
        metadata = WallpaperMetadata("meta,/ :';3", "url", "collection_id", "title", null)
        storage.put(metadata, getFakeInputStream())
        metadata = WallpaperMetadata("meta,/ :';4", "url", "collection_id", "title", null)
        storage.put(metadata, getFakeInputStream())
        return arrayListOf(
            "meta,/ :';1".urlEncode(),
            "meta,/ :';2".urlEncode(),
            "meta,/ :';3".urlEncode(),
            "meta,/ :';4".urlEncode()
        )
    }

    private fun getFakeInputStream(): InputStream = ByteArrayInputStream(bytes)

    private fun WallpaperMetadata.isValid(): Boolean = WallpaperStorage.isMetaDataValid(
        this.wallpaperId,
        this.wallpaperUrl,
        this.collectionId,
        this.collectionTitle)

    private fun String.urlEncode() = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private fun getWallpaperFiles(): Array<File>  = baseDir.listFiles()

    private fun assertFileNamesAreEqualsToWallpaperIds(files: Array<File>, wallpaperIds: Array<String>) {
        for (i in wallpaperIds.indices) {
            assertThat(files[i].name, equalTo(wallpaperIds[i]))
        }
    }
}
