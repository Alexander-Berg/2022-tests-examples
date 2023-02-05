package ru.yandex.market.clean.data.mapper

import com.annimon.stream.Exceptional
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.data.money.dto.PriceDto
import ru.yandex.market.clean.data.model.dto.YandexHelpSubscriptionStatusDto
import ru.yandex.market.clean.domain.model.HelpIsNearSubscriptionStatus
import ru.yandex.market.clean.data.mapper.money.MoneyMapper
import ru.yandex.market.domain.money.model.Money
import java.math.BigDecimal

@RunWith(Parameterized::class)
class HelpIsNearSubscriptionStatusMapperTest(
    private val input: YandexHelpSubscriptionStatusDto,
    private val expectedOutput: HelpIsNearSubscriptionStatus
) {

    private val moneyMapper = mock<MoneyMapper> {
        if (input.donatedTotal != null) {
            on { map(input.donatedTotal!!) } doReturn Exceptional.of {
                Money.createRub(input.donatedTotal!!.value!!)
            }
        }
    }
    private val mapper = HelpIsNearSubscriptionStatusMapper(moneyMapper)

    @Test
    fun testMapping() {
        val result = mapper.map(input)

        Assertions.assertThat(result).isEqualTo(expectedOutput)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                YandexHelpSubscriptionStatusDto(isSubscribed = null, donatedTotal = null),
                HelpIsNearSubscriptionStatus(isSubscribed = false, donatedTotal = Money.zeroRub())
            ),
            //1
            arrayOf(
                YandexHelpSubscriptionStatusDto(isSubscribed = true, donatedTotal = PriceDto(BigDecimal.TEN, "rub")),
                HelpIsNearSubscriptionStatus(isSubscribed = true, donatedTotal = Money.createRub(10))
            ),
            //2
            arrayOf(
                YandexHelpSubscriptionStatusDto(isSubscribed = false, donatedTotal = PriceDto(BigDecimal.ONE, null)),
                HelpIsNearSubscriptionStatus(isSubscribed = false, donatedTotal = Money.createRub(1))
            ),
        )
    }
}
