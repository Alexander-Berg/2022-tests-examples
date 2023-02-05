package ru.yandex.market.ui.view.mvp.cartcounterbutton

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.facades.OffersCacheHealthFacade
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OffersCacheTest {

    private val offersCacheHealthFacade = mock<OffersCacheHealthFacade>()
    private val configuration = OffersCache.Configuration(
        countForSendHealthEvent = 100
    )

    private val offersCache = OffersCache(offersCacheHealthFacade, configuration)

    @Test
    fun `check send health analytic`() {
        verify(offersCacheHealthFacade, never()).sendAddOffersToCacheEvent(any())

        for (i in 0..310) {
            offersCache.addOffer(productOfferTestInstance(offer = offerTestInstance(stockKeepingUnitId = i.toString())))
        }

        verify(offersCacheHealthFacade, times(3)).sendAddOffersToCacheEvent(any())
    }

    @Test
    fun `check that the cache is synchronized`() {
        val executorService1 = Executors.newSingleThreadExecutor()
            .apply {
                execute {
                    for (i in 0..499) {
                        offersCache.addOffer(
                            productOfferTestInstance(
                                offer = offerTestInstance(stockKeepingUnitId = "thread 1-$i")
                            )
                        )
                    }
                }
            }
        val executorService2 = Executors.newSingleThreadExecutor()
            .apply {
                execute {
                    for (i in 0..499) {
                        offersCache.addOffer(
                            productOfferTestInstance(
                                offer = offerTestInstance(stockKeepingUnitId = "thread 2-$i")
                            )
                        )
                    }
                }
            }
        executorService1.awaitTermination(5, TimeUnit.SECONDS)
        executorService2.awaitTermination(5, TimeUnit.SECONDS)
        executorService1.shutdown()
        executorService2.shutdown()

        Assertions.assertThat(offersCache.offers.size).isEqualTo(1000)
    }
}