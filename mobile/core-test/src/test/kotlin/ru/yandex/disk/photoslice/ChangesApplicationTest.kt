package ru.yandex.disk.photoslice

import android.preference.PreferenceManager
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.photoslice.PhotosliceTestHelper.newMomentBuilder
import ru.yandex.disk.remote.BaseRemoteRepoMethodTest
import ru.yandex.disk.remote.Collector
import ru.yandex.disk.remote.DateFormat
import ru.yandex.disk.remote.PhotosliceTag
import ru.yandex.disk.remote.RemoteRepoTestHelper.load
import ru.yandex.disk.sql.SQLiteOpenHelper2
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.utils.CursorList
import java.util.Arrays.asList
import java.util.Collections.emptyList

@Config(manifest = Config.NONE)
class ChangesApplicationTest : BaseRemoteRepoMethodTest() {

    private lateinit var momentsDatabase: MomentsDatabase
    private lateinit var dbOpenHelper: SQLiteOpenHelper2

    @Before
    override fun setUp() {
        super.setUp()
        dbOpenHelper = TestObjectsFactory.createSqlite(mockContext)
        TestObjectsFactory.createDiskDatabase(dbOpenHelper)
        momentsDatabase = TestObjectsFactory.createMomentsDatabase(dbOpenHelper,
                PreferenceManager.getDefaultSharedPreferences(mockContext))
    }

    @Test
    fun shouldDeleteMoment() {
        val builder = newMomentBuilder()
        doInSync {
            momentsDatabase.insertOrReplace(builder
                    .setSyncId("0000001431958859000_0000001431958859000").build())
            momentsDatabase.insertOrReplace(builder.setSyncId("m2").build())
        }
        parseAndApply("ps_delta_moment_deleted.json", 1)

        val moments = momentsDatabase.queryReadyMoments()
        assertThat(moments.count, equalTo(1))
        assertThat(moments[0].syncId, equalTo("m2"))
    }

    @Test
    fun shouldDeleteMomentItem() {
        doInSync {
            momentsDatabase.insertOrReplace("0000001431958859000_0000001431958859000",
                MomentItemMapping("1_0000001431958859000_eaf69bd64b5691d6136fdab7" + "0bf4d278382cadb3ac926961a1fad31d5bc148eb", "/disk/a"))
            momentsDatabase.insertOrReplace("0000001431958859000_0000001431958859000",
                MomentItemMapping("item2", "/disk/b"))
            momentsDatabase.insertOrReplace("m2",
                MomentItemMapping("item1", "/disk/c"))
        }

        parseAndApply("ps_delta_moment_deleted.json", 0)

        val items = momentsDatabase.queryMomentItemMappings()
        assertThat(items.count, equalTo(2))
    }

    @Test
    fun shouldUpdateMomentItem() {
        doInSync {
            momentsDatabase.insertOrReplace("0000001431959389000_0000001431959389000",
                MomentItemMapping("1_0000001431959389000_5c3c17c52e55caa95cd24dfdcb10501e0c885" + "693b9a297ef2bd4e2eb96c58f42", "/disk/a"))
            momentsDatabase.insertOrReplace("0000001431959389000_0000001431959389000",
                MomentItemMapping("item2", "/disk/b"))
        }

        parseAndApply("ps_delta_update_moment_item.json", 0)

        val items = CursorList(momentsDatabase.queryMomentItemMappings())
        assertThat(items[0].path, equalTo("/disk/b"))
        assertThat(items[1].path, equalTo("/disk/IMG_20150518_172948.jpg"))
    }

    @Test
    fun shouldUpdateMomentItemsCount() {
        doInSync {
            val builder = newMomentBuilder().setItemsCount(10)
            momentsDatabase.insertOrReplace(builder
                    .setSyncId("0000001431959384000_0000001431959389000").build())
            momentsDatabase.insertOrReplace(builder.setSyncId("m2").build())
        }

        parseAndApply("ps_delta_moment_item_deleted.json", 1)

        val moments = momentsDatabase.queryReadyMoments()
        assertThat(moments[0].itemsCount, equalTo(10))
        assertThat(moments[1].itemsCount, equalTo(1))
    }

    @Test
    fun shouldNotUpdateMomentItemsCount() {
        doInSync {
            momentsDatabase.insertOrReplace(newMomentBuilder()
                    .setItemsCount(10)
                    .setSyncId("0000001431959389000_0000001431959389000").build())
        }

        parseAndApply("ps_delta_insert_moment_item.json", 2)

        val moments = momentsDatabase.queryReadyMoments()
        val moment = moments[0]
        assertThat(moment.itemsCount, equalTo(10))
    }

