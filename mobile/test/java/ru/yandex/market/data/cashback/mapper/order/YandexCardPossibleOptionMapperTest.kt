package ru.yandex.market.data.cashback.mapper.order

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.data.cashback.network.dto.order.YandexCardCashbackDto
import ru.yandex.market.data.cashback.network.dto.order.yandexCardCashbackDtoTestInstance
import ru.yandex.market.domain.cashback.model.PossibleCashbackOption
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_YandexCardCashbackTestInstance
import java.math.BigDecimal

@RunWith(Parameterized::class)
class YandexCardPossibleOptionMapperTest(
    private val input: YandexCardCashbackDto?,
    private val expectedOutput: PossibleCashbackOption.YandexCardCashback?
) {
    private val mapper = YandexCardPossibleOptionMapper()

    @Test
    fun testMapping() {
        val result = mapper.map(input)
        Assertions.assertThat(result).isEqualTo(expectedOutput)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0 - нет кэшбэка за карту
            arrayOf(null, null as PossibleCashbackOption.YandexCardCashback?),
            //1 - кэшбэк без указания amount
            arrayOf(
                yandexCardCashbackDtoTestInstance(amount = null),
                null
            ),
            //2 - кэшбэк без указания процента
            arrayOf(
                yandexCardCashbackDtoTestInstance(cashbackPercent = null),
                null
            ),
            //3 - кэшбэк без указания максимальной суммы заказа
            arrayOf(
                yandexCardCashbackDtoTestInstance(maxOrderTotal = null),
                null
            ),
            //3 - кэшбэк без указания promoKey
            arrayOf(
                yandexCardCashbackDtoTestInstance(
                    amount = BigDecimal(123),
                    cashbackPercent = 5,
                    maxOrderTotal = BigDecimal(154000),
                    promoKey = null
                ),
                possibleCashbackOption_YandexCardCashbackTestInstance(
                    cashbackAmount = BigDecimal(123),
                    percentValue = 5,
                    maxOrderTotal = BigDecimal(154000),
                    promoKey = ""
                )
            ),
            //4 - кэшбэк без указания agitationPriority
            arrayOf(
                yandexCardCashbackDtoTestInstance(
                    amount = BigDecimal(234),
                    cashbackPercent = 10,
                    maxOrderTotal = BigDecimal(8000000),
                    promoKey = "yandex card promo",
                    agitationPriority = null
                ),
                possibleCashbackOption_YandexCardCashbackTestInstance(
                    cashbackAmount = BigDecimal(234),
                    percentValue = 10,
                    maxOrderTotal = BigDecimal(8000000),
                    promoKey = "yandex card promo",
                    agitationPriority = 0,
                )
            ),
            //5 - правильно заданный кэшбэк
            arrayOf(
                yandexCardCashbackDtoTestInstance(
                    amount = BigDecimal(234),
                    cashbackPercent = 10,
                    maxOrderTotal = BigDecimal(8000000),
                    promoKey = "yandex card promo",
                    agitationPriority = 20
                ),
                possibleCashbackOption_YandexCardCashbackTestInstance(
                    cashbackAmount = BigDecimal(234),
                    percentValue = 10,
                    maxOrderTotal = BigDecimal(8000000),
                    promoKey = "yandex card promo",
                    agitationPriority = 20
                )
            ),
        )
    }
}
