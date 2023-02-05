package ru.yandex.market.data.cashback.mapper.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.data.cashback.network.dto.order.WelcomeCashbackDto
import ru.yandex.market.data.cashback.network.dto.order.welcomeCashbackDtoTestInstance
import ru.yandex.market.domain.cashback.model.PossibleCashbackOption
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_WelcomeCashbackTestInstance

@RunWith(Parameterized::class)
class WelcomeCashbackPossibleOptionMapperTest(
    private val input: WelcomeCashbackDto?,
    private val expectedOutput: PossibleCashbackOption?
) {

    private val mapper = WelcomeCashbackPossibleOptionMapper()

    @Test
    fun testMapping() {
        val result = mapper.map(input)
        assertThat(result).isEqualTo(expectedOutput)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0 - нет дополнительного кэшбэка
            arrayOf(null, null as PossibleCashbackOption?),
            //1 - кэшбэк за первый заказ с не указанным amount
            arrayOf(
                welcomeCashbackDtoTestInstance(amount = null),
                null
            ),
            //2 - кэшбэк за первый заказ с не указанным minMultiCartTotal
            arrayOf(
                welcomeCashbackDtoTestInstance(minMultiCartTotal = null),
                null
            ),
            //3 - кэшбэк за первый заказ с не указанным remainingMultiCartTotal
            arrayOf(
                welcomeCashbackDtoTestInstance(remainingMultiCartTotal = null),
                null
            ),
            //4 - не задан приоритет отображения акции
            arrayOf(
                welcomeCashbackDtoTestInstance(
                    agitationPriority = null,
                    remainingMultiCartTotal = 200,
                    minMultiCartTotal = 4000,
                    amount = 500,
                    promoKey = "rj45"
                ),
                possibleCashbackOption_WelcomeCashbackTestInstance(
                    agitationPriority = 0,
                    amount = 500,
                    remainingMultiCartTotal = 200,
                    minMultiCartTotal = 4000,
                    promoKey = "rj45"
                )
            ),
            //5 - правильно заданный кэшбэк за первый заказ
            arrayOf(
                welcomeCashbackDtoTestInstance(
                    agitationPriority = 20,
                    remainingMultiCartTotal = 200,
                    minMultiCartTotal = 4000,
                    amount = 500,
                    promoKey = "rj45"
                ),
                possibleCashbackOption_WelcomeCashbackTestInstance(
                    agitationPriority = 20,
                    amount = 500,
                    remainingMultiCartTotal = 200,
                    minMultiCartTotal = 4000,
                    promoKey = "rj45"
                )
            ),
        )
    }
}
