package ru.yandex.market.clean.presentation.feature.purchasebylist.map.chips.outlet

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.chips.outlet.OutletSpeedType
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.chips.outlet.PurchaseByListOutletChipFormatter
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.chips.outlet.PurchaseByListOutletChipVo
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.utils.Characters

@RunWith(Parameterized::class)
class PurchaseByListOutletChipFormatterTest(
    private val outletSpeedTypes: Set<OutletSpeedType>,
    private val expectedOutput: List<PurchaseByListOutletChipVo>,
) {

    private val resource = mock<ResourcesManager> {
        on { getString(R.string.in_one_two_hours) } doReturn IN_ONE_TWO_HOURS
        on { getString(R.string.from_one_to_three_days) } doReturn FROM_ONE_TO_THREE_DAYS
    }

    @Test
    fun `Test outlet chip formatting`() {
        val formatter = PurchaseByListOutletChipFormatter(resource)
        val result = formatter.format(outletSpeedTypes, null)
        assertThat(result).isEqualTo(expectedOutput)
    }

    private companion object {
        const val IN_ONE_TWO_HOURS = "Через 1${Characters.EN_DASH}2 часа"
        const val FROM_ONE_TO_THREE_DAYS = "От 1 до 3 дней"

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            // показываем выбранный букинг
            // показываем не выбранный от 1 до 3 дней
            arrayOf(
                setOf(
                    OutletSpeedType.BOOKING,
                    OutletSpeedType.TODAY,
                    OutletSpeedType.TOMORROW,
                    OutletSpeedType.IN_ONE_THREE_DAYS,
                ),
                listOf(
                    PurchaseByListOutletChipVo(
                        outletSpeedType = OutletSpeedType.BOOKING,
                        isSelected = false,
                        title = IN_ONE_TWO_HOURS,
                    ),
                    PurchaseByListOutletChipVo(
                        outletSpeedType = OutletSpeedType.IN_ONE_THREE_DAYS,
                        isSelected = false,
                        title = FROM_ONE_TO_THREE_DAYS,
                    )
                )
            ),

            // показываем выбранный букинг
            // показываем не выбранный от 1 до 3 дней
            arrayOf(
                setOf(
                    OutletSpeedType.BOOKING,
                    OutletSpeedType.IN_ONE_THREE_DAYS,
                ),
                listOf(
                    PurchaseByListOutletChipVo(
                        outletSpeedType = OutletSpeedType.BOOKING,
                        isSelected = false,
                        title = IN_ONE_TWO_HOURS,
                    ),
                    PurchaseByListOutletChipVo(
                        outletSpeedType = OutletSpeedType.IN_ONE_THREE_DAYS,
                        isSelected = false,
                        title = FROM_ONE_TO_THREE_DAYS,
                    )
                )
            ),

            // показываем выбранный от 1 до 3 дней
            arrayOf(
                setOf(
                    OutletSpeedType.IN_ONE_THREE_DAYS,
                ),
                listOf(
                    PurchaseByListOutletChipVo(
                        outletSpeedType = OutletSpeedType.IN_ONE_THREE_DAYS,
                        isSelected = false,
                        title = FROM_ONE_TO_THREE_DAYS,
                    )
                )
            ),
        )
    }
}
