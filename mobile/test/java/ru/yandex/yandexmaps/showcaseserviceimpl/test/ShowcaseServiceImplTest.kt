package ru.yandex.yandexmaps.showcaseserviceimpl.test

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.MockitoAnnotations.openMocks
import retrofit2.HttpException
import retrofit2.Response
import ru.yandex.maps.showcase.showcaseservice.ShowcaseCacheService
import ru.yandex.maps.showcase.showcaseservice.ShowcaseServiceImpl
import ru.yandex.maps.showcase.showcaseservice.moshi.BoundingBox
import ru.yandex.maps.showcase.showcaseservice.request.ShowcaseRequestService
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.ShowcaseConfig
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.analytics.ShowcaseServiceAnalytics
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.CachedShowcaseData
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.Meta
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.ShowcaseDataType
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.ShowcaseV3Data
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point
import java.util.Date

internal class ShowcaseServiceImplTest {

    private val somePoint = Point(54.50, 32.50)
    private val someZoom: Int = 13

    private val errorPoint = Point(90.00, 90.00)

    private lateinit var showcaseData: ShowcaseV3Data
    private lateinit var cachedShowcaseData: CachedShowcaseData

    private lateinit var emptyCacheService: ShowcaseCacheService
    private lateinit var fullCacheService: ShowcaseCacheService
    private lateinit var stubShowcaseRequestService: ShowcaseRequestService
    private lateinit var fullShowcaseRequestService: ShowcaseRequestService
    private lateinit var errorShowcaseRequestService: ShowcaseRequestService
    private lateinit var ioScheduler: TestScheduler
    private lateinit var showcaseRequestServiceWithRichAnswer: ShowcaseRequestService
    private lateinit var config: ShowcaseConfig

    @Mock
    private lateinit var analytics: ShowcaseServiceAnalytics

    private lateinit var mocksCloseable: AutoCloseable

    @Before
    fun refreshData() {
        mocksCloseable = openMocks(this)
        showcaseData = ShowcaseV3Data(
            meta = Meta(
                type = ShowcaseDataType.EMPTY,
                boundingBoxes = listOf(
                    BoundingBox(
                        northEast = Point(55.00, 33.00),
                        southWest = Point(54.00, 32.00)
                    )
                ),
                zoomRange = Meta.ZoomRange(0, 19),
                expires = Date(System.currentTimeMillis() + 100000L)
            ),
            rubrics = mock(defaultAnswer = Answers.RETURNS_MOCKS),
            dataV2 = ShowcaseV3Data.V2Data()
        )

        cachedShowcaseData = CachedShowcaseData(showcaseData, 0)

        emptyCacheService = mock {
            on { getCacheForCameraPosition(somePoint, someZoom) } doReturn Maybe.empty<CachedShowcaseData>()
            on { getCacheForCameraPosition(errorPoint, someZoom) } doReturn Maybe.empty<CachedShowcaseData>()
        }

        fullCacheService = mock {
            on { getCacheForCameraPosition(somePoint, someZoom) } doReturn Maybe.just(cachedShowcaseData)
        }

        stubShowcaseRequestService = mock(defaultAnswer = Answers.RETURNS_MOCKS)
        fullShowcaseRequestService = mock {
            on { getShowcaseV3(somePoint.lon, somePoint.lat, someZoom) } doReturn Single.just(showcaseData)
        }

        showcaseRequestServiceWithRichAnswer = mock {
            on { getShowcaseV3(somePoint.lon, somePoint.lat, someZoom) } doReturn Single.just(showcaseData.copy(meta = showcaseData.meta.copy(type = ShowcaseDataType.RICH)))
        }

        errorShowcaseRequestService = mock {
            on { getShowcaseV3(errorPoint.lon, errorPoint.lat, someZoom) } doReturn
                Single.error<ShowcaseV3Data>(HttpException(Response.error<ShowcaseV3Data>(500, "".toResponseBody("json".toMediaTypeOrNull()))))
        }

        config = ShowcaseConfig(isDraft = false)

        ioScheduler = TestScheduler()
    }

    @After
    fun tearDown() {
        mocksCloseable.close()
    }

    @Test
    fun putCacheTest() {
        val showcaseServiceImpl = ShowcaseServiceImpl(fullShowcaseRequestService, emptyCacheService, ioScheduler, analytics, config)
        showcaseServiceImpl.getShowcaseData(somePoint, someZoom).subscribe()
        ioScheduler.triggerActions()

        verify(emptyCacheService, times(1)).putData(showcaseData)
    }

    @Test
    fun sendErrorToAnalyticsTest() {
        val showcaseServiceImpl = ShowcaseServiceImpl(errorShowcaseRequestService, emptyCacheService, ioScheduler, analytics, config)

        showcaseServiceImpl.getShowcaseData(errorPoint, someZoom).subscribe()
        showcaseServiceImpl.getShowcaseData(errorPoint, someZoom).subscribe()
        ioScheduler.triggerActions()

        verify(analytics, times(2)).showcaseResponseError(500, "retrofit2.HttpException", "HTTP 500 Response.error()")
    }
}
