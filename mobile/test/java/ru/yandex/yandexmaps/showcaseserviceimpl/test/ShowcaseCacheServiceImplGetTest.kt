package ru.yandex.yandexmaps.showcaseserviceimpl.test

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.yandex.maps.showcase.showcaseservice.DbConnectionModule
import ru.yandex.maps.showcase.showcaseservice.ShowcaseCacheServiceImpl
import ru.yandex.maps.showcase.showcaseservice.moshi.Serializer
import ru.yandex.maps.showcase.showcaseservice.moshi.boundingBox
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.Meta
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.ShowcaseDataType
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.ShowcaseV3Data
import ru.yandex.yandexmaps.multiplatform.core.geometry.BoundingBox
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point
import java.util.*
import javax.inject.Provider

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class ShowcaseCacheServiceImplGetTest(boundingBoxes: List<BoundingBox>, private val point: Point, private val isPointInside: Boolean) {

    private val showcaseData = ShowcaseV3Data(
        meta = Meta(
            type = ShowcaseDataType.EMPTY,
            boundingBoxes = boundingBoxes,
            zoomRange = Meta.ZoomRange(0, 19),
            expires = Date(System.currentTimeMillis() + 100000L)
        ),
        rubrics = mock(defaultAnswer = Answers.RETURNS_MOCKS),
        dataV2 = ShowcaseV3Data.V2Data()
    )

    private val testScheduler = TestScheduler()
    @Suppress("DEPRECATION") // blocked by androidx migration
    private val database = DbConnectionModule.storIOSQLite(RuntimeEnvironment.application)

    private val showcaseCacheServiceImpl = ShowcaseCacheServiceImpl(
        moshiProvider = Provider { Serializer.moshi },
        cacheDatabase = database,
        ioScheduler = testScheduler
    )

    init {
        showcaseCacheServiceImpl.putData(showcaseData).subscribe()
        testScheduler.triggerActions()
    }

    @Test
    fun getTest() {
        var showcaseData: ShowcaseV3Data? = null
        showcaseCacheServiceImpl.getCacheForCameraPosition(point, 10).subscribe { showcaseData = it.data }
        testScheduler.triggerActions()
        if (isPointInside) assertThat(showcaseData).isNotNull() else assertThat(showcaseData).isNull()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "BB: {0}; CenterPoint: {1}, assert = {2}")
        fun parameters(): List<Array<Any>> = listOf(
            arrayOf(listOf(boundingBox(3.0, 3.0, 5.0, 5.0)), Point(2.0, 2.0), false),
            arrayOf(listOf(boundingBox(3.0, 3.0, 5.0, 5.0)), Point(4.0, 4.0), true),
            arrayOf(listOf(boundingBox(-1.9, 3.0, -1.1, 5.0)), Point(-1.5, 4.0), true),
            arrayOf(listOf(boundingBox(-1.9, 3.0, -1.1, 5.0)), Point(-1.98, 4.0), false),
            arrayOf(listOf(boundingBox(-3.0, -3.0, -1.0, -1.0)), Point(-2.0, -2.0), true),
            arrayOf(listOf(boundingBox(-3.0, -3.0, -1.0, -1.0)), Point(-4.0, -4.0), false),
            arrayOf(listOf(boundingBox(3.0, -3.0, 5.0, -1.0)), Point(4.0, -2.0), true),
            arrayOf(listOf(boundingBox(3.0, -3.0, 5.0, -1.0)), Point(2.0, -2.0), false),
            arrayOf(listOf(boundingBox(-1.0, -1.0, 1.0, 1.0)), Point(0.0, 0.0), true),
            arrayOf(listOf(boundingBox(3.0, 7.0, 5.0, 5.0)), Point(4.0, 6.0), false),
            arrayOf(listOf(boundingBox(3.0, 7.0, 5.0, 5.0)), Point(4.0, 8.0), true)
        )
    }
}
