package ru.yandex.market.clean.presentation.feature.checkout.confirm.cashback

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.domain.cashback.model.Cashback
import ru.yandex.market.domain.cashback.model.CashbackOptionType
import ru.yandex.market.domain.cashback.model.CashbackRestrictionReason
import ru.yandex.market.domain.cashback.model.cashbackOptionTestInstance
import ru.yandex.market.domain.cashback.model.cashbackOptionsTestInstance
import ru.yandex.market.domain.cashback.model.cashbackTestInstance
import ru.yandex.market.common.android.ResourcesManagerImpl
import java.math.BigDecimal

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class CheckoutCashBackFormatterTest(
    private val testInputData: TestInputData,
    private val expected: CheckoutConfirmCashBackVo?
) {

    private val formatter = CheckoutCashBackFormatter(
        ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources)
    )

    @Test
    fun format() {
        val actual = formatter.format(
            testInputData.cashback,
            testInputData.hasYandexPlus,
            testInputData.hasFreeDeliveryByYandexPlus,
            testInputData.selectedPaymentMethod,
            testInputData.isBoostFaqShow
        )

        assertThat(actual).isEqualTo(expected)
    }

    class TestInputData(
        private val logName: String,
        val cashback: Cashback?,
        val hasYandexPlus: Boolean,
        val hasFreeDeliveryByYandexPlus: Boolean?,
        val selectedPaymentMethod: PaymentMethod? = null,
        val isBoostFaqShow: Boolean,
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
                    logName = "Неплюсовик, есть списание и начисление, нет бесплатной доставки по плюсу",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            ),
                            spendOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = CashbackRestrictionReason.NOT_YA_PLUS_USER
                            )
                        )
                    ),
                    hasYandexPlus = false,
                    hasFreeDeliveryByYandexPlus = false,
                    selectedPaymentMethod = null,
                    isBoostFaqShow = false,
                ),
                CheckoutConfirmCashBackVo(
                    cashBackVoState = CashBackVoState.HAS_OPTIONS,
                    title = "У вас копятся баллы",
                    subtitle = "Подключите Яндекс Плюс, чтобы их тратить",
                    info = "",
                    isSpendSelected = false,
                    spendOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Списать",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.PLUS_HOME,
                        enabled = true
                    ),
                    getOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Получить",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.EMIT,
                        enabled = true
                    ),
                    boostFaq = null,
                )
            ),
            //1
            arrayOf(
                TestInputData(
                    logName = "Неплюсовик, есть списание и начисление, есть бесплатная доставка по плюсу",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            ),
                            spendOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = CashbackRestrictionReason.NOT_YA_PLUS_USER
                            )
                        )
                    ),
                    hasYandexPlus = false,
                    hasFreeDeliveryByYandexPlus = true,
                    selectedPaymentMethod = null,
                    isBoostFaqShow = false,
                ),
                CheckoutConfirmCashBackVo(
                    cashBackVoState = CashBackVoState.HAS_OPTIONS,
                    title = "У вас копятся баллы",
                    subtitle = "Подключите Плюс, чтобы их тратить и не платить за доставку заказов",
                    info = "",
                    isSpendSelected = false,
                    spendOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Списать",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.PLUS_HOME,
                        enabled = true
                    ),
                    getOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Получить",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.EMIT,
                        enabled = true
                    ),
                    boostFaq = null,
                )
            ),
            //2
            arrayOf(
                TestInputData(
                    logName = "Неплюсовик, только списание",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            spendOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = CashbackRestrictionReason.NOT_YA_PLUS_USER
                            )
                        )
                    ),
                    hasYandexPlus = false,
                    hasFreeDeliveryByYandexPlus = null,
                    selectedPaymentMethod = null,
                    isBoostFaqShow = false,
                ),
                CheckoutConfirmCashBackVo(
                    cashBackVoState = CashBackVoState.HAS_OPTIONS,
                    title = "У вас копятся баллы",
                    subtitle = "",
                    info = "",
                    isSpendSelected = false,
                    spendOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Списать",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.PLUS_HOME,
                        enabled = true
                    ),
                    getOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "",
                        value = "Не списывать\nбаллы",
                        hasIcon = false,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.KEEP,
                        enabled = true
                    ),
                    boostFaq = null,
                )
            ),
            //3
            arrayOf(
                TestInputData(
                    logName = "Неплюсовик, только начисление",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            ),
                            spendOption = cashbackOptionTestInstance(
                                value = BigDecimal.ZERO
                            )
                        )
                    ),
                    hasYandexPlus = false,
                    hasFreeDeliveryByYandexPlus = null,
                    selectedPaymentMethod = null,
                    isBoostFaqShow = false,
                ),
                CheckoutConfirmCashBackVo(
                    cashBackVoState = CashBackVoState.HAS_OPTIONS,
                    title = "У вас копятся баллы",
                    subtitle = "",
                    info = "",
                    isSpendSelected = false,
                    spendOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "",
                        value = "Списание\nнедоступно",
                        hasIcon = false,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.NONE,
                        enabled = false
                    ),
                    getOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Получить",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.EMIT,
                        enabled = true
                    ),
                    boostFaq = null,
                )
            ),
            //4
            arrayOf(
                TestInputData(
                    logName = "Неплюсовик, нет доступных опций",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.ZERO,
                                restrictionReason = null
                            ),
                            spendOption = cashbackOptionTestInstance(
                                value = BigDecimal.ZERO
                            )
                        )
                    ),
                    hasYandexPlus = false,
                    hasFreeDeliveryByYandexPlus = true,
                    selectedPaymentMethod = null,
                    isBoostFaqShow = false,
                ),
                CheckoutConfirmCashBackVo(
                    cashBackVoState = CashBackVoState.NOT_ALLOWED,
                    title = "У вас копятся баллы",
                    subtitle = "Подключите Плюс, чтобы их тратить и не платить за доставку заказов",
                    info = "",
                    isSpendSelected = false,
                    spendOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "",
                        value = "Списание\nнедоступно",
                        hasIcon = false,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.NONE,
                        enabled = false
                    ),
                    getOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "",
                        value = "Не списывать\nбаллы",
                        hasIcon = false,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.KEEP,
                        enabled = true
                    ),
                    boostFaq = null,
                )
            ),
            //5
            arrayOf(
                TestInputData(
                    logName = "Плюсовик, есть списание и начисление",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            ),
                            spendOption = cashbackOptionTestInstance(
                                value = BigDecimal.ONE,
                                restrictionReason = null
                            )
                        ),
                        selectedOption = CashbackOptionType.SPEND
                    ),
                    hasYandexPlus = true,
                    hasFreeDeliveryByYandexPlus = true,
                    selectedPaymentMethod = null,
                    isBoostFaqShow = false,
                ),
                CheckoutConfirmCashBackVo(
                    cashBackVoState = CashBackVoState.HAS_OPTIONS,
                    title = "Баллы Яндекс Плюса",
                    subtitle = "",
                    info = "",
                    isSpendSelected = true,
                    spendOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Списать",
                        value = "1",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.SPEND,
                        enabled = true
                    ),
                    getOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Получить",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.EMIT,
                        enabled = true
                    ),
                    boostFaq = null,
                )
            ),
            //6
            arrayOf(
                TestInputData(
                    logName = "Плюсовик, есть начсление, списание недоступно по оплате",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            ),
                            spendOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = CashbackRestrictionReason.NOT_SUITABLE_PAYMENT_TYPE
                            )
                        ),
                        selectedOption = CashbackOptionType.EMIT
                    ),
                    hasYandexPlus = true,
                    hasFreeDeliveryByYandexPlus = true,
                    selectedPaymentMethod = null,
                    isBoostFaqShow = false,
                ),
                CheckoutConfirmCashBackVo(
                    cashBackVoState = CashBackVoState.HAS_OPTIONS,
                    title = "Баллы Яндекс Плюса",
                    subtitle = "",
                    info = "С выбранным способом оплаты списание баллов недоступно.",
                    isSpendSelected = false,
                    spendOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "",
                        value = "Списание\nнедоступно",
                        hasIcon = false,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.NONE,
                        enabled = false
                    ),
                    getOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Получить",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.EMIT,
                        enabled = true
                    ),
                    boostFaq = null,
                )
            ),
            //7
            arrayOf(
                TestInputData(
                    logName = "Плюсовик, есть начисление, списане недоступно по категории",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            ),
                            spendOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = CashbackRestrictionReason.NOT_SUITABLE_CATEGORY
                            )
                        ),
                        selectedOption = CashbackOptionType.EMIT
                    ),
                    hasYandexPlus = true,
                    hasFreeDeliveryByYandexPlus = true,
                    selectedPaymentMethod = null,
                    isBoostFaqShow = false,
                ),
                CheckoutConfirmCashBackVo(
                    cashBackVoState = CashBackVoState.HAS_OPTIONS,
                    title = "Баллы Яндекс Плюса",
                    subtitle = "",
                    info = "Баллами нельзя оплачивать товары, доставляемые партнёрами, алкоголь, БАДы и стики для испарителей.",
                    isSpendSelected = false,
                    spendOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "",
                        value = "Списание\nнедоступно",
                        hasIcon = false,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.NONE,
                        enabled = false
                    ),
                    getOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Получить",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.EMIT,
                        enabled = true
                    ),
                    boostFaq = null,
                )
            ),
            //8
            arrayOf(
                TestInputData(
                    logName = "Плюсовик, есть начисление, списание 0 баллов",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            ),
                            spendOption = cashbackOptionTestInstance(
                                value = BigDecimal.ZERO,
                                restrictionReason = null
                            )
                        ),
                        selectedOption = CashbackOptionType.EMIT
                    ),
                    hasYandexPlus = true,
                    hasFreeDeliveryByYandexPlus = true,
                    selectedPaymentMethod = null,
                    isBoostFaqShow = false,
                ),
                CheckoutConfirmCashBackVo(
                    cashBackVoState = CashBackVoState.HAS_OPTIONS,
                    title = "Баллы Яндекс Плюса",
                    subtitle = "",
                    info = "",
                    isSpendSelected = false,
                    spendOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "",
                        value = "Списание\nнедоступно",
                        hasIcon = false,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.NONE,
                        enabled = false
                    ),
                    getOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Получить",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.EMIT,
                        enabled = true
                    ),
                    boostFaq = null,
                )
            ),
            //9
            arrayOf(
                TestInputData(
                    logName = "Плюсовик, есть опции, оплата спасибами",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            ),
                            spendOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            )
                        )
                    ),
                    hasYandexPlus = true,
                    hasFreeDeliveryByYandexPlus = true,
                    selectedPaymentMethod = PaymentMethod.SPASIBO_PAY,
                    isBoostFaqShow = false,
                ),
                CheckoutConfirmCashBackVo(
                    cashBackVoState = CashBackVoState.HAS_OPTIONS,
                    title = "Баллы Яндекс Плюса",
                    subtitle = "",
                    info = "С выбранным способом оплаты списание баллов недоступно.",
                    isSpendSelected = false,
                    spendOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "",
                        value = "Списание\nнедоступно",
                        hasIcon = false,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.NONE,
                        enabled = false
                    ),
                    getOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Получить",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.EMIT,
                        enabled = true
                    ),
                    boostFaq = null,
                )
            ),
            //10
            arrayOf(
                TestInputData(
                    logName = "Информация о бусте пвз",
                    cashback = cashbackTestInstance(
                        cashbackOptions = cashbackOptionsTestInstance(
                            emitOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            ),
                            spendOption = cashbackOptionTestInstance(
                                value = BigDecimal.TEN,
                                restrictionReason = null
                            )
                        )
                    ),
                    hasYandexPlus = true,
                    hasFreeDeliveryByYandexPlus = true,
                    selectedPaymentMethod = null,
                    isBoostFaqShow = true,
                ),
                CheckoutConfirmCashBackVo(
                    cashBackVoState = CashBackVoState.HAS_OPTIONS,
                    title = "Баллы Яндекс Плюса",
                    subtitle = "",
                    info = "",
                    isSpendSelected = false,
                    spendOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Списать",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.SPEND,
                        enabled = true
                    ),
                    getOption = CheckoutConfirmCashBackVo.OptionVo(
                        title = "Получить",
                        value = "10",
                        hasIcon = true,
                        action = CheckoutConfirmCashBackVo.OptionVo.Action.EMIT,
                        enabled = true
                    ),
                    boostFaq = "Вы получите дополнительные баллы, потому что выбрали пункт самовывоза Яндекс.Маркета",
                )
            ),
        )
    }
}