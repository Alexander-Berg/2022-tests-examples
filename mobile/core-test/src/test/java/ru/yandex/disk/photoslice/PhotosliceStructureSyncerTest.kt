@file:Suppress("IllegalIdentifier")

package ru.yandex.disk.photoslice

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.robolectric.annotation.Config
import ru.yandex.disk.DiskItem
import ru.yandex.disk.ServerDiskItem
import ru.yandex.disk.offline.operations.registry.PendingOperationsRegistry
import ru.yandex.disk.photoslice.PhotosliceTestHelper.newMomentBuilder
import ru.yandex.disk.provider.DiskContract
import ru.yandex.disk.provider.DiskDatabase
import ru.yandex.disk.provider.DiskItemBuilder
import ru.yandex.disk.remote.PhotosliceApi
import ru.yandex.disk.remote.PhotosliceTag
import ru.yandex.disk.remote.RemoteRepo
import ru.yandex.disk.remote.RemoteRepoOnNext
import ru.yandex.disk.remote.exceptions.PermanentException
import ru.yandex.disk.remote.exceptions.RemoteExecutionException
import ru.yandex.disk.settings.markers.AlbumsSettings
import ru.yandex.disk.sync.PhotosliceSyncStateManager
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.DiskMatchers.anyListOnNext
import ru.yandex.disk.test.DiskMatchers.provide
import ru.yandex.disk.test.DiskMatchers.provideUnit
import ru.yandex.disk.test.DiskMatchers.returnAndProvide
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.util.ReflectionUtils
import ru.yandex.disk.util.SystemClock
import ru.yandex.util.Path.asPath

private const val MOCK_MOMENT_ID = "momentId"

@Config(manifest = Config.NONE)
class PhotosliceStructureSyncerTest : AndroidTestCase2() {

    private val mockRemoteRepo = mock<RemoteRepo>()
    private val mockCallback = mock<PhotosliceStructureSyncer.Callback>()
    private val mockSycnstateManager = mock<PhotosliceSyncStateManager>()
    private val pendingOperationsRegistry = mock<PendingOperationsRegistry>()

    private lateinit var momentsDatabase: MomentsDatabase
    private lateinit var syncer: PhotosliceStructureSyncer
    private lateinit var preferences: SharedPreferences
    private lateinit var diskDatabase: DiskDatabase

    public override fun setUp() {
        super.setUp()
        val context = mockContext
        val dbOpenHelper = TestObjectsFactory.createSqlite(context)
        diskDatabase = TestObjectsFactory.createDiskDatabase(dbOpenHelper)
        momentsDatabase = TestObjectsFactory.createMomentsDatabase(dbOpenHelper,
            PreferenceManager.getDefaultSharedPreferences(context))

        preferences = context.getSharedPreferences("test", Context.MODE_PRIVATE)
        syncer = createSyncer()
    }

    private fun createSyncer(): PhotosliceStructureSyncer {
        val albumsMock: AlbumsSettings = mock {
            on { getAlbumsReadiness() } doReturn AlbumsSettings.Readiness.READY
        }
        val metadataFetcherFactory = object : MetadataFetcherFactory {
            override fun create(callback: MetadataFetcher.Callback): MetadataFetcher {
                return MetadataFetcher(diskDatabase, mockRemoteRepo, callback)
            }

        }
        return PhotosliceStructureSyncer(momentsDatabase, mockRemoteRepo, metadataFetcherFactory,
            pendingOperationsRegistry, mockSycnstateManager, SystemClock.REAL, albumsMock, mock(), mockCallback)
    }

    @Test
    fun shouldSyncAllMoments() {
        val photosliceTag = newPhotosliceTag()
        whenever(mockRemoteRepo.initPhotoslice()).thenReturn(photosliceTag)
        doAnswer(provide<Any, List<PhotosliceApi.Moment>>(listOf(createMoment())))
            .whenever(mockRemoteRepo).listMoments(eq(photosliceTag),
                anyListOnNext())

        syncer.syncStructure()

        val moments = momentsDatabase.queryReadyMoments()
        assertThat(moments.count, equalTo(1))
    }

