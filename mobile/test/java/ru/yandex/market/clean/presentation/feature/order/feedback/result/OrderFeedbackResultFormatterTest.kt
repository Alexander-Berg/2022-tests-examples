package ru.yandex.market.clean.presentation.feature.order.feedback.result

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.orderfeedback.OrderFeedbackScenario
import ru.yandex.market.clean.domain.model.referralprogram.ReferralProgramStatus
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramStatus_EnabledTestInstance
import ru.yandex.market.clean.presentation.feature.order.feedback.result.vo.OrderFeedbackResultVo
import ru.yandex.market.clean.presentation.feature.referralprogram.ReferralProgramEntryPointFormatter
import ru.yandex.market.common.android.ResourcesManager

@RunWith(Parameterized::class)
class OrderFeedbackResultFormatterTest(
    private val scenario: OrderFeedbackScenario,
    private val referralProgramStatus: ReferralProgramStatus,
    private val expectedOutput: OrderFeedbackResultVo
) {

    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(R.string.order_feedback_result_title) } doReturn TITLE
        on { getString(R.string.order_feedback_result_subtitle_thank_you) } doReturn POSITIVE_SUBTITLE
        on { getString(R.string.order_feedback_result_primary_button_to_pruchases) } doReturn TO_PURCHASE
        on { getString(R.string.order_feedback_result_button_continue) } doReturn CONTINUE
        on { getString(R.string.order_feedback_result_subtitle_sorry) } doReturn SORRY_SUBTITLE
        on { getString(R.string.order_feedback_result_subtitle_sorry_refund) } doReturn SORRY_REFUND
        on { getString(R.string.order_feedback_result_subtitle_sorry_support) } doReturn SORRY_SUPPORT
        on { getString(R.string.order_feedback_result_primary_button_refund) } doReturn RETURN_ORDER
        on { getString(R.string.order_feedback_result_primary_button_support) } doReturn CONTACT_SUPPORT
    }
    private val referralProgramEntryPointFormatter = mock<ReferralProgramEntryPointFormatter> {
        if (referralProgramStatus is ReferralProgramStatus.Enabled) {
            on { format(referralProgramStatus) } doReturn RECOMMEND
        }
    }
    private val formatter = OrderFeedbackResultFormatter(resourcesDataStore, referralProgramEntryPointFormatter)

    @Test
    fun testFormat() {
        Assertions.assertThat(formatter.format(scenario, referralProgramStatus)).isEqualTo(expectedOutput)
    }

    companion object {

        private const val TITLE = "Спасибо за оценку"
        private const val POSITIVE_SUBTITLE = "Мы это очень ценим."
        private const val CONTINUE = "Продолжить"
        private const val TO_PURCHASE = "К покупкам"
        private const val RECOMMEND = "Рекомендовать маркет"
        private const val SORRY_SUBTITLE = "Простите, что огорчили"
        private const val SORRY_REFUND = "Простите, что повреждён"
        private const val SORRY_SUPPORT = "Простите, обратитесь в поддержку"
        private const val RETURN_ORDER = "Вернуть товар"
        private const val CONTACT_SUPPORT = "Обратитесь в поддержку"

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                OrderFeedbackScenario.THANK_YOU,
                referralProgramStatus_EnabledTestInstance(),
                OrderFeedbackResultVo(
                    isShowOkImage = true,
                    title = TITLE,
                    subTitle = POSITIVE_SUBTITLE,
                    primaryButtonText = TO_PURCHASE,
                    secondaryActionVo = OrderFeedbackResultVo.SecondaryActionVo(
                        buttonText = RECOMMEND,
                        buttonAction = OrderFeedbackResultVo.ButtonAction.REFERRAL_PROGRAM
                    )

                )
            ),
            //1
            arrayOf(
                OrderFeedbackScenario.THANK_YOU,
                ReferralProgramStatus.Disabled,
                OrderFeedbackResultVo(
                    isShowOkImage = true,
                    title = TITLE,
                    subTitle = POSITIVE_SUBTITLE,
                    primaryButtonText = TO_PURCHASE,
                    secondaryActionVo = null

                )
            ),
            //2
            arrayOf(
                OrderFeedbackScenario.SORRY,
                ReferralProgramStatus.Disabled,
                OrderFeedbackResultVo(
                    isShowOkImage = false,
                    title = TITLE,
                    subTitle = SORRY_SUBTITLE,
                    primaryButtonText = CONTINUE,
                    secondaryActionVo = null

                )
            ),
            //3
            arrayOf(
                OrderFeedbackScenario.SORRY,
                referralProgramStatus_EnabledTestInstance(),
                OrderFeedbackResultVo(
                    isShowOkImage = false,
                    title = TITLE,
                    subTitle = SORRY_SUBTITLE,
                    primaryButtonText = CONTINUE,
                    secondaryActionVo = null
                )
            ),
            //4
            arrayOf(
                OrderFeedbackScenario.SORRY_REFUND,
                referralProgramStatus_EnabledTestInstance(),
                OrderFeedbackResultVo(
                    isShowOkImage = false,
                    title = TITLE,
                    subTitle = SORRY_REFUND,
                    primaryButtonText = RETURN_ORDER,
                    secondaryActionVo = OrderFeedbackResultVo.SecondaryActionVo(
                        buttonText = CONTINUE,
                        buttonAction = OrderFeedbackResultVo.ButtonAction.CLOSE
                    )
                )
            ),
            //5
            arrayOf(
                OrderFeedbackScenario.SORRY_REFUND,
                ReferralProgramStatus.Disabled,
                OrderFeedbackResultVo(
                    isShowOkImage = false,
                    title = TITLE,
                    subTitle = SORRY_REFUND,
                    primaryButtonText = RETURN_ORDER,
                    secondaryActionVo = OrderFeedbackResultVo.SecondaryActionVo(
                        buttonText = CONTINUE,
                        buttonAction = OrderFeedbackResultVo.ButtonAction.CLOSE
                    )
                )
            ),
            //6
            arrayOf(
                OrderFeedbackScenario.SORRY_SUPPORT,
                referralProgramStatus_EnabledTestInstance(),
                OrderFeedbackResultVo(
                    isShowOkImage = false,
                    title = TITLE,
                    subTitle = SORRY_SUPPORT,
                    primaryButtonText = CONTACT_SUPPORT,
                    secondaryActionVo = OrderFeedbackResultVo.SecondaryActionVo(
                        buttonText = CONTINUE,
                        buttonAction = OrderFeedbackResultVo.ButtonAction.CLOSE
                    )
                )
            ),
            //7
            arrayOf(
                OrderFeedbackScenario.SORRY_SUPPORT,
                referralProgramStatus_EnabledTestInstance(),
                OrderFeedbackResultVo(
                    isShowOkImage = false,
                    title = TITLE,
                    subTitle = SORRY_SUPPORT,
                    primaryButtonText = CONTACT_SUPPORT,
                    secondaryActionVo = OrderFeedbackResultVo.SecondaryActionVo(
                        buttonText = CONTINUE,
                        buttonAction = OrderFeedbackResultVo.ButtonAction.CLOSE
                    )
                )
            ),
        )
    }
}