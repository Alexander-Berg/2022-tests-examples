package ru.yandex.market.clean.presentation.formatter

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.agitate.GrowingCashbackAgitationFormatter
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.agitate.GrowingCashbackAgitationVo
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.agitate.NumberToWordFormatter
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.domain.cashback.model.GrowingCashbackActionInfo
import ru.yandex.market.domain.cashback.model.GrowingCashbackActionState
import ru.yandex.market.safe.Safe
import ru.yandex.market.utils.EquableCharSequence
import java.math.BigDecimal
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class GrowingCashbackAgitationFormatterTest {

    private val moneyFormatter = mock<MoneyFormatter> {
        on {
            formatCashbackOrderThreshold(any())
        } doReturn PRICE_TEXT
    }
    private val numberToWordFormatter = mock<NumberToWordFormatter>() {
        on {
            format(2)
        } doReturn Safe { "два" }
    }
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val formatter = GrowingCashbackAgitationFormatter(
        moneyFormatter,
        numberToWordFormatter,
        ResourcesManagerImpl(context.resources),
    )

    @Test
    fun `format agitation for available special`() {
        val formatted = formatter.format(TEST_INPUT_AVAILABLE)
        assertThat(formatted).isEqualTo(TEST_OUTPUT_SUCCESS)
    }

    @Test
    fun `format agitation for unavailable special`() {
        val formatted = formatter.format(TEST_INPUT_UNAVAILABLE)
        assertThat(formatted).isEqualTo(TEST_OUTPUT_END)
    }

    @Test
    fun `format error`() {
        val error = mock<Throwable>()
        val formatted = formatter.formatError(error)
        assertThat(formatted).isEqualTo(TEST_OUTPUT_ERROR)
    }

    companion object {

        private const val PRICE_TEXT = "300 рублей"
        private val TEST_INPUT_AVAILABLE = GrowingCashbackActionInfo(
            actionState = GrowingCashbackActionState(
                maxOrdersCount = 2,
                maxReward = 600,
                orderThreshold = BigDecimal(300),
                state = GrowingCashbackActionState.State.ACTIVE,
                aboutPageLink = "",
                actionEndDate = Date(),
            ),
            ordersReward = listOf(
                GrowingCashbackActionInfo.OrderReward(
                    "first",
                    isOrderPurchased = false,
                    isOrderDelivered = false,
                    reward = 200,
                ),
                GrowingCashbackActionInfo.OrderReward(
                    "second",
                    isOrderPurchased = false,
                    isOrderDelivered = false,
                    reward = 400,
                ),
            ),
        )
        private val TEST_INPUT_UNAVAILABLE = GrowingCashbackActionInfo(
            actionState = GrowingCashbackActionState(
                maxOrdersCount = 2,
                maxReward = 600,
                orderThreshold = BigDecimal(300),
                state = GrowingCashbackActionState.State.END,
                aboutPageLink = "",
                actionEndDate = Date(),
            ),
            ordersReward = listOf(
                GrowingCashbackActionInfo.OrderReward(
                    "first",
                    isOrderPurchased = false,
                    isOrderDelivered = false,
                    reward = 200,
                ),
                GrowingCashbackActionInfo.OrderReward(
                    "second",
                    isOrderPurchased = false,
                    isOrderDelivered = false,
                    reward = 400,
                ),
            ),
        )
        private val TEST_OUTPUT_SUCCESS = GrowingCashbackAgitationVo(
            image = GrowingCashbackAgitationVo.Image.SUCCESS,
            title = "Подключите Плюс\nи получайте баллы",
            text = EquableCharSequence(
                ":image: 200 и 400 баллов за первые\nдва заказа от 300 рублей в приложении",
                ":image: 200 и 400 баллов за первые\nдва заказа от 300 рублей в приложении"
            ),
            buttonText = "Подключить Плюс",
            buttonAction = GrowingCashbackAgitationVo.ButtonAction.BUY_PLUS,
            linkText = "Условия акции",
            linkUrl = "https://market.yandex.ru/special/growing-cashback",
        )

        private val TEST_OUTPUT_END = GrowingCashbackAgitationVo(
            image = GrowingCashbackAgitationVo.Image.END,
            title = "Увы, акция закончилась",
            text = EquableCharSequence(
                "Но у нас часто бывают скидки — заходите проверить",
                "Но у нас часто бывают скидки — заходите проверить"
            ),
            buttonText = "За покупками",
            buttonAction = GrowingCashbackAgitationVo.ButtonAction.CLOSE,
            linkText = "",
            linkUrl = "",
        )

        private val TEST_OUTPUT_ERROR = GrowingCashbackAgitationVo(
            image = GrowingCashbackAgitationVo.Image.ERROR,
            title = "Попробуйте ещё раз",
            text = EquableCharSequence(
                "Или зайдите позже. А мы пока\nпостараемся всё наладить",
                "Или зайдите позже. А мы пока\nпостараемся всё наладить"
            ),
            buttonText = "Повторить",
            buttonAction = GrowingCashbackAgitationVo.ButtonAction.RELOAD,
            linkUrl = "",
            linkText = "",
        )
    }
}