    @Test
    fun shouldSyncMomentItems() {
        val photosliceTag = newPhotosliceTag()
        whenever(mockRemoteRepo.initPhotoslice()).thenReturn(photosliceTag)

        mockSingleItemMeta(photosliceTag)

        syncer.syncStructure()

        val momentItems = momentsDatabase.queryMomentItemMappings()
        assertThat(momentItems.count, equalTo(1))

        val diskItemA = diskDatabase.queryFileByPath(asPath("/disk/abc"))
        assertThat(diskItemA.moveToFirst(), equalTo(true))
        val columnIndex = diskItemA.getColumnIndex(DiskContract.DiskFile.ROW_TYPE)
        assertThat(diskItemA.isNull(columnIndex), equalTo(true))
    }

    @Test
    fun shouldStorePhotosliceTag() {
        val incoming = newPhotosliceTag()
        whenever(mockRemoteRepo.initPhotoslice()).thenReturn(incoming)
        doAnswer(provide<Any, List<PhotosliceApi.Moment>>(listOf(createMoment())))
            .whenever(mockRemoteRepo).listMoments<RuntimeException>(eq(incoming),
                anyListOnNext())

        syncer.syncStructure()

        assertThat(momentsDatabase.photosliceTag, equalTo(incoming))
    }

    @Test
    fun shouldApplyAllDeltas() {
        val builder = newMomentBuilder()
        momentsDatabase.insertOrReplace(builder.setSyncId("1").build())
        momentsDatabase.insertOrReplace(builder.setSyncId("2").build())
        momentsDatabase.insertOrReplace(builder.setSyncId("3").build())
        momentsDatabase.setSyncSuccessful()
        momentsDatabase.endSync()

        momentsDatabase.photosliceTag = PhotosliceTag("0", "0")

        doAnswer(
            returnAndProvide(DeltasUpdated(PhotosliceTag("0", "1"), false),
                IndexChange.createDeleteChange("1"), IndexChange.createDeleteChange("3"))
        ).whenever(mockRemoteRepo).getPhotosliceChanges(anyPhotosliceTag(), anyOnNextChange())

        syncer.syncStructure()

        assertThat(momentsDatabase.queryReadyMoments().get(0).syncId, equalTo("2"))
    }

    @Test
    fun shouldUpdateTagAfterDeltaApplication() {
        val photosliceTag = newPhotosliceTag()
        momentsDatabase.photosliceTag = photosliceTag

        val newPhotosliceTag = PhotosliceTag("1", "3")
        doAnswer(returnAndProvide(DeltasUpdated(newPhotosliceTag, false)))
            .whenever(mockRemoteRepo)
            .getPhotosliceChanges(eq(photosliceTag), anyOnNextChange())

        syncer.syncStructure()

        assertThat(momentsDatabase.photosliceTag, equalTo(newPhotosliceTag))
    }

    @Test
    fun shouldFetchNewMomentItemMetadataOnDeltaSync() {
        val photosliceTag = newPhotosliceTag()
        momentsDatabase.photosliceTag = photosliceTag

        val newPhotosliceTag = PhotosliceTag("1", "3")
        doAnswer(
            returnAndProvide(DeltasUpdated(newPhotosliceTag, false),
                createInsert("moment", "id", "disk:/A.jpg"))
        ).whenever(mockRemoteRepo).getPhotosliceChanges(eq(photosliceTag), anyOnNextChange())

        whenever(mockRemoteRepo.getFileListWithMinimalMetadata(listOf("/disk/A.jpg")))
            .thenReturn(serverResponseList(DiskItemBuilder()
                .setPath("/disk/A.jpg")
                .setEtag("ETAG").build()))

        syncer.syncStructure()

        val diskItemA = diskDatabase.queryFileItem(asPath("/disk/A.jpg")!!)
        assertThat(diskItemA!!.eTag, equalTo("ETAG"))
    }

    @Test
    fun shouldFetchUpdatedMomentItemMetadataOnDeltaSync() {
        val photosliceTag = newPhotosliceTag()
        momentsDatabase.photosliceTag = photosliceTag
        momentsDatabase.insertOrReplace(newMomentBuilder().setSyncId("1").build())
        momentsDatabase.insertOrReplace("1", MomentItemMapping("1", "/disk/a"))

        val newPhotosliceTag = PhotosliceTag("1", "3")
        doAnswer(
            returnAndProvide(DeltasUpdated(newPhotosliceTag, false),
                createUpdate("1", "1", "disk:/b"))
        ).whenever(mockRemoteRepo).getPhotosliceChanges(eq(photosliceTag), anyOnNextChange())

        whenever(mockRemoteRepo.getFileListWithMinimalMetadata(listOf("/disk/b")))
            .thenReturn(serverResponseList(DiskItemBuilder()
                .setPath("/disk/b")
                .setEtag("ETAG").build()))

        syncer.syncStructure()

        val diskB = diskDatabase.queryFileItem(asPath("/disk/b")!!)
        assertThat(diskB!!.eTag, notNullValue())
    }

