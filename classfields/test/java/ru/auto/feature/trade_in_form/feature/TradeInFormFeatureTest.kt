package ru.auto.feature.trade_in_form.feature

import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.RxTest
import ru.auto.core_logic.Analyst
import ru.auto.data.model.Currency
import ru.auto.data.model.MoneyRange
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.model.data.offer.TradeInApplyInfo
import ru.auto.data.model.data.offer.TradeInType
import ru.auto.data.network.scala.ScalaApi
import ru.auto.data.network.scala.response.BaseResponse
import ru.auto.data.repository.StatRepository
import ru.auto.feature.trade_in_form.TradeInFormCoordinator
import ru.auto.feature.trade_in_form.TradeInFormProvider
import ru.auto.feature.trade_in_form.data.ITradeInInteractor
import ru.auto.feature.trade_in_form.data.TradeInInteractor
import ru.auto.feature.trade_in_form.data.TradeInRepository
import ru.auto.feature.trade_in_form.data.model.TradeInPredict
import ru.auto.feature.trade_in_form.di.ITradeInFormProvider
import ru.auto.feature.trade_in_form.viewmodel.TradeInSelectPriceViewModel
import rx.Single
import kotlin.test.assertEquals

@RunWith(AllureParametrizedRunner::class)
class TradeInFormFeatureTest(
    private val parameter: TradeInApplyInfo
): RxTest() {

    private val scalaApi: ScalaApi = mock()

    private val feature by lazy {
        TradeInFormProvider(
            args = ITradeInFormProvider.Args(
                offer = OFFER,
                tradeInPredict = TRADE_IN_PREDICT,
                source = ""
            ),
            dependencies = object : TradeInFormProvider.Dependencies {
                override val tradeInInteractor: ITradeInInteractor = TradeInInteractor(
                    TradeInRepository(scalaApi),
                    StatRepository(scalaApi)
                )
            },
            coordinatorProvider = ::TradeInFormCoordinator
        ).feature
    }

    @Before
    fun setup() {
        Analyst.init(mock())
    }

    @Test
    fun `trade in feature send apply if user select price`() {

        mockTradeInApply()

        val selectPriceModel = TradeInSelectPriceViewModel(
            id = parameter.priceRange.id,
            title = mock(),
            subtitle = mock(),
            isChecked = false
        )

        feature.run {
            accept(TradeInForm.Msg.OnPriceSelected(selectPriceModel))
            accept(TradeInForm.Msg.OnSendButtonClicked)
        }

        assertEquals(TradeInForm.State.ApplyFormState.LOADING, feature.currentState.applyFormState)

        verify(scalaApi).applyTradeIn(
            argWhere { request ->
                assertEquals(OFFER.id, request.offer?.id)
                assertEquals(parameter.tradeInType.name, request.offer?.trade_in_info?.trade_in_type?.name)
                assertEquals(parameter.priceRange.amountFrom, request.offer?.trade_in_info?.trade_in_price_range?.from)
                assertEquals(parameter.priceRange.amountTo, request.offer?.trade_in_info?.trade_in_price_range?.to)
                assertEquals(
                    parameter.priceRange.currency.name,
                    request.offer?.trade_in_info?.trade_in_price_range?.currency
                )
                true
            }
        )
    }

    private fun mockTradeInApply() {
        whenever(scalaApi.applyTradeIn(any())).thenReturn(Single.just(BaseResponse()))
    }

    companion object {

        @Parameterized.Parameters(name = "index={index}")
        @JvmStatic
        fun data(): Collection<Array<out Any?>> = listOf(
            arrayOf(
                TradeInApplyInfo(
                    TradeInType.FOR_MONEY,
                    TRADE_IN_PREDICT.tradeInBuyoutPrice
                )
            ),
            arrayOf(
                TradeInApplyInfo(
                    TradeInType.FOR_USED,
                    TRADE_IN_PREDICT.tradeInUsedPrice
                )
            ),
            arrayOf(
                TradeInApplyInfo(
                    TradeInType.FOR_NEW,
                    TRADE_IN_PREDICT.tradeInNewPrice
                )
            )
        )

        private val OFFER = Offer(
            id = "1",
            category = VehicleCategory.CARS,
            sellerType = SellerType.PRIVATE
        )

        private val TRADE_IN_PREDICT = TradeInPredict(
            tradeInBuyoutPrice = MoneyRange(
                amountFrom = 100_000L,
                amountTo = 150_000L,
                currency = Currency.RUR,
                medium = 0f,
                id = MoneyRange.TRADE_IN_BUYOUT
            ),
            tradeInUsedPrice = MoneyRange(
                amountFrom = 200_000L,
                amountTo = 250_000L,
                currency = Currency.RUR,
                medium = 0f,
                id = MoneyRange.TRADE_IN_USED
            ),
            tradeInNewPrice = MoneyRange(
                amountFrom = 300_000L,
                amountTo = 350_000L,
                currency = Currency.RUR,
                medium = 0f,
                id = MoneyRange.TRADE_IN_NEW
            )
        )
    }
}
