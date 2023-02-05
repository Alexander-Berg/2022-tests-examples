package ru.yandex.market.clean.presentation.feature.referralprogram

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramStatus_EnabledTestInstance
import ru.yandex.market.common.android.ResourcesManager

class ReferralProgramEntryPointFormatterTest {

    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(R.string.referral_program_can_not_get_entry_point) } doReturn TITLE_CAN_NOT_GET_BENEFIT
        on { getFormattedString(R.string.referral_program_can_get_entry_point, BENEFIT) } doReturn TITLE_CAN_GET_BENEFIT
    }

    private val referralProgramEntryPointFormatter = ReferralProgramEntryPointFormatter(resourcesDataStore)

    @Test
    fun `format can get benefit entry point`() {
        val status = referralProgramStatus_EnabledTestInstance(refererReward = BENEFIT, isGotFullReward = false)

        assertThat(referralProgramEntryPointFormatter.format(status)).isEqualTo(TITLE_CAN_GET_BENEFIT)
    }

    @Test
    fun `format can not get benefit entry point`() {
        val status = referralProgramStatus_EnabledTestInstance(refererReward = BENEFIT, isGotFullReward = true)

        assertThat(referralProgramEntryPointFormatter.format(status)).isEqualTo(TITLE_CAN_NOT_GET_BENEFIT)
    }

    companion object {
        private const val BENEFIT = 300
        private const val TITLE_CAN_GET_BENEFIT = "Рекомендуй друга и 300 баллов"
        private const val TITLE_CAN_NOT_GET_BENEFIT = "Рекомендуй друга"
    }
}