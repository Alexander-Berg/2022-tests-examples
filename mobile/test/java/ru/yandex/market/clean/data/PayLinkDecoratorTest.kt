package ru.yandex.market.clean.data

import android.os.Build
import io.reactivex.schedulers.Schedulers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.MethodRule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.base.network.common.address.HttpAddress
import ru.yandex.market.base.network.common.address.HttpAddressParser
import ru.yandex.market.common.schedulers.WorkerScheduler
import ru.yandex.market.manager.AuthManager
import ru.yandex.market.passport.model.AuthorizedUrl
import ru.yandex.market.utils.asOptional

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class PayLinkDecoratorTest {

    @JvmField
    @Rule
    val mockitoRule: MethodRule = MockitoJUnit.rule()

    private val authManager = mock<AuthManager>()
    private val httpAddressParser = mock<HttpAddressParser>()
    private val scheduler = WorkerScheduler(Schedulers.trampoline())
    private val stationSubscriptionPaymentUrlDecorator = StationSubscriptionPaymentUrlDecorator(httpAddressParser)
    private val decorator =
        PayLinkDecorator(authManager, httpAddressParser, scheduler, stationSubscriptionPaymentUrlDecorator)

    @Test
    fun `Adds spasibo query param when selected payment methods contain spasibo`() {
        whenever(authManager.getAuthUrl(any())).thenReturn(AUTH_URL.asOptional())
        val address = HttpAddress.builder()
            .scheme("https")
            .host("beru.ru")
            .addPathSegment("pay")
            .build()
        whenever(httpAddressParser.parse(any<String>())).thenReturn(address)

        decorator.decoratePayLink(
            payLink = "",
            isSpasiboPayEnabled = true,
            isStationSubscription = false,
            selectedCardId = ""
        )
            .test()
            .assertNoErrors()

        verify(authManager).getAuthUrl("https://beru.ru/pay?${PayLinkDecorator.QUERY_PARAM_SPASIBO_PAY_NAME}=${PayLinkDecorator.QUERY_PARAM_SPASIBO_PAY_VALUE}")
    }

    @Test
    fun `Adds next spasibo query param when selected payment methods contain spasibo`() {
        whenever(authManager.getAuthUrl(any())).thenReturn(AUTH_URL.asOptional())
        val address = HttpAddress.builder()
            .scheme("https")
            .host("beru.ru")
            .addPathSegment("pay")
            .addQueryParameter("param", "value")
            .build()
        whenever(httpAddressParser.parse(any<String>())).thenReturn(address)

        decorator.decoratePayLink(
            payLink = "",
            isSpasiboPayEnabled = true,
            isStationSubscription = false,
            selectedCardId = ""
        )
            .test()
            .assertNoErrors()

        verify(authManager).getAuthUrl("https://beru.ru/pay?param=value&${PayLinkDecorator.QUERY_PARAM_SPASIBO_PAY_NAME}=${PayLinkDecorator.QUERY_PARAM_SPASIBO_PAY_VALUE}")
    }

    @Test
    fun `Do not add spasibo query param when selected payment methods does not contain spasibo`() {
        whenever(authManager.getAuthUrl(any())).thenReturn(AUTH_URL.asOptional())

        val payLink = "https://beru.ru/pay"
        decorator.decoratePayLink(
            payLink = payLink,
            isSpasiboPayEnabled = false,
            isStationSubscription = false,
            selectedCardId = ""
        )
            .test()
            .assertNoErrors()

        verify(authManager).getAuthUrl(payLink)
    }

    @Test
    fun `Adds queries for station subscription link`() {
        whenever(authManager.getAuthUrl(any())).thenReturn(AUTH_URL.asOptional())
        val address = HttpAddress.builder()
            .scheme("https")
            .host("beru.ru")
            .addPathSegment("pay")
            .build()
        whenever(httpAddressParser.parse(any<String>())).thenReturn(address)

        decorator.decoratePayLink(
            payLink = "",
            isSpasiboPayEnabled = true,
            isStationSubscription = true,
            selectedCardId = MOCK_SELECTED_CARD
        )
            .test()
            .assertNoErrors()

        verify(authManager).getAuthUrl(
            "https://beru.ru/pay?${StationSubscriptionPaymentUrlDecorator.PAYMENT_METHOD_ID_KEY}=${MOCK_SELECTED_CARD}" +
                    "&${StationSubscriptionPaymentUrlDecorator.DONT_USE_COOKIES_KEY}=${StationSubscriptionPaymentUrlDecorator.DONT_USE_COOKIES_VALUE}" +
                    "&${StationSubscriptionPaymentUrlDecorator.POST_MESSAGE_VERSION_KEY}=${StationSubscriptionPaymentUrlDecorator.POST_MESSAGE_VERSION_VALUE}" +
                    "&${StationSubscriptionPaymentUrlDecorator.WIDGET_SUB_SERVICE_NAME_KEY}=${StationSubscriptionPaymentUrlDecorator.WIDGET_SUB_SERVICE_NAME_VALUE}" +
                    "&${StationSubscriptionPaymentUrlDecorator.MODE_KEY}=${StationSubscriptionPaymentUrlDecorator.MODE_VALUE}"
        )
    }

    @Test
    fun `Adds queries for station subscription link when card is empty`() {
        whenever(authManager.getAuthUrl(any())).thenReturn(AUTH_URL.asOptional())
        val address = HttpAddress.builder()
            .scheme("https")
            .host("beru.ru")
            .addPathSegment("pay")
            .build()
        whenever(httpAddressParser.parse(any<String>())).thenReturn(address)

        decorator.decoratePayLink(
            payLink = "",
            isSpasiboPayEnabled = true,
            isStationSubscription = true,
            selectedCardId = ""
        )
            .test()
            .assertNoErrors()

        verify(authManager).getAuthUrl(
            "https://beru.ru/pay?${StationSubscriptionPaymentUrlDecorator.DONT_USE_COOKIES_KEY}=${StationSubscriptionPaymentUrlDecorator.DONT_USE_COOKIES_VALUE}" +
                    "&${StationSubscriptionPaymentUrlDecorator.POST_MESSAGE_VERSION_KEY}=${StationSubscriptionPaymentUrlDecorator.POST_MESSAGE_VERSION_VALUE}" +
                    "&${StationSubscriptionPaymentUrlDecorator.WIDGET_SUB_SERVICE_NAME_KEY}=${StationSubscriptionPaymentUrlDecorator.WIDGET_SUB_SERVICE_NAME_VALUE}" +
                    "&${StationSubscriptionPaymentUrlDecorator.MODE_KEY}=${StationSubscriptionPaymentUrlDecorator.MODE_VALUE}"
        )
    }

    companion object {
        private val AUTH_URL = AuthorizedUrl("https://beru.ru/auth", emptyMap(), emptyList())
        private const val MOCK_SELECTED_CARD = "000000"
    }
}