    @Test
    fun shouldInitIfGetDeltasThrowsPermanentException() {
        val photosliceTag = newPhotosliceTag()
        momentsDatabase.photosliceTag = photosliceTag

        doThrow(PermanentException("test"))
            .whenever(mockRemoteRepo)
            .getPhotosliceChanges(eq(photosliceTag), anyOnNextChange())

        val incoming = PhotosliceTag("recreated", "1")
        whenever(mockRemoteRepo.initPhotoslice()).thenReturn(incoming)
        doAnswer(provide<Any, List<PhotosliceApi.Moment>>(listOf(createMoment())))
            .whenever(mockRemoteRepo).listMoments<RuntimeException>(eq(incoming),
                anyListOnNext())

        syncer.syncStructure()

        assertThat(momentsDatabase.photosliceTag, equalTo(incoming))
    }

    @Test
    fun shouldClearMomentsDatabaseIfGetDeltasThrowsPermanentException() {
        val builder = newMomentBuilder()
        momentsDatabase.insertOrReplace(builder.setSyncId("1").build())

        val photosliceTag = newPhotosliceTag()
        momentsDatabase.photosliceTag = photosliceTag

        doThrow(PermanentException("test"))
            .whenever(mockRemoteRepo)
            .getPhotosliceChanges(eq(photosliceTag), anyOnNextChange())

        val incoming = PhotosliceTag("recreated", "1")
        whenever(mockRemoteRepo.initPhotoslice()).thenReturn(incoming)
        doAnswer(provide<Any, Any>())
            .whenever(mockRemoteRepo).listMoments(eq(incoming), anyListOnNext())

        syncer.syncStructure()

        assertThat(momentsDatabase.queryNotInitedSyncingMoments().count, equalTo(0))
    }

    @Test
    fun shouldIterateThroughDeltas() {
        val photosliceTag = newPhotosliceTag()
        momentsDatabase.photosliceTag = photosliceTag

        val paths = mutableListOf<List<String>>()
        var currentTag = photosliceTag

        val range = 1..4
        for (rev in range) {
            val result = DeltasUpdated(PhotosliceTag("1", "$rev"), rev != range.last)
            val change = createInsert("moment", "id_$rev", "disk:/$rev.JPG")

            doAnswer(returnAndProvide(result, change))
                .whenever(mockRemoteRepo)
                .getPhotosliceChanges(eq(currentTag), anyOnNextChange())

            paths.add(listOf(change.path))
            currentTag = result.newTag
        }

        val pathsCaptor = argumentCaptor<List<String>>()
        whenever(mockRemoteRepo.getFileListWithMinimalMetadata(pathsCaptor.capture())).thenReturn(listOf())

        assertThat(syncer.syncStructure(), equalTo(true))

        assertThat(pathsCaptor.allValues, equalTo<List<List<String>>>(paths))
        assertThat(momentsDatabase.photosliceTag, equalTo(currentTag))
    }

    @Test
    fun shouldProviderPreviousDataDuringDeltaSync() {
        momentsDatabase.insertOrReplace(newMomentBuilder().setSyncId("1").build())
        momentsDatabase.insertOrReplace("1", MomentItemMapping("1", "/disk/a"))
        momentsDatabase.setSyncSuccessful()
        momentsDatabase.endSync()

        val diskItemBuilder = DiskItemBuilder()
        diskDatabase.updateOrInsert(diskItemBuilder.setPath("/disk/a").build())
        diskDatabase.updateOrInsert(diskItemBuilder.setPath("/disk/b").build())

        val photosliceTag = newPhotosliceTag()
        momentsDatabase.photosliceTag = photosliceTag

        doAnswer(
            returnAndProvide(DeltasUpdated(photosliceTag, false),
                createInsert("1", "2", "disk:/b"))
        ).whenever(mockRemoteRepo).getPhotosliceChanges(eq(photosliceTag), anyOnNextChange())

        diskDatabase = spy(diskDatabase)
        doAnswer { invocation ->
            invocation.callRealMethod()
            assertThat(momentsDatabase.queryMomentItemMappings().count, equalTo(1))
            null
        }.whenever(diskDatabase).endTransaction()

        syncer = createSyncer()
        syncer.syncStructure()

        assertThat(momentsDatabase.queryMomentItemMappings().count, equalTo(2))
    }

