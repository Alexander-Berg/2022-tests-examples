package ru.yandex.market.clean.presentation.feature.checkout.confirm.plus

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.presentation.feature.cart.AboutPlusNavigationSource
import ru.yandex.market.clean.presentation.feature.cart.vo.PlusInfoVo
import ru.yandex.market.clean.presentation.feature.cashback.Gradient
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.domain.cashback.model.Cashback
import ru.yandex.market.domain.cashback.model.cashbackOptionTestInstance
import ru.yandex.market.domain.cashback.model.cashbackOptionsTestInstance
import ru.yandex.market.domain.cashback.model.cashbackTestInstance
import java.math.BigDecimal

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class CheckoutPlusInfoFormatterTest(
    private val testInputData: TestInputData,
    private val expected: PlusInfoVo?
) {
    private val formatter = CheckoutPlusInfoFormatter(
        ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources)
    )

    @Test
    fun formatForNotPlus() {
        val actual = formatter.formatForNotPlus(testInputData.cashback)
        assertThat(actual).isEqualTo(expected)
    }

    class TestInputData(
        private val logName: String,
        val cashback: Cashback
    ) {
        override fun toString(): String = logName
    }

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        @JvmStatic
        fun data(): Iterable<Array<Any?>> = listOf(
            //0
            arrayOf(
                TestInputData(
                    logName = "Кешбэк положительный",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            )
                        )
                    )
                ),
                PlusInfoVo(
                    isDisclosureVisible = true,
                    title = "С Плюсом выгодно",
                    subtitle = "Получите от :image: :gradient:10:gradient: баллов за покупку, если подключите Плюс сейчас",
                    aboutPlusNavigationSource = AboutPlusNavigationSource.PLUS_INFO_BLOCK,
                    navigateTarget = PlusInfoVo.NavigateTarget.PlusHome(),
                    gradient = Gradient.PLUS_GRADIENT_2_COLORS
                )
            ),
            //1
            arrayOf(
                TestInputData(
                    logName = "Кешбэк отрицательный",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.valueOf(-100),
                                restrictionReason = null
                            )
                        )
                    )
                ),
                null
            ),
            //2
            arrayOf(
                TestInputData(
                    logName = "Кешбэк положительный",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.valueOf(100),
                                restrictionReason = null
                            )
                        )
                    )
                ),
                PlusInfoVo(
                    isDisclosureVisible = true,
                    title = "С Плюсом выгодно",
                    subtitle = "Получите от :image: :gradient:100:gradient: баллов за покупку, если подключите Плюс сейчас",
                    aboutPlusNavigationSource = AboutPlusNavigationSource.PLUS_INFO_BLOCK,
                    navigateTarget = PlusInfoVo.NavigateTarget.PlusHome(),
                    gradient = Gradient.PLUS_GRADIENT_2_COLORS
                )
            ),
            //3
            arrayOf(
                TestInputData(
                    logName = "Кешбэк положительный",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.valueOf(101),
                                restrictionReason = null
                            )
                        )
                    )
                ),
                PlusInfoVo(
                    isDisclosureVisible = true,
                    title = "С Плюсом выгодно",
                    subtitle = "Получите от :image: :gradient:101:gradient: балла за покупку, если подключите Плюс сейчас",
                    aboutPlusNavigationSource = AboutPlusNavigationSource.PLUS_INFO_BLOCK,
                    navigateTarget = PlusInfoVo.NavigateTarget.PlusHome(),
                    gradient = Gradient.PLUS_GRADIENT_2_COLORS
                )
            ),
            //4
            arrayOf(
                TestInputData(
                    logName = "Кешбэк положительный",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.valueOf(102),
                                restrictionReason = null
                            )
                        )
                    )
                ),
                PlusInfoVo(
                    isDisclosureVisible = true,
                    title = "С Плюсом выгодно",
                    subtitle = "Получите от :image: :gradient:102:gradient: баллов за покупку, если подключите Плюс сейчас",
                    aboutPlusNavigationSource = AboutPlusNavigationSource.PLUS_INFO_BLOCK,
                    navigateTarget = PlusInfoVo.NavigateTarget.PlusHome(),
                    gradient = Gradient.PLUS_GRADIENT_2_COLORS
                )
            ),
            //3
            arrayOf(
                TestInputData(
                    logName = "Кешбэк == 0",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.ZERO,
                                restrictionReason = null
                            )
                        )
                    )
                ),
                null
            )
        )
    }
}