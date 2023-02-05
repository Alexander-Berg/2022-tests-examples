package ru.yandex.yandexmaps.showcaseserviceimpl.test

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import ru.yandex.maps.showcase.showcaseservice.ShowcaseCacheService
import ru.yandex.maps.showcase.showcaseservice.ShowcaseLookupServiceImpl
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.ShowcaseCamera
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.ShowcaseDataState
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.ShowcaseService
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.CachedShowcaseData
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.CameraPosition
import java.util.concurrent.TimeUnit
import javax.inject.Provider

internal class ShowcaseLookupServiceTest {

    private lateinit var instantShowcaseService: ShowcaseService
    private lateinit var remoteServiceScheduler: TestScheduler
    private lateinit var remoteRequestShowcaseService: ShowcaseService
    private lateinit var cacheServiceWithData: ShowcaseCacheService
    private lateinit var cacheServiceWithoutData: ShowcaseCacheService

    private val showcaseCamera = object : ShowcaseCamera {
        override val cameraMoves: Observable<CameraPosition> = Observable.just(mock(defaultAnswer = Answers.RETURNS_MOCKS))
    }

    @Before
    fun setup() {

        instantShowcaseService = mock {
            whenever(it.getShowcaseData(any(), any()))
                .thenAnswer {
                    Maybe.just(mock<CachedShowcaseData>(defaultAnswer = Answers.RETURNS_MOCKS))
                }
        }

        remoteServiceScheduler = TestScheduler()
        remoteRequestShowcaseService = mock {
            whenever(it.getShowcaseData(any(), any()))
                .thenAnswer {
                    Maybe.timer(500, TimeUnit.MILLISECONDS, remoteServiceScheduler)
                        .map { mock<CachedShowcaseData>(defaultAnswer = Answers.RETURNS_MOCKS) }
                }
        }

        cacheServiceWithData = mock {
            whenever(it.getCacheForCameraPosition(any(), any()))
                .thenAnswer {
                    Maybe.just(mock<CachedShowcaseData>(defaultAnswer = Answers.RETURNS_MOCKS))
                }
        }

        cacheServiceWithoutData = mock {
            whenever(it.getCacheForCameraPosition(any(), any()))
                .thenAnswer {
                    Maybe.empty<CachedShowcaseData>()
                }
        }
    }

    @Test
    fun cachedDataIsReturnedInstantly() {

        var result: ShowcaseDataState? = null

        val showcaseLookupServiceImpl = ShowcaseLookupServiceImpl(
            showcaseCamera = showcaseCamera,
            showcaseService = dagger.Lazy { instantShowcaseService },
            showcaseCacheServiceProvider = Provider { cacheServiceWithData },
            computationScheduler = TestScheduler()
        )

        showcaseLookupServiceImpl.showcaseDataChanges().subscribe { result = it }

        assertThat(result).isInstanceOf(ShowcaseDataState.Success::class.java)
    }

    @Test
    fun ifCacheDoesNotExistAndServiceSlowReturnLoadingAndThenSuccess() {
        val results = mutableListOf<ShowcaseDataState>()

        val showcaseLookupServiceImpl = ShowcaseLookupServiceImpl(
            showcaseCamera = showcaseCamera,
            showcaseService = dagger.Lazy { remoteRequestShowcaseService },
            showcaseCacheServiceProvider = Provider { cacheServiceWithoutData },
            computationScheduler = remoteServiceScheduler
        )

        showcaseLookupServiceImpl.showcaseDataChanges().subscribe { results.add(it) }
        remoteServiceScheduler.advanceTimeBy(ShowcaseLookupServiceImpl.minTimeForResponse, ShowcaseLookupServiceImpl.timeUnitForResponse)

        assertThat(results).element(0).isInstanceOf(ShowcaseDataState.Loading::class.java)
        assertThat(results).element(1).isInstanceOf(ShowcaseDataState.Success::class.java)
    }

    @Test
    fun isCacheDoesNotExistAndShowcaseCollapsedReceivedLoading() {
        val result = mutableListOf<ShowcaseDataState>()
        val scheduler = TestScheduler()
        val showcaseLookupServiceImpl = ShowcaseLookupServiceImpl(
            showcaseCamera = showcaseCamera,
            showcaseService = dagger.Lazy { remoteRequestShowcaseService },
            showcaseCacheServiceProvider = Provider { cacheServiceWithoutData },
            computationScheduler = scheduler
        )

        showcaseLookupServiceImpl.showcaseDataChanges().subscribe { result.add(it) }

        assertThat(result).size().isEqualTo(1)
        assertThat(result).element(0).isInstanceOf(ShowcaseDataState.Loading::class.java)
    }
}
