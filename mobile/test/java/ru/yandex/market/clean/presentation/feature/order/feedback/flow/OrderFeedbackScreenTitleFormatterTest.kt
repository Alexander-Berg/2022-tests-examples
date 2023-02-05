package ru.yandex.market.clean.presentation.feature.order.feedback.flow

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.common.android.ResourcesManager

@RunWith(Parameterized::class)
class OrderFeedbackScreenTitleFormatterTest(
    private val orderId: String,
    private val shopOrderId: String?,
    private val isClickAndCollect: Boolean,
    private val expectedOrderId: String
) {

    private val resourcesDataStore = mock<ResourcesManager> {
        on {
            getFormattedString(
                R.string.order_feedback_title_template,
                expectedOrderId
            )
        } doReturn "$FORMATTED_TITLE_PREFIX $expectedOrderId"
    }
    private val formatter = OrderFeedbackScreenTitleFormatter(resourcesDataStore)

    @Test
    fun testFormat() {
        assertThat(
            formatter.format(
                orderId,
                shopOrderId,
                isClickAndCollect
            )
        ).isEqualTo("$FORMATTED_TITLE_PREFIX $expectedOrderId")
    }

    companion object {

        private const val FORMATTED_TITLE_PREFIX = "Заказ №"

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf("12345", null, false, "12345"),
            //1
            arrayOf("12345", null, true, "12345"),
            //2
            arrayOf("12345", "12345", false, "12345"),
            //3
            arrayOf("12345", "12345", true, "12345"),
            //4
            arrayOf("12345", "54321", false, "12345"),
            //5
            arrayOf("12345", "54321", true, "12345 / 54321"),
        )
    }
}