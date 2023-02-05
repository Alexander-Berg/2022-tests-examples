package ru.yandex.market.clean.presentation.feature.referralprogram.onboarding

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.referralprogram.ReferralProgramInfo
import ru.yandex.market.clean.domain.model.referralprogram.partnerProgramInfo_EnabledTestInstance
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramInfo_EnabledTestInstance
import ru.yandex.market.clean.domain.model.referralprogram.referralPromocodeBenefit_MoneyNominalTestInstance
import ru.yandex.market.clean.domain.model.referralprogram.referralPromocodeBenefit_OrderPercentDiscountTestInstance
import ru.yandex.market.clean.presentation.feature.referralprogram.partner.partnerProgramTestInstance
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.common.errors.common.CommonErrorPresentation
import ru.yandex.market.common.errors.common.CommonErrorVo
import ru.yandex.market.domain.money.model.Money
import java.util.GregorianCalendar

@RunWith(Enclosed::class)
class ReferralProgramOnboardingFormatterTest {

    @RunWith(Parameterized::class)
    class TestFormatEnabled(
        private val promocode: ReferralProgramInfo.Enabled,
        private val expected: ReferralProgramOnboardingVo
    ) {
        private val referralProgramBenefitFormatter = mock<ReferralPromocodeBenefitFormatter> {
            on { format(PERCENT_DISCOUNT) } doReturn PERCENT_DISCOUNT_VALUE
            on { format(MONEY_DISCOUNT) } doReturn MONEY_DISCOUNT_VALUE
        }
        private val resourcesDataStore = mock<ResourcesManager> {
            on {
                getQuantityString(
                    R.plurals.referral_program_onboarding_total_statistic,
                    DEFAULT_VALUE
                )
            } doReturn DEFAULT
            on {
                getQuantityString(
                    R.plurals.referral_program_onboarding_friends_statistic,
                    DEFAULT_VALUE
                )
            } doReturn DEFAULT
            on {
                getQuantityString(
                    R.plurals.referral_program_onboarding_expected_statistic,
                    DEFAULT_VALUE
                )
            } doReturn DEFAULT

            on {
                getFormattedString(
                    R.string.referral_program_money_benefit,
                    "1 000"
                )
            } doReturn PARTNER_REWARD
        }
        private val formatter = ReferralProgramOnboardingFormatter(resourcesDataStore, referralProgramBenefitFormatter)

        @Test
        fun testFormat() {
            val result = formatter.formatEnabled(promocode)

            assertThat(result).isEqualTo(expected)
        }

        companion object {

            private val PERCENT_DISCOUNT =
                referralPromocodeBenefit_OrderPercentDiscountTestInstance(10)
            private val MONEY_DISCOUNT = referralPromocodeBenefit_MoneyNominalTestInstance(
                Money.Companion.createRub(
                    500
                )
            )
            private val PARTNER_INFO =
                partnerProgramInfo_EnabledTestInstance(
                    Money.Companion.createRub(
                        1000
                    )
                )
            private const val PERCENT_DISCOUNT_VALUE = "10 %"
            private const val MONEY_DISCOUNT_VALUE = "500 ₽"
            private const val DEFAULT_VALUE = 100
            private const val DEFAULT = "100"
            private const val PARTNER_REWARD = "1 000₽"

            @Parameterized.Parameters(name = "{index}: {0} -> {1}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                arrayOf(
                    referralProgramInfo_EnabledTestInstance(
                        promocodeBenefit = PERCENT_DISCOUNT,
                        refererReward = 200,
                        isGotFullReward = false,
                        promocode = "promo10",
                        expiredDate = GregorianCalendar(2019, 1, 10, 0, 0, 0).time,
                        alreadyGot = DEFAULT_VALUE,
                        expectedCashback = DEFAULT_VALUE,
                        friendsOrdered = DEFAULT_VALUE,
                        maxRefererReward = DEFAULT_VALUE,
                        partnerProgramInfo = PARTNER_INFO,
                        minPromocodeOrderCost = Money.createRub(1000)
                    ),
                    referralProgramOnboardingVoTestInstance(
                        promocode = "promo10",
                        promocodeDiscount = PERCENT_DISCOUNT_VALUE,
                        promocodeExpiredDate = "10 февраля",
                        benefit = 200,
                        minPromocodeOrderCost = PARTNER_REWARD,
                        alreadyGot = referralProgramStatisticItemTestInstance(DEFAULT_VALUE, DEFAULT),
                        expectedCashback = referralProgramStatisticItemTestInstance(DEFAULT_VALUE, DEFAULT),
                        friendsOrdered = referralProgramStatisticItemTestInstance(DEFAULT_VALUE, DEFAULT),
                        maxRefererReward = DEFAULT,
                        isGotFullReward = false,
                        partnerProgramInfo = partnerProgramTestInstance(PARTNER_REWARD)
                    )
                ),
                arrayOf(
                    referralProgramInfo_EnabledTestInstance(
                        promocodeBenefit = PERCENT_DISCOUNT,
                        refererReward = 300,
                        isGotFullReward = false,
                        promocode = "promo20",
                        expiredDate = GregorianCalendar(2019, 5, 20, 0, 0, 0).time,
                        alreadyGot = DEFAULT_VALUE,
                        expectedCashback = DEFAULT_VALUE,
                        friendsOrdered = DEFAULT_VALUE,
                        maxRefererReward = DEFAULT_VALUE,
                        partnerProgramInfo = PARTNER_INFO,
                        minPromocodeOrderCost = Money.createRub(1000)
                    ),
                    referralProgramOnboardingVoTestInstance(
                        promocode = "promo20",
                        promocodeDiscount = PERCENT_DISCOUNT_VALUE,
                        promocodeExpiredDate = "20 июня",
                        benefit = 300,
                        alreadyGot = referralProgramStatisticItemTestInstance(DEFAULT_VALUE, DEFAULT),
                        expectedCashback = referralProgramStatisticItemTestInstance(DEFAULT_VALUE, DEFAULT),
                        friendsOrdered = referralProgramStatisticItemTestInstance(DEFAULT_VALUE, DEFAULT),
                        maxRefererReward = DEFAULT,
                        isGotFullReward = false,
                        partnerProgramInfo = partnerProgramTestInstance(PARTNER_REWARD),
                        minPromocodeOrderCost = PARTNER_REWARD
                    )
                ),
                arrayOf(
                    referralProgramInfo_EnabledTestInstance(
                        promocodeBenefit = MONEY_DISCOUNT,
                        isGotFullReward = true,
                        promocode = "promo30",
                        expiredDate = GregorianCalendar(2019, 11, 15, 0, 0, 0).time,
                        alreadyGot = DEFAULT_VALUE,
                        expectedCashback = DEFAULT_VALUE,
                        friendsOrdered = DEFAULT_VALUE,
                        maxRefererReward = DEFAULT_VALUE,
                        partnerProgramInfo = PARTNER_INFO,
                        minPromocodeOrderCost = Money.createRub(1000)
                    ),
                    referralProgramOnboardingVoTestInstance(
                        promocode = "promo30",
                        promocodeDiscount = MONEY_DISCOUNT_VALUE,
                        promocodeExpiredDate = "15 декабря",
                        benefit = null,
                        alreadyGot = referralProgramStatisticItemTestInstance(DEFAULT_VALUE, DEFAULT),
                        expectedCashback = referralProgramStatisticItemTestInstance(DEFAULT_VALUE, DEFAULT),
                        friendsOrdered = referralProgramStatisticItemTestInstance(DEFAULT_VALUE, DEFAULT),
                        maxRefererReward = DEFAULT,
                        isGotFullReward = true,
                        partnerProgramInfo = partnerProgramTestInstance(PARTNER_REWARD),
                        minPromocodeOrderCost = PARTNER_REWARD
                    )
                )
            )
        }
    }

    class TestFormatDisabled {
        private val resourcesDataStore = mock<ResourcesManager> {
            on { getString(R.string.referral_program_promo_expired_title) } doReturn TITLE
            on { getString(R.string.referral_program_promo_expired_subtitle) } doReturn SUBTITLE
            on { getString(R.string.referral_program_promo_expired_action) } doReturn ACTION
        }

        private val formatter = ReferralProgramOnboardingFormatter(resourcesDataStore, mock())

        @Test
        fun testFormat() {
            val buttonAction: () -> Unit = {}
            val cause = Throwable()
            val expected = CommonErrorVo(
                presentationType = CommonErrorPresentation.GENERAL,
                title = TITLE,
                subTitle = SUBTITLE,
                image = CommonErrorVo.ImageKind.NEUTRAL,
                positiveAction = CommonErrorVo.ActionDescription(
                    title = ACTION,
                    onInvoke = buttonAction
                ),
                negativeAction = null,
                customImageRes = R.drawable.ic_promocode_error,
                cause = cause
            )
            val actual = formatter.formatDisabled(cause, buttonAction)

            assertThat(actual).isEqualTo(expected)

        }

        companion object {
            private const val TITLE = "disabled title"
            private const val SUBTITLE = "disabled subtitle"
            private const val ACTION = "disabled action"
        }
    }
}