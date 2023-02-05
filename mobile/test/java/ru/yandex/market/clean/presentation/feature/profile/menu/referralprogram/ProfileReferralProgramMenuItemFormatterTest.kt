package ru.yandex.market.clean.presentation.feature.profile.menu.referralprogram

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramStatus_EnabledTestInstance
import ru.yandex.market.common.android.ResourcesManager

class ProfileReferralProgramMenuItemFormatterTest {

    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(R.string.profile_menu_referral_program) } doReturn EXPECTED_TITLE
        on {
            getFormattedString(
                R.string.profile_menu_referral_program_subtitle,
                BENEFIT
            )
        } doReturn BENEFIT_SUBTITLE
    }

    private val formatter = ProfileReferralProgramMenuItemFormatter(resourcesDataStore)

    @Test
    fun `format item without info`() {
        val expected = ReferralProgramMenuVo(EXPECTED_TITLE, null)
        val actual = formatter.formatSimple()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `format item with benefit`() {
        val expected = ReferralProgramMenuVo(EXPECTED_TITLE, BENEFIT_SUBTITLE)
        val actual = formatter.format(
            referralProgramStatus_EnabledTestInstance(
                isGotFullReward = false,
                refererReward = BENEFIT
            )
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `format item without benefit`() {
        val expected = ReferralProgramMenuVo(EXPECTED_TITLE, null)
        val actual = formatter.format(referralProgramStatus_EnabledTestInstance(isGotFullReward = true))

        assertThat(actual).isEqualTo(expected)
    }

    companion object {
        private const val EXPECTED_TITLE = "expected title"
        private const val BENEFIT = 300
        private const val BENEFIT_SUBTITLE = "with benefit 300"
    }
}