package ru.yandex.market.data.cashback.mapper.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.data.cashback.network.dto.order.cashbackDtoTestInstance
import ru.yandex.market.domain.cashback.model.PossibleCashbackOption

class PossibleCashbackOptionsMapperTest {

    private val welcomeCashback = mock<PossibleCashbackOption.WelcomeCashback>()
    private val paymentSystemCashback = mock<PossibleCashbackOption.PaymentSystemCashback>()
    private val growingCashbackCashback = mock<PossibleCashbackOption.GrowingCashback>()
    private val yandexCardCashback = mock<PossibleCashbackOption.YandexCardCashback>()
    private val cashbackDto = cashbackDtoTestInstance()

    private val welcomeCashbackPossibleOptionMapper = mock<WelcomeCashbackPossibleOptionMapper> {
        on { map(cashbackDto.welcomeCashback) } doReturn welcomeCashback
    }
    private val paymentSystemPossibleOptionMapper = mock<PaymentSystemPossibleOptionMapper> {
        on { map(cashbackDto.paymentSystemCashback) } doReturn paymentSystemCashback
    }
    private val growingCashbackPossibleOptionMapper = mock<GrowingCashbackPossibleOptionMapper> {
        on { map(cashbackDto.growingCashback) } doReturn growingCashbackCashback
    }
    private val yandexCardPossibleOptionMapper = mock<YandexCardPossibleOptionMapper> {
        on { map(cashbackDto.yandexCardCashback) } doReturn yandexCardCashback
    }

    private val mapper = PossibleCashbackOptionsMapper(
        welcomeCashbackPossibleOptionMapper,
        paymentSystemPossibleOptionMapper,
        growingCashbackPossibleOptionMapper,
        yandexCardPossibleOptionMapper
    )

    @Test
    fun testMap() {
        val actual = mapper.map(cashbackDto)

        assertThat(actual).isEqualTo(
            listOf(
                welcomeCashback,
                paymentSystemCashback,
                growingCashbackCashback,
                yandexCardCashback
            )
        )
    }

}
