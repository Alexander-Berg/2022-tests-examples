package ru.yandex.supercheck.data.repository.config

import com.nhaarman.mockitokotlin2.whenever
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import org.mockito.Mockito.mock
import ru.yandex.supercheck.analytics.Reporter
import ru.yandex.supercheck.core.scheduler.RxSchedulers
import ru.yandex.supercheck.data.config.debug.config.ConfigSourceResolver
import ru.yandex.supercheck.data.network.api.SupercheckApi
import ru.yandex.supercheck.data.repository.scanandgo.promocode.PromoCodeCache
import ru.yandex.supercheck.model.data.network.api.config.ConfigResponse
import ru.yandex.supercheck.model.data.network.api.productlist.createConfigFeatureDTOAdapterFactory
import ru.yandex.supercheck.model.domain.promocode.PromoCode
import ru.yandex.supercheck.readFile
import java.util.concurrent.TimeUnit


class ConfigRepositoryImplTest {

    companion object {
        private val CONFIG_RESPONSE_JSON = readFile("config_response.json")

        private const val VKISVILL_SCANNING_PROMO_FEATURE = "vkusvill_scanning_promo"
    }

    private val mockSupercheckApi = mock(SupercheckApi::class.java)
    private val mockConfigSourceResolver = mock(ConfigSourceResolver::class.java)
    private val promoCodeCache = mock(PromoCodeCache::class.java)
    private val promoCodeConverter = mock(PromoCode.Converter::class.java)
    private val reporter = mock(Reporter::class.java)

    private val configRepositoryImpl =
        ConfigRepositoryImpl(mockSupercheckApi, object : RxSchedulers {
            override val io: Scheduler
                get() = TestScheduler()
            override val computation: Scheduler
                get() = TestScheduler()
            override val trampoline: Scheduler
                get() = TestScheduler()
            override val mainThread: Scheduler
                get() = throw NotImplementedError()
        }, mockConfigSourceResolver, promoCodeCache, promoCodeConverter, reporter)

    @Test
    fun isFeaturePresent_success() {
        mockSuccess()

        val testObserver = configRepositoryImpl
            .isFeaturePresent(VKISVILL_SCANNING_PROMO_FEATURE)
            .test()

        testObserver.assertNoErrors().assertValue(true)
    }

    @Test
    fun isFeaturePresent_exception() {
        mockFail()

        val testObserver = configRepositoryImpl
            .isFeaturePresent(VKISVILL_SCANNING_PROMO_FEATURE)
            .test()

        testObserver.assertNoErrors().assertNoValues()
    }

    @Test
    fun isFeaturePresent_successAfterException() {
        mockSuccessAfterFail()

        val testObserver = configRepositoryImpl
            .isFeaturePresent(VKISVILL_SCANNING_PROMO_FEATURE)
            .test()

        testObserver.assertNoErrors().assertNoValues().awaitDone(3, TimeUnit.SECONDS)
            .assertValue(true)
    }

    private fun mockSuccess() {
        whenever(mockSupercheckApi.getConfig()).thenReturn(successResult)
    }

    private fun mockFail() {
        whenever(mockSupercheckApi.getConfig()).thenReturn(errorResult)
    }

    private fun mockSuccessAfterFail() {
        whenever(mockSupercheckApi.getConfig()).thenReturn(successAfterError)
    }

    private val errorResult = Single.error<ConfigResponse>(RuntimeException())

    private val successResult = Single.just(getConfigResponse()!!)

    private val successAfterError = Single.create(object : SingleOnSubscribe<ConfigResponse> {
        var counter = 2
        override fun subscribe(emitter: SingleEmitter<ConfigResponse>) {
            if (counter > 0) {
                emitter.onError(RuntimeException())
                counter--
            } else {
                emitter.onSuccess(getConfigResponse()!!)
            }
        }

    })

    private fun getConfigResponse(): ConfigResponse? {
        val parser = Moshi.Builder()
            .add(createConfigFeatureDTOAdapterFactory())
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter(ConfigResponse::class.java)

        return parser.fromJson(CONFIG_RESPONSE_JSON)
    }


}