package com.yandex.mobile.realty.promo

import com.yandex.mobile.realty.data.repository.InMemoryPromoShowRepository
import com.yandex.mobile.realty.domain.model.geo.GeoIntent
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.domain.search.interactor.YandexRentSearchInteractor
import com.yandex.mobile.realty.domain.search.interactor.YandexRentSearchParams
import com.yandex.mobile.realty.domain.startup.interactor.WelcomePromoInteractor
import com.yandex.mobile.realty.domain.startup.repository.WelcomePromoRepository
import com.yandex.mobile.realty.rx.Optional
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import rx.Single

/**
 * @author solovevai on 01.06.2020.
 */
class WelcomePromoInteractorTest {

    @Test
    fun testShouldNotShowPromoWhenPromoShown() {
        val interactor = createWelcomePromoInteractor(promoShown = true)
        assertFalse(interactor.shouldShowPromo().toBlocking().value())
    }

    @Test
    fun testShouldNotShowPromoWhenYandexRentIsNotAvailable() {
        val interactor = createWelcomePromoInteractor(
            promoShown = false,
            isYandexRentAvailable = false
        )
        assertFalse(interactor.shouldShowPromo().toBlocking().value())
    }

    @Test
    fun testShouldShowPromoWhenPromoNotShown() {
        val interactor = createWelcomePromoInteractor(promoShown = false)
        assertTrue(interactor.shouldShowPromo().toBlocking().value())
    }

    private fun createWelcomePromoInteractor(
        promoShown: Boolean,
        isYandexRentAvailable: Boolean = true
    ): WelcomePromoInteractor {
        val promoRepository = mock(WelcomePromoRepository::class.java)
        `when`(promoRepository.isWelcomePromoShown()).thenReturn(Single.just(promoShown))
        val yandexRentSearchInteractor = mock(YandexRentSearchInteractor::class.java)
        val searchParams = if (isYandexRentAvailable) {
            val region = GeoRegion.DEFAULT
            YandexRentSearchParams(
                Filter.DEFAULT,
                GeoIntent.Objects.valueOf(region),
                region
            )
        } else {
            null
        }
        `when`(yandexRentSearchInteractor.getSearchParamsForCurrentRegion())
            .thenReturn(Single.just(Optional.of(searchParams)))

        val promoShownRepository = InMemoryPromoShowRepository()

        return WelcomePromoInteractor(
            promoRepository,
            promoShownRepository,
            yandexRentSearchInteractor
        )
    }
}
