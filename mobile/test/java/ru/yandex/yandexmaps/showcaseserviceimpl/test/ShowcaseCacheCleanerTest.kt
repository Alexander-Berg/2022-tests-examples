package ru.yandex.yandexmaps.showcaseserviceimpl.test

import com.pushtorefresh.storio3.sqlite.StorIOSQLite
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.yandex.maps.showcase.showcaseservice.DbConnectionModule
import ru.yandex.maps.showcase.showcaseservice.ShowcaseCacheCleanerImpl
import ru.yandex.maps.showcase.showcaseservice.db.getEntities
import ru.yandex.maps.showcase.showcaseservice.db.putEntities
import ru.yandex.maps.showcase.showcaseservice.db.putEntity
import ru.yandex.maps.showcase.showcaseservice.db.toLongCoordinate
import ru.yandex.maps.storiopurgatorium.showcase.ShowcaseDataEntity
import ru.yandex.maps.storiopurgatorium.showcase.ShowcaseMetadataEntity
import ru.yandex.yandexmaps.multiplatform.core.safemode.SafeModeIndicator
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class ShowcaseCacheCleanerTest {

    private lateinit var store: StorIOSQLite
    private val expiredShowcaseMeta = ShowcaseMetadataEntity(
        northEastLat = 14.00.toLongCoordinate(),
        northEastLon = 14.00.toLongCoordinate(),
        southWestLat = 13.00.toLongCoordinate(),
        southWestLon = 13.00.toLongCoordinate(),
        zoomMin = 0,
        zoomMax = 23,
        expire = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
        dataId = 1
    )
    private val actualShowcaseMeta = ShowcaseMetadataEntity(
        northEastLat = 13.00.toLongCoordinate(),
        northEastLon = 13.00.toLongCoordinate(),
        southWestLat = 12.00.toLongCoordinate(),
        southWestLon = 12.00.toLongCoordinate(),
        zoomMin = 0,
        zoomMax = 23,
        expire = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
        dataId = 2
    )

    @Before
    fun setUp() {
        @Suppress("DEPRECATION") // blocked by androidx migration
        val application = RuntimeEnvironment.application
        store = DbConnectionModule.storIOSQLite(application)
    }

    @Test
    fun removeExpiredMetadataTest() {
        store.putEntities(listOf(ShowcaseDataEntity(id = null, data = "data1"), ShowcaseDataEntity(id = null, data = "data2"))).executeAsBlocking()
        store.putEntities(listOf(expiredShowcaseMeta, actualShowcaseMeta)).executeAsBlocking()
        val testScheduler = TestScheduler()

        val showcaseCacheCleaner = ShowcaseCacheCleanerImpl(store, testScheduler, SafeModeIndicator.STUB)
        showcaseCacheCleaner.removeExpiredMetadata().subscribe()
        testScheduler.triggerActions()

        val metadataEntities = store.getEntities<ShowcaseMetadataEntity>(ShowcaseMetadataEntity.TABLE_NAME).executeAsBlocking()
        assertThat(metadataEntities).hasSize(1)
        assertThat(metadataEntities).element(0).isEqualTo(actualShowcaseMeta)
    }

    @Test
    fun removeUnusedDataTest() {
        val data1 = ShowcaseDataEntity(id = null, data = "data1")
        val data2 = ShowcaseDataEntity(id = null, data = "data2")

        store.putEntities(listOf(data1, data2)).executeAsBlocking()
        store.putEntities(listOf(expiredShowcaseMeta, actualShowcaseMeta)).executeAsBlocking()

        val data3 = ShowcaseDataEntity(id = null, data = "data3")
        val newEpiredMetadata = expiredShowcaseMeta.copy(dataId = 3)
        store.putEntity(data3).executeAsBlocking()
        store.putEntity(newEpiredMetadata).executeAsBlocking()

        val testScheduler = TestScheduler()
        val showcaseCacheCleaner = ShowcaseCacheCleanerImpl(store, testScheduler, SafeModeIndicator.STUB)
        showcaseCacheCleaner.removeExpiredMetadata().subscribe()
        testScheduler.triggerActions()

        val dataEntities = store.getEntities<ShowcaseDataEntity>(ShowcaseDataEntity.TABLE_NAME).executeAsBlocking()
        assertThat(dataEntities).apply {
            hasSize(1)
            element(0).isEqualToIgnoringGivenFields(data2, "id")
        }
    }
}
