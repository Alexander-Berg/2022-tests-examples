package ru.yandex.market.data.cashback.mapper.order

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.data.cashback.network.dto.growingcashback.GrowingCashbackOptionDto
import ru.yandex.market.data.cashback.network.dto.growingcashback.growingCashbackOptionDtoTestInstance
import ru.yandex.market.domain.cashback.model.PossibleCashbackOption
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_GrowingCashbackTestInstance

@RunWith(Parameterized::class)
class GrowingCashbackPossibleOptionMapperTest(
    private val input: GrowingCashbackOptionDto?,
    private val expectedOutput: PossibleCashbackOption?
) {
    private val mapper = GrowingCashbackPossibleOptionMapper()

    @Test
    fun testMapping() {
        val result = mapper.map(input)
        Assertions.assertThat(result).isEqualTo(expectedOutput)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0 - нет дополнительного кэшбэка
            arrayOf(null, null as PossibleCashbackOption?),
            //1 - растущий кэшбэк без указания amount
            arrayOf(
                growingCashbackOptionDtoTestInstance(amount = null),
                null
            ),
            //2 - растущий кэшбэк без указания minMultiCartTotal
            arrayOf(
                growingCashbackOptionDtoTestInstance(minMultiCartTotal = null),
                null
            ),
            //3 - растущий кэшбэк без указания remainingMultiCartTotal
            arrayOf(
                growingCashbackOptionDtoTestInstance(remainingMultiCartTotal = null),
                null
            ),
            //4 - не задан приоритет отображения акции
            arrayOf(
                growingCashbackOptionDtoTestInstance(
                    agitationPriority = null,
                    remainingMultiCartTotal = 500,
                    minMultiCartTotal = 3500,
                    amount = 300,
                    promoKey = "growingPromo"
                ),
                possibleCashbackOption_GrowingCashbackTestInstance(
                    agitationPriority = 0,
                    amount = 300,
                    remainingMultiCartTotal = 500,
                    minMultiCartTotal = 3500,
                    promoKey = "growingPromo"
                )
            ),
            //5 - правильно заданный растущий кэшбэк
            arrayOf(
                growingCashbackOptionDtoTestInstance(
                    agitationPriority = 10,
                    remainingMultiCartTotal = 500,
                    minMultiCartTotal = 3500,
                    amount = 300,
                    promoKey = "growingPromo"
                ),
                possibleCashbackOption_GrowingCashbackTestInstance(
                    agitationPriority = 10,
                    amount = 300,
                    remainingMultiCartTotal = 500,
                    minMultiCartTotal = 3500,
                    promoKey = "growingPromo"
                )
            ),
        )
    }
}
