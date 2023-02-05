package ru.yandex.supercheck.domain.promocode

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Maybe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.supercheck.data.repository.scanandgo.promocode.PromoCodeCache
import ru.yandex.supercheck.data.repository.scanandgo.promocode.PromoCodeRepositoryImpl
import ru.yandex.supercheck.model.domain.promocode.PromoCode

@RunWith(MockitoJUnitRunner::class)
class PromoCodeInteractorTest {

    private val promoCodeCache = mock<PromoCodeCache>()

    private val promoCodeInteractor =
        PromoCodeInteractor(PromoCodeRepositoryImpl(promoCodeCache))

    @Before
    fun setUp() {
        mockPromoCodes(Maybe.just(TestData.PROMO_CODES))
        mockPrimaryPromoCode(Maybe.just(TestData.VKUSVILL_PROMO_CODE_15))
    }

    @Test
    fun promoCodeForMainScreenExists() {
        promoCodeInteractor.getPromoCodeForMainScreen()
            .test()
            .assertResult(TestData.VKUSVILL_PROMO_CODE_15)
    }

    @Test
    fun promoCodeForMainScreenNotExists() {
        mockPrimaryPromoCode(Maybe.empty())
        promoCodeInteractor.getPromoCodeForMainScreen()
            .test()
            .assertResult()
    }

    @Test
    fun promoCodeForShopExists() {
        promoCodeInteractor.getPromoCodeForShop(TestData.VKUSVILL_ID)
            .test()
            .assertResult(TestData.VKUSVILL_PROMO_CODE_15)
    }

    @Test
    fun promoCodeForShopNotExists() {
        mockPromoCodes(Maybe.just(listOf(TestData.ULYBKA_RADUGI_PROMO_CODE_100)))
        promoCodeInteractor.getPromoCodeForShop(TestData.VKUSVILL_ID)
            .test()
            .assertResult()
    }

    private fun mockPrimaryPromoCode(promoCode: Maybe<PromoCode>) {
        whenever(promoCodeCache.getPrimaryPromoCode()).thenReturn(promoCode)
    }

    private fun mockPromoCodes(promoCodes: Maybe<List<PromoCode>>) {
        whenever(promoCodeCache.getPromoCodes()).thenReturn(promoCodes)
    }

}