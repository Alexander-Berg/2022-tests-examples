package ru.yandex.market.clean.data.mapper.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.domain.model.order.HelpIsNearInfo
import ru.yandex.market.clean.domain.model.order.helpIsNearInfoTestInstance
import ru.yandex.market.data.order.YandexHelpInfoDto
import ru.yandex.market.data.order.yandexHelpInfoDtoTestInstance
import java.math.BigDecimal

@RunWith(Parameterized::class)
class HelpIsNearInfoMapperTest(
    private val input: YandexHelpInfoDto?,
    private val expectedOutput: HelpIsNearInfo?
) {
    private val mapper = HelpIsNearInfoMapper()

    @Test
    fun testMapping() {
        val result = mapper.map(input)

        assertThat(result).isEqualTo(expectedOutput)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                null as YandexHelpInfoDto?,
                null as HelpIsNearInfo?
            ),
            //1
            arrayOf(
                yandexHelpInfoDtoTestInstance(yandexHelpStatus = "DISABLED"),
                helpIsNearInfoTestInstance(status = HelpIsNearInfo.Status.DISABLED)
            ),
            //2
            arrayOf(
                yandexHelpInfoDtoTestInstance(yandexHelpStatus = "ENABLED"),
                helpIsNearInfoTestInstance(status = HelpIsNearInfo.Status.ENABLED)
            ),
            //3
            arrayOf(
                yandexHelpInfoDtoTestInstance(yandexHelpStatus = "ROUNDING_ERROR"),
                helpIsNearInfoTestInstance(status = HelpIsNearInfo.Status.ROUNDING_ERROR)
            ),
            //4
            arrayOf(
                yandexHelpInfoDtoTestInstance(yandexHelpStatus = "UNKNOWN"),
                helpIsNearInfoTestInstance(status = HelpIsNearInfo.Status.UNKNOWN)
            ),
            //5
            arrayOf(
                yandexHelpInfoDtoTestInstance(yandexHelpStatus = ""),
                helpIsNearInfoTestInstance(status = HelpIsNearInfo.Status.UNKNOWN)
            ),
            //6
            arrayOf(
                yandexHelpInfoDtoTestInstance(yandexHelpStatus = "NOT_SUPPORTED_STATUS"),
                helpIsNearInfoTestInstance(status = HelpIsNearInfo.Status.UNKNOWN)
            ),
            //7
            arrayOf(
                yandexHelpInfoDtoTestInstance(yandexHelpDonationAmount = BigDecimal(12345)),
                helpIsNearInfoTestInstance(status = HelpIsNearInfo.Status.UNKNOWN, donationAmount = BigDecimal(12345))
            ),
            //8
            arrayOf(
                yandexHelpInfoDtoTestInstance(yandexHelpDonationAmount = null),
                helpIsNearInfoTestInstance(status = HelpIsNearInfo.Status.UNKNOWN, donationAmount = BigDecimal.ZERO)
            ),
        )
    }
}