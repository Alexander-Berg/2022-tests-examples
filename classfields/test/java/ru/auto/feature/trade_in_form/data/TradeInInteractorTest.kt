package ru.auto.feature.trade_in_form.data

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.RxTest
import ru.auto.data.model.Currency
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.model.network.scala.stats.NWPredictPrices
import ru.auto.data.model.network.scala.stats.NWPriceRange
import ru.auto.data.model.network.scala.trade_in.NWTradeInAvailableResponse
import ru.auto.data.network.scala.ScalaApi
import ru.auto.data.network.scala.response.PredictPricesResponse
import ru.auto.data.repository.StatRepository
import rx.Single
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class TradeInInteractorTest: RxTest() {

    private val scalaApi: ScalaApi = mock()

    private val interactor: TradeInInteractor = TradeInInteractor(
        TradeInRepository(scalaApi),
        StatRepository(scalaApi)
    )

    @Test
    fun `trade in predict is correct if response filled`() {

        mockStatsPredict(true)
        mockTradeInIsAvailable(true)

        val predict = interactor.getTradeInPredict(OFFER).toBlocking().value()

        assertEquals(100_000L, predict.tradeInBuyoutPrice.amountFrom)
        assertEquals(150_000L, predict.tradeInBuyoutPrice.amountTo)
        assertEquals(Currency.RUR, predict.tradeInBuyoutPrice.currency)

        assertEquals(200_000L, predict.tradeInUsedPrice.amountFrom)
        assertEquals(250_000L, predict.tradeInUsedPrice.amountTo)
        assertEquals(Currency.RUR, predict.tradeInUsedPrice.currency)

        assertEquals(300_000L, predict.tradeInNewPrice.amountFrom)
        assertEquals(350_000L, predict.tradeInNewPrice.amountTo)
        assertEquals(Currency.RUR, predict.tradeInNewPrice.currency)

        verify(scalaApi).isTradeInAvailable(
            argWhere { request ->
                assertEquals(OFFER.id, request.offer?.id)
                true
            }
        )
    }

    @Test
    fun `trade in predict error if is not available`() {

        mockStatsPredict(true)
        mockTradeInIsAvailable(false)

        assertThrows<RuntimeException> {
            interactor.getTradeInPredict(OFFER).toBlocking().value()
        }
    }

    @Test
    fun `trade in predict error if stats predict is not filled`() {

        mockStatsPredict(false)
        mockTradeInIsAvailable(true)

        assertThrows<RuntimeException> {
            interactor.getTradeInPredict(OFFER).toBlocking().value()
        }
    }

    @Test
    fun `trade in predict error if there is moto offer`() {

        mockStatsPredict(true)
        mockTradeInIsAvailable(true)

        val offer = OFFER.copy(category = VehicleCategory.MOTO)
        assertThrows<RuntimeException> {
            interactor.getTradeInPredict(offer).toBlocking().value()
        }

        verify(scalaApi, never()).isTradeInAvailable(any())
    }

    @Test
    fun `trade in predict error if there is trucks offer`() {

        mockStatsPredict(true)
        mockTradeInIsAvailable(true)

        val offer = OFFER.copy(category = VehicleCategory.TRUCKS)
        assertThrows<RuntimeException> {
            interactor.getTradeInPredict(offer).toBlocking().value()
        }

        verify(scalaApi, never()).isTradeInAvailable(any())
    }

    @Test
    fun `trade in predict error if there is dealer offer`() {

        mockStatsPredict(true)
        mockTradeInIsAvailable(true)

        val offer = OFFER.copy(sellerType = SellerType.COMMERCIAL)
        assertThrows<RuntimeException> {
            interactor.getTradeInPredict(offer).toBlocking().value()
        }

        verify(scalaApi, never()).isTradeInAvailable(any())
    }

    private fun mockTradeInIsAvailable(isAvailable: Boolean) {
        whenever(scalaApi.isTradeInAvailable(any())).thenReturn(Single.just(NWTradeInAvailableResponse(isAvailable)))
    }

    private fun mockStatsPredict(isResponseFilled: Boolean) {

        val predictedPrices = when {
            isResponseFilled -> {
                PredictPricesResponse(
                    prices = NWPredictPrices(
                        tradein_dealer_matrix_buyout = NWPriceRange(
                            from = 100_000L,
                            to = 150_000L,
                            currency = RUR_CURRENCY
                        ),
                        tradein_dealer_matrix_used = NWPriceRange(
                            from = 200_000L,
                            to = 250_000L,
                            currency = RUR_CURRENCY
                        ),
                        tradein_dealer_matrix_new = NWPriceRange(
                            from = 300_000L,
                            to = 350_000L,
                            currency = RUR_CURRENCY
                        )
                    )
                )
            }
            else -> PredictPricesResponse()
        }

        whenever(scalaApi.getPredictedPrices(any())).thenReturn(Single.just(predictedPrices))
    }

    companion object {
        private val OFFER = Offer(
            id = "1",
            category = VehicleCategory.CARS,
            sellerType = SellerType.PRIVATE
        )
        private const val RUR_CURRENCY = "RUR"
    }
}
