package ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.formatter

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.base.network.common.Response
import ru.yandex.market.base.network.common.exception.CommunicationException
import ru.yandex.market.clean.domain.model.referralprogram.ReferralProgramStatus
import ru.yandex.market.clean.domain.model.referralprogram.referralProgramStatus_EnabledTestInstance
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.vo.GrowingCashbackActionVo
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.vo.GrowingCashbackInfoButtonVo
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.domain.cashback.model.GrowingCashbackActionInfo
import ru.yandex.market.domain.cashback.model.growingCashbackActionInfoTestInstance
import ru.yandex.market.domain.cashback.model.growingCashbackActionStateTestInstance

@RunWith(Enclosed::class)
class GrowingCashbackInfoFormatterTest {

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
    class CompletedStateTest(
        private val actionInfo: GrowingCashbackActionInfo,
        private val referralStatus: ReferralProgramStatus,
        private val expected: GrowingCashbackActionVo.GrowingCashbackInfoVo
    ) {

        private val formatter = GrowingCashbackInfoFormatter(
            ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources)
        )

        @Test
        fun testFormat() {
            val actual = formatter.formatComplete(actionInfo, referralStatus)
            assertThat(actual).isEqualTo(expected)
        }

        companion object {

            @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0} -> {1}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                //0
                arrayOf(
                    growingCashbackActionInfoTestInstance(
                        actionState = growingCashbackActionStateTestInstance(maxReward = 1550)
                    ),
                    referralProgramStatus_EnabledTestInstance(refererReward = 300),
                    GrowingCashbackActionVo.GrowingCashbackInfoVo(
                        image = GrowingCashbackActionVo.Image.REWARD,
                        title = "Вы получили 1 550 баллов — максимум для этой акции",
                        subtitle = "Порекомендуйте нас друзьям и получите ещё :image: :color:300:color: баллов Плюса за каждого",
                        button = GrowingCashbackInfoButtonVo(
                            title = "Порекомендовать Маркет",
                            action = GrowingCashbackInfoButtonVo.ButtonAction.REFERRAL
                        )
                    )
                ),
                //1
                arrayOf(
                    growingCashbackActionInfoTestInstance(
                        actionState = growingCashbackActionStateTestInstance(maxReward = 1551)
                    ),
                    ReferralProgramStatus.Disabled,
                    GrowingCashbackActionVo.GrowingCashbackInfoVo(
                        image = GrowingCashbackActionVo.Image.REWARD,
                        title = "Вы получили 1 551 балл — максимум для этой акции",
                        subtitle = "Спасибо за участие в акции!\nЖелаем приятных покупок",
                        button = GrowingCashbackInfoButtonVo(
                            title = "Отлично",
                            action = GrowingCashbackInfoButtonVo.ButtonAction.CLOSE
                        )
                    )
                ),
                //2
                arrayOf(
                    growingCashbackActionInfoTestInstance(
                        actionState = growingCashbackActionStateTestInstance(maxReward = 1552)
                    ),
                    ReferralProgramStatus.Disabled,
                    GrowingCashbackActionVo.GrowingCashbackInfoVo(
                        image = GrowingCashbackActionVo.Image.REWARD,
                        title = "Вы получили 1 552 балла — максимум для этой акции",
                        subtitle = "Спасибо за участие в акции!\nЖелаем приятных покупок",
                        button = GrowingCashbackInfoButtonVo(
                            title = "Отлично",
                            action = GrowingCashbackInfoButtonVo.ButtonAction.CLOSE
                        )
                    )
                ),
                //3
                arrayOf(
                    growingCashbackActionInfoTestInstance(
                        actionState = growingCashbackActionStateTestInstance(maxReward = 1554)
                    ),
                    ReferralProgramStatus.Disabled,
                    GrowingCashbackActionVo.GrowingCashbackInfoVo(
                        image = GrowingCashbackActionVo.Image.REWARD,
                        title = "Вы получили 1 554 балла — максимум для этой акции",
                        subtitle = "Спасибо за участие в акции!\nЖелаем приятных покупок",
                        button = GrowingCashbackInfoButtonVo(
                            title = "Отлично",
                            action = GrowingCashbackInfoButtonVo.ButtonAction.CLOSE
                        )
                    )
                ),
                //4
                arrayOf(
                    growingCashbackActionInfoTestInstance(
                        actionState = growingCashbackActionStateTestInstance(maxReward = 1555)
                    ),
                    ReferralProgramStatus.Disabled,
                    GrowingCashbackActionVo.GrowingCashbackInfoVo(
                        image = GrowingCashbackActionVo.Image.REWARD,
                        title = "Вы получили 1 555 баллов — максимум для этой акции",
                        subtitle = "Спасибо за участие в акции!\nЖелаем приятных покупок",
                        button = GrowingCashbackInfoButtonVo(
                            title = "Отлично",
                            action = GrowingCashbackInfoButtonVo.ButtonAction.CLOSE
                        )
                    )
                ),
            )
        }

    }

    @RunWith(RobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class EndedStateTest {

        private val formatter = GrowingCashbackInfoFormatter(
            ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources)
        )

        @Test
        fun testFormat() {
            val expected = GrowingCashbackActionVo.GrowingCashbackInfoVo(
                image = GrowingCashbackActionVo.Image.END,
                title = "Увы, акция закончилась",
                subtitle = "Но у нас часто бывают скидки — заходите проверить",
                button = GrowingCashbackInfoButtonVo(
                    title = "За покупками",
                    action = GrowingCashbackInfoButtonVo.ButtonAction.CLOSE
                )
            )
            val actual = formatter.formatEnd()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @RunWith(RobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class ErrorStateTest {
        private val formatter = GrowingCashbackInfoFormatter(
            ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources)
        )

        @Test
        fun `format network error`() {
            val expected = GrowingCashbackActionVo.GrowingCashbackInfoVo(
                image = GrowingCashbackActionVo.Image.ERROR,
                title = "Проверьте, как там интернет",
                subtitle = "Кажется, слабая связь. Проверьте её и попробуйте обновить страницу",
                button = GrowingCashbackInfoButtonVo(
                    title = "Обновить",
                    action = GrowingCashbackInfoButtonVo.ButtonAction.RELOAD
                )
            )
            val error = CommunicationException(Response.NETWORK_ERROR)
            val actual = formatter.formatError(error)
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `format common error`() {
            val expected = GrowingCashbackActionVo.GrowingCashbackInfoVo(
                image = GrowingCashbackActionVo.Image.ERROR,
                title = "Попробуйте ещё раз",
                subtitle = "Или зайдите позже. А мы пока\nпостараемся всё наладить",
                button = GrowingCashbackInfoButtonVo(
                    title = "Повторить",
                    action = GrowingCashbackInfoButtonVo.ButtonAction.RELOAD
                )
            )
            val error = IllegalStateException()
            val actual = formatter.formatError(error)
            assertThat(actual).isEqualTo(expected)
        }
    }
}