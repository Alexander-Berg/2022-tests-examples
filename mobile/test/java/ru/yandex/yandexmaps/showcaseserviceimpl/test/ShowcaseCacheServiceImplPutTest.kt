package ru.yandex.yandexmaps.showcaseserviceimpl.test

import com.nhaarman.mockito_kotlin.mock
import com.pushtorefresh.storio3.sqlite.StorIOSQLite
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.yandex.maps.showcase.showcaseservice.DbConnectionModule
import ru.yandex.maps.showcase.showcaseservice.ShowcaseCacheServiceImpl
import ru.yandex.maps.showcase.showcaseservice.db.getEntities
import ru.yandex.maps.showcase.showcaseservice.moshi.BoundingBox
import ru.yandex.maps.showcase.showcaseservice.moshi.Serializer
import ru.yandex.maps.showcase.showcaseservice.moshi.boundingBox
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.Meta
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.ShowcaseDataType
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.ShowcaseV3Data
import ru.yandex.maps.storiopurgatorium.showcase.ShowcaseDataEntity
import ru.yandex.maps.storiopurgatorium.showcase.ShowcaseMetadataEntity
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point
import java.util.*
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ShowcaseCacheServiceImplPutTest {

    private lateinit var showcaseData: ShowcaseV3Data
    private lateinit var ioScheduler: TestScheduler
    private lateinit var storIOSQLite: StorIOSQLite

    @Before
    fun setUp() {
        @Suppress("DEPRECATION") // blocked by androidx migration
        val application = RuntimeEnvironment.application

        showcaseData = ShowcaseV3Data(
            meta = Meta(
                type = ShowcaseDataType.EMPTY,
                boundingBoxes = listOf(
                    BoundingBox(
                        northEast = Point(55.00, 33.00),
                        southWest = Point(54.00, 32.00)
                    ),
                    BoundingBox(
                        northEast = Point(65.00, 43.00),
                        southWest = Point(64.00, 42.00)
                    )
                ),
                zoomRange = Meta.ZoomRange(0, 19),
                expires = Date(System.currentTimeMillis() + 100000L)
            ),
            rubrics = mock(defaultAnswer = Answers.RETURNS_MOCKS),
            dataV2 = ShowcaseV3Data.V2Data()
        )

        ioScheduler = TestScheduler()
        storIOSQLite = DbConnectionModule.storIOSQLite(application)
    }

    @Test
    fun keepCacheTest() {
        val showcaseCacheServiceImpl = ShowcaseCacheServiceImpl(Provider { Serializer.moshi }, storIOSQLite, ioScheduler)
        showcaseCacheServiceImpl.putData(showcaseData).subscribe()
        ioScheduler.triggerActions()

        val metadataCount = storIOSQLite.getEntities<ShowcaseMetadataEntity>(ShowcaseMetadataEntity.TABLE_NAME).executeAsBlocking().size
        val dataCount = storIOSQLite.getEntities<ShowcaseDataEntity>(ShowcaseDataEntity.TABLE_NAME).executeAsBlocking().size

        assertThat(metadataCount).isEqualTo(2)
        assertThat(dataCount).isEqualTo(1)
    }

    @Test
    fun insertMetadataWithSameKeyTest() {
        val showcase = ShowcaseV3Data(
            meta = Meta(
                type = ShowcaseDataType.EMPTY,
                boundingBoxes = listOf(boundingBox(50.331880733483764, 22.001611471657753, 46.59320118809951, 16.376611471657753)),
                zoomRange = Meta.ZoomRange(0, 23),
                expires = Date(1538415810556)
            ),
            rubrics = mock(defaultAnswer = Answers.RETURNS_MOCKS),
            dataV2 = ShowcaseV3Data.V2Data()
        )
        val showcaseCacheServiceImpl = ShowcaseCacheServiceImpl(Provider { Serializer.moshi }, storIOSQLite, ioScheduler)
        showcaseCacheServiceImpl.putData(showcase).subscribe()
        ioScheduler.triggerActions()

        showcaseCacheServiceImpl.putData(showcase.copy(meta = showcase.meta.copy(expires = Date(System.currentTimeMillis() + 500000L)))).subscribe()
        ioScheduler.triggerActions()

        val metadataCount = storIOSQLite.getEntities<ShowcaseMetadataEntity>(ShowcaseMetadataEntity.TABLE_NAME).executeAsBlocking().size
        val dataCount = storIOSQLite.getEntities<ShowcaseDataEntity>(ShowcaseDataEntity.TABLE_NAME).executeAsBlocking().size

        assertThat(metadataCount).isEqualTo(1)
        assertThat(dataCount).isEqualTo(2)
    }
}