    @Test
    fun shouldInsertNewMomentItem() {
        parseAndApply("ps_delta_insert_moment_item.json", 0)

        val items = momentsDatabase.queryMomentItemMappings()
        assertThat(items.count, equalTo(1))

        val item = MomentItemMapping(
            "1_0000001431959389000_5c3c17c52e55caa95c" + "d24dfdcb10501e0c885693b9a297ef2bd4e2eb96c58f42",
            "/disk/IMG_20150518_172948.jpg")
        items.moveToFirst()
        assertThat(items.getString(items.getColumnIndex(MomentItemMappingColumns.MOMENT_ID)),
            equalTo("0000001431959389000_0000001431959389000"))
        items.moveToFirst()
        assertThat(items.makeItemForRow(), equalTo(item))
    }

    @Test
    fun shouldInsertNewMoment() {
        parseAndApply("ps_delta_insert_moment_item.json", 1)

        val items = momentsDatabase.queryReadyMoments()
        assertThat(items.count, equalTo(1))
        assertThat(Moment.Builder.copyOf(items[0]), equalTo(Moment.Builder.newBuilder()
                .setSyncId("0000001431959389000_0000001431959389000")
                .setToDate(DateFormat.asLong("2015-05-18T14:29:49+00:00"))
                .setItemsCount(1)
                .setFromDate(DateFormat.asLong("2015-05-18T14:29:49+00:00"))
                .setLocalityEn("Moscow")
                .setLocalityRu("Москва")
                .setLocalityTr("Moskova")
                .setLocalityUk("Москва")
                .setIsInited(true)
                .build()
        ))
    }

    @Test
    fun shouldInsertNewMomentWithPlaces() {
        parseAndApply("ps_delta_insert_moment_item_with_places.json", 0)

        val items = momentsDatabase.queryReadyMoments()
        assertThat(Moment.Builder.copyOf(items[0]), equalTo(Moment.Builder.newBuilder()
                .setSyncId("0000001431959389000_0000001431959389000")
                .setToDate(DateFormat.asLong("2015-05-18T14:29:49+00:00"))
                .setItemsCount(1)
                .setFromDate(DateFormat.asLong("2015-05-18T14:29:49+00:00"))
                .setLocalityEn("Moscow")
                .setLocalityRu("Москва")
                .setLocalityTr("Moskova")
                .setLocalityUk("Москва")
                .setPlacesRu(listOf("улица Льва Толстого"))
                .setPlacesEn(listOf("ulitsa Lva Tolstogo"))
                .setPlacesTr(listOf("ulitsa Lva Tolstogo"))
                .setPlacesUk(listOf("улица Льва Толстого"))
                .setIsInited(true)
                .build()
        ))
    }

    @Test
    fun shouldCloseCursorOnPlacesUpdate() {
        momentsDatabase.insertOrReplace(newMomentBuilder()
                .setSyncId("0000001431959389000_0000001431959389000").build())

        parseAndApply("ps_delta_insert_moment_item.json", 2)

        dbOpenHelper.close()
    }

    @Test
    fun shouldInsertFirstMomentPlaces() {
        doInSync {
            momentsDatabase.insertOrReplace(newMomentBuilder()
                    .setSyncId("0000001431959389000_0000001431959389000").build())
        }

        parseAndApply("ps_delta_insert_moment_item.json", 2)

        val moments = momentsDatabase.queryReadyMoments()
        val moment = moments[0]
        assertThat(moment.placesRu, equalTo(listOf("улица Льва Толстого")))
        assertThat(moment.placesEn, equalTo(listOf("ulitsa Lva Tolstogo")))
        assertThat(moment.placesTr, equalTo(listOf("ulitsa Lva Tolstogo")))
        assertThat(moment.placesUk, equalTo(listOf("улица Льва Толстого")))
    }

    @Test
    fun shouldUpdateMomentLocality() {
        doInSync {
            momentsDatabase.insertOrReplace(newMomentBuilder()
                    .setLocalityRu("Самара")
                    .setLocalityEn("Samara")
                    .setLocalityTr("Samara")
                    .setLocalityUk("Самара")
                    .setSyncId("0000001432720636000_0000001432720645000").build())
        }

        parseAndApply("ps_delta_locality_update.json", 0)

        val moments = momentsDatabase.queryReadyMoments()
        val moment = moments[0]
        assertThat(moment.localityRu, equalTo("Москва"))
        assertThat(moment.localityEn, equalTo("Moscow"))
        assertThat(moment.localityTr, equalTo("Moskova"))
        assertThat(moment.localityUk, equalTo("Москва"))
    }

    @Test
    fun shouldDeleteMomentLocality() {
        doInSync {
            momentsDatabase.insertOrReplace(newMomentBuilder()
                    .setSyncId("0000001432720636000_0000001432720645000").build())
        }

        parseAndApply("ps_delta_locality_delete.json", 0)

        val moments = momentsDatabase.queryReadyMoments()
        val moment = moments[0]
        assertThat(moment.localityRu, `is`(nullValue()))
        assertThat(moment.localityEn, `is`(nullValue()))
        assertThat(moment.localityTr, `is`(nullValue()))
        assertThat(moment.localityUk, `is`(nullValue()))
    }

    @Test
    fun shouldDeleteLastPlaces() {
        doInSync {
            momentsDatabase.insertOrReplace(newMomentBuilder()
                    .setPlacesRu(listOf("улица Льва Толстого"))
                    .setPlacesEn(listOf("ulitsa Lva Tolstogo"))
                    .setPlacesTr(listOf("ulitsa Lva Tolstogo"))
                    .setPlacesUk(listOf("улица Льва Толстого"))
                    .setSyncId("0000001432720636000_0000001432720645000")
                    .build())
        }
        parseAndApply("ps_delta_places_delete.json", 0)

        val moments = momentsDatabase.queryReadyMoments()
        val moment = moments[0]
        assertThat(moment.placesEn, equalTo(emptyList<Any>()))
        assertThat(moment.placesEn, equalTo(emptyList<Any>()))
        assertThat(moment.placesTr, equalTo(emptyList<Any>()))
        assertThat(moment.placesUk, equalTo(emptyList<Any>()))
    }

    @Test
    fun shouldUpdateMomentFromTo() {
        doInSync {
            momentsDatabase.insertOrReplace(newMomentBuilder()
                    .setFromDate(0L)
                    .setToDate(0L)
                    .setSyncId("0000001432720636000_0000001432720645000")
                    .build())

        }
        parseAndApply("ps_delta_locality_delete.json", 0)

        val moments = momentsDatabase.queryReadyMoments()
        val moment = moments[0]
        assertThat(moment.fromDate, equalTo(DateFormat.asLong("2014-05-27T09:57:16+00:00")))
        assertThat(moment.toDate, equalTo(DateFormat.asLong("2015-05-27T09:57:16+00:00")))
    }

    @Test
    fun shouldInsertMomentPlaceInTheMiddle() {
        doInSync {
            momentsDatabase.insertOrReplace(newMomentBuilder()
                    .setPlacesRu(asList("место 1 ru", "место 2 ru"))
                    .setPlacesEn(asList("место 1 en", "место 2 en"))
                    .setPlacesTr(asList("место 1 tr", "место 2 tr"))
                    .setPlacesUk(asList("место 1 uk", "место 2 uk"))
                    .setSyncId("0000001432720636000_0000001432720636000")
                    .build())
        }
        parseAndApply("ps_delta_insert_places_in_the_middle.json", 0)

        val moments = momentsDatabase.queryReadyMoments()
        val moment = moments[0]
        assertThat(moment.placesRu,
                equalTo(asList("место 1 ru", "улица Льва Толстого", "место 2 ru")))
        assertThat(moment.placesEn,
                equalTo(asList("место 1 en", "ulitsa Lva Tolstogo", "место 2 en")))
        assertThat(moment.placesTr,
                equalTo(asList("место 1 tr", "ulitsa Lva Tolstogo", "место 2 tr")))
        assertThat(moment.placesUk,
                equalTo(asList("место 1 uk", "улица Льва Толстого", "место 2 uk")))
    }

    @Test
    fun shouldUpdateMomentPlaceInTheMiddle() {
        doInSync {
            momentsDatabase.insertOrReplace(newMomentBuilder()
                    .setPlacesRu(asList("место 1 ru", "место 2 ru", "место 3 ru"))
                    .setPlacesEn(asList("место 1 en", "место 2 en", "место 3 en"))
                    .setPlacesTr(asList("место 1 tr", "место 2 tr", "место 3 tr"))
                    .setPlacesUk(asList("место 1 uk", "место 2 uk", "место 3 uk"))
                    .setSyncId("0000001432720636000_0000001432720636000")
                    .build())
        }
        parseAndApply("ps_delta_update_places_in_the_middle.json", 0)

        val moments = momentsDatabase.queryReadyMoments()
        val moment = moments[0]
        assertThat(moment.placesRu,
                equalTo(asList("место 1 ru", "улица Льва Толстого", "место 3 ru")))
        assertThat(moment.placesEn,
                equalTo(asList("место 1 en", "ulitsa Lva Tolstogo", "место 3 en")))
        assertThat(moment.placesTr,
                equalTo(asList("место 1 tr", "ulitsa Lva Tolstogo", "место 3 tr")))
        assertThat(moment.placesUk,
                equalTo(asList("место 1 uk", "улица Льва Толстого", "место 3 uk")))
    }

    @Test
    fun shouldDeleteMomentPlaceFromTheMiddle() {
        doInSync {
            momentsDatabase.insertOrReplace(newMomentBuilder()
                    .setPlacesRu(asList("место 1 ru", "место 2 ru", "место 3 ru"))
                    .setPlacesEn(asList("место 1 en", "место 2 en", "место 3 en"))
                    .setPlacesTr(asList("место 1 tr", "место 2 tr", "место 3 tr"))
                    .setPlacesUk(asList("место 1 uk", "место 2 uk", "место 3 uk"))
                    .setSyncId("0000001432720636000_0000001432720645000")
                    .build())
        }

        parseAndApply("ps_delta_places_delete_from_the_middle.json", 0)

        val moments = momentsDatabase.queryReadyMoments()
        val moment = moments[0]
        assertThat(moment.placesRu,
                equalTo(asList("место 1 ru", "место 3 ru")))
        assertThat(moment.placesEn,
                equalTo(asList("место 1 en", "место 3 en")))
        assertThat(moment.placesTr,
                equalTo(asList("место 1 tr", "место 3 tr")))
        assertThat(moment.placesUk,
                equalTo(asList("место 1 uk", "место 3 uk")))
    }

    @Test
    fun shouldProcessIfMomentAbsent() {
        doInSync {
            momentsDatabase.insertOrReplace(newMomentBuilder()
                    .setPlacesRu(asList("место 1 ru", "место 2 ru", "место 3 ru"))
                    .setSyncId("0000001432720636000_0000001432720645000")
                    .build())
        }
        parseAndApply("ps_delta_places_delete_absent.json", 0)

        val moments = momentsDatabase.queryReadyMoments()
        val moment = moments[0]
        assertThat(moment.placesRu,
                equalTo(asList("место 1 ru", "место 2 ru", "место 3 ru")))
    }

    @Test
    fun shouldParseMoment0() {
        val photosliceTag = PhotosliceTag("1", "2")
        fakeOkHttpInterceptor.addResponse(200, load("photoslice_index_0.json"))

        remoteRepo.listMoments<RuntimeException>(photosliceTag) { items -> items[0].convertToInsertChange().apply(momentsDatabase) }

        val moments = momentsDatabase.queryNotInitedSyncingMoments()
        val moment = moments[0]

        assertThat(moment.itemsCount, equalTo(4))
        assertThat(moment.fromDate, equalTo(1428855804000L))
        assertThat(moment.toDate, equalTo(1428855814000L))
        assertThat(moment.localityRu, equalTo("Самара"))
    }

    @Test
    fun shouldParseMomentWithoutLocality() {
        val photosliceTag = PhotosliceTag("1", "2")
        fakeOkHttpInterceptor.addResponse(200, load("photoslice_index_1.json"))

        remoteRepo.listMoments<RuntimeException>(photosliceTag) { items -> items[0].convertToInsertChange().apply(momentsDatabase) }

        val moments = momentsDatabase.queryNotInitedSyncingMoments()
        val moment = moments[0]
        assertThat(moment.itemsCount, equalTo(5))
    }

    @Test
    fun shouldParseMomentItem() {
        fakeOkHttpInterceptor.addResponse(200, load("photoslice_moment_items.json"))

        remoteRepo.listMomentItems<RuntimeException>(newPhotosliceTag(), listOf("3")
        ) { item -> item.apply(momentsDatabase) }

        val items = momentsDatabase.queryMomentItemMappings()
        items.moveToFirst()
        val item = items.makeItemForRow()!!

        assertThat(item.syncId, equalTo("1_0000001428855814000_2819c27b9a747a36d0" + "722930bcdbc3be41d8a88198fdbb11dbc1d971aba1865b"))
        assertThat(item.path, equalTo("/disk/Фотокамера/2015-04-12 19-23-34.JPG"))
    }

    private fun parseAndApply(responseFileName: String, changeIndex: Int) {
        doInSync {
            fakeOkHttpInterceptor.addResponse(200, load(responseFileName))

            val changes = Collector<Change>()
            remoteRepo.getPhotosliceChanges(newPhotosliceTag(), changes)
            val change = changes[changeIndex]
            change.apply(momentsDatabase)
        }
    }

    private fun doInSync(action: () -> Unit) {
        momentsDatabase.beginSync()
        action.invoke()
        momentsDatabase.setSyncSuccessful()
        momentsDatabase.endSync()
    }

    private fun newPhotosliceTag() = PhotosliceTag("1", "2")
}