    @Test
    fun shouldProvidePreviousDataDuringReinit() {
        momentsDatabase.setSyncSuccessful()
        momentsDatabase.endSync()
        val diskItemBuilder = DiskItemBuilder()
        diskDatabase.updateOrInsert(diskItemBuilder.setPath("/disk/a").build())

        val photosliceTag = newPhotosliceTag()
        whenever(mockRemoteRepo.initPhotoslice()).thenReturn(photosliceTag)
        doAnswer(provide<Any, List<PhotosliceApi.Moment>>(listOf(createMoment())))
            .whenever(mockRemoteRepo).listMoments(eq(photosliceTag),
                anyListOnNext())
        doAnswer(provide<Any, ItemChange>(createInsert(MOCK_MOMENT_ID, "abc", "disk:/a")))
            .whenever(mockRemoteRepo).listMomentItems(eq(photosliceTag),
                anyList(), anyOnNext())

        momentsDatabase = spy(momentsDatabase)
        doAnswer { invocation ->
            invocation.callRealMethod()
            assertThat(momentsDatabase.queryReadyMoments().count, equalTo(0))
            null
        }.whenever(momentsDatabase).endTransaction()

        syncer = createSyncer()
        syncer.syncStructure()
        syncer.onBatchLoaded(mapOf(MOCK_MOMENT_ID to 1))

        assertThat(momentsDatabase.queryReadyMoments().count, equalTo(1))
    }

    @Test(expected = PermanentException::class)
    fun shouldResetEtagIfSecondInitFailed() {
        val photosliceTag = newPhotosliceTag()
        momentsDatabase.photosliceTag = photosliceTag

        doThrow(PermanentException("test"))
            .whenever(mockRemoteRepo)
            .getPhotosliceChanges(eq(photosliceTag), anyOnNextChange())

        whenever(mockRemoteRepo.initPhotoslice()).thenThrow(PermanentException("test"))

        try {
            syncer.syncStructure()
        } finally {
            assertThat(momentsDatabase.photosliceTag, `is`(nullValue()))
        }
    }

    @Test
    fun shouldNotCorruptAfterSecondSync() {
        //MOBDISK-5915
        momentsDatabase.beginSync()

        momentsDatabase.insertOrReplace(newMomentBuilder().setSyncId("1").setItemsCount(1).build())
        momentsDatabase.insertOrReplace("1", MomentItemMapping("123", "/disk/a"))
        diskDatabase.updateOrInsert(DiskItemBuilder().setPath("/disk/a").build())

        momentsDatabase.setSyncSuccessful()
        momentsDatabase.endSync()

        sync()

        sync()

        val moments = momentsDatabase.queryReadyMoments()
        assertThat(moments.get(0).itemsCount, equalTo(1))
    }

    @Test
    fun `should save photoslice tag after save moments`() {
        val photosliceTag = newPhotosliceTag()
        whenever(mockRemoteRepo.initPhotoslice()).thenReturn(photosliceTag)
        mockSingleItemMeta(photosliceTag)

        syncer.syncStructure()

        verify(mockSycnstateManager).initSyncTag = photosliceTag
    }

    @Test
    fun `should not save photoslice tag if failed on fetch moments`() {
        val photosliceTag = newPhotosliceTag()
        whenever(mockRemoteRepo.initPhotoslice()).thenReturn(photosliceTag)
        doThrow(RemoteExecutionException("TEST"))
            .whenever(mockRemoteRepo)
            .listMoments(eq(photosliceTag), anyListOnNext())

        try {
            syncer.syncStructure()
        } catch (e: RemoteExecutionException) {
            // expected
        }
        verify(mockSycnstateManager).initSyncTag
        verify(mockSycnstateManager).hasReadySnapshot()
        verifyNoMoreInteractions(mockSycnstateManager)
    }

    @Test
    fun `should mark moment as loaded after load all it items`() {
        val photosliceTag = newPhotosliceTag()
        whenever(mockRemoteRepo.initPhotoslice()).thenReturn(photosliceTag)

        mockSingleItemMeta(photosliceTag)

        syncer.syncStructure()

        val moment = momentsDatabase.queryReadyMoments()[0]
        assertThat(moment.isInited, equalTo(true))
    }

    @Test
    fun `should not request moments if have init sync tag`() {
        val newPhotosliceTag = newPhotosliceTag()
        whenever(mockSycnstateManager.initSyncTag).thenReturn(newPhotosliceTag)

        whenever(mockRemoteRepo.initPhotoslice()).thenReturn(newPhotosliceTag)
        syncer.syncStructure()

        verify(mockRemoteRepo, never()).initPhotoslice()
        verify(mockRemoteRepo, never()).listMoments<RuntimeException>(anyPhotosliceTag(),
            anyListOnNext())
    }

    @Test
    fun `should not request already synched moments items`() {
        val photosliceTag = newPhotosliceTag()
        whenever(mockSycnstateManager.initSyncTag).thenReturn(photosliceTag)

        momentsDatabase.insertOrReplace(newMomentBuilder().setSyncId("1").setItemsCount(2).setIsInited(true).build())
        momentsDatabase.insertOrReplace(newMomentBuilder().setSyncId("2").setItemsCount(1).setIsInited(false).build())
        momentsDatabase.setSyncSuccessful()
        momentsDatabase.endSync()

        mockSingleItemMeta(photosliceTag)
        momentsDatabase.beginSync()
        syncer.syncStructure()

        verify(mockRemoteRepo, never()).initPhotoslice()
        verify(mockRemoteRepo, never()).listMoments<RuntimeException>(eq(photosliceTag), anyListOnNext())
        verify(mockRemoteRepo).listMomentItems(eq(photosliceTag), anyList(), anyOnNext<ItemChange>())
        verify(mockRemoteRepo).getFileListWithMinimalMetadata(anyList())
        verifyNoMoreInteractions(mockRemoteRepo)
    }

    @Test(expected = PermanentException::class)
    fun `should drop photoslice init tag on error in sync`() {
        val photosliceTag = newPhotosliceTag()
        whenever(mockSycnstateManager.initSyncTag).thenReturn(photosliceTag)
        whenever(mockRemoteRepo.initPhotoslice()).thenReturn(photosliceTag)

        momentsDatabase.beginSyncFromScratch()
        momentsDatabase.insertOrReplace(newMomentBuilder().setSyncId("1").setItemsCount(2).setIsInited(false).build())
        doThrow(PermanentException("TEST 404"))
            .whenever(mockRemoteRepo).listMomentItems(eq(photosliceTag), anyList(),
                anyOnNext())

        syncer.syncStructure()

        verify(mockSycnstateManager).initSyncTag = null
    }

    private fun mockSingleItemMeta(photosliceTag: PhotosliceTag) {
        whenever(mockRemoteRepo.listMoments(eq(photosliceTag), anyListOnNext()))
                .doAnswer(provideUnit(listOf(createMoment())))

        doAnswer(provide<Any, ItemChange>(createInsert(MOCK_MOMENT_ID, "abc", "disk:/abc")))
            .whenever(mockRemoteRepo).listMomentItems(eq(photosliceTag), anyList(), anyOnNext())

        whenever(mockRemoteRepo.getFileListWithMinimalMetadata(listOf("/disk/abc")))
            .thenReturn(serverResponseList(DiskItemBuilder()
                .setPath("/disk/abc")
                .setEtag("ETAG").build()))
    }

    private fun sync() {
        momentsDatabase.beginSync()
        momentsDatabase.setSyncSuccessful()
        momentsDatabase.endSync()
    }

    private fun anyOnNextChange() = anyOnNext<Change>()

    private fun anyPhotosliceTag() = any<PhotosliceTag>()

    private fun newPhotosliceTag() = PhotosliceTag("old", "old-revision")

    fun <T> anyOnNext() = any<RemoteRepoOnNext<T, RuntimeException>>()

    private fun createMoment(momentId: String = MOCK_MOMENT_ID): PhotosliceApi.Moment {
        return PhotosliceApi.Moment().also {
            ReflectionUtils.setField(it, "momentId", momentId)
            ReflectionUtils.setField(it, "itemsCount", 1)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun serverResponseList(vararg items: DiskItem): List<ServerDiskItem> {
        return listOf(*items) as List<ServerDiskItem>
    }

    private fun createInsert(momentId: String, itemId: String, path: String): ItemChange {
        return ItemChange.create(momentId, itemId, path, null, null, null, Change.ChangeType.INSERT)
    }

    private fun createUpdate(momentId: String, itemId: String, path: String): ItemChange {
        return ItemChange.create(momentId, itemId, path, null, null, null, Change.ChangeType.UPDATE)
    }
}
