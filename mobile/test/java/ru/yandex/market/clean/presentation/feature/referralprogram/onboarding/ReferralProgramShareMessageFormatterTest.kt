package ru.yandex.market.clean.presentation.feature.referralprogram.onboarding

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramInfo_EnabledTestInstance
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.domain.money.model.moneyTestInstance
import ru.yandex.market.domain.money.model.amountTestInstance
import java.math.BigDecimal
import java.util.GregorianCalendar

class ReferralProgramShareMessageFormatterTest {

    private val resourceDataSource = mock<ResourcesManager> {
        on { getFormattedString(eq(R.string.referral_program_share_message), anyVararg()) } doReturn SHARE_MESSAGE
    }

    private val referralPromocodeBenefitFormatter = mock<ReferralPromocodeBenefitFormatter> {
        on { format(any()) } doReturn PROMOCODE_BENEFIT
    }
    private val referralProgramShareMessageFormatter = ReferralProgramShareMessageFormatter(
        resourceDataSource,
        referralPromocodeBenefitFormatter
    )

    @Test
    fun `get formatted string from resources with correct arguments`() {
        val refferalAmount = amountTestInstance(BigDecimal(3000))
        val referralProgramInfo = referralProgramInfo_EnabledTestInstance(
            expiredDate = GregorianCalendar(2019, 10, 18, 16, 0, 0).time,
            minPromocodeOrderCost = moneyTestInstance().copy(amount = refferalAmount)
        )

        val result = referralProgramShareMessageFormatter.format(referralProgramInfo)

        verify(resourceDataSource).getFormattedString(
            R.string.referral_program_share_message,
            PROMOCODE_BENEFIT,
            "3 000",
            referralProgramInfo.promocode,
            "18 ноября",
            referralProgramInfo.referrerLink
        )
        assertThat(result).isEqualTo(SHARE_MESSAGE)
    }

    companion object {
        private const val PROMOCODE_BENEFIT = "10 %"
        private const val SHARE_MESSAGE = "share message"
    }
}