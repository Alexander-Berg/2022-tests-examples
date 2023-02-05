package ru.yandex.market.data.deeplinks

import android.net.Uri
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.NoOpMarketApplication
import ru.yandex.market.activity.order.CallCourierAnalytics
import ru.yandex.market.analitycs.events.health.HealthEvent
import ru.yandex.market.analytics.facades.MiscellaneousAnalyticsFacade
import ru.yandex.market.analytics.health.HealthName
import ru.yandex.market.cart.cases.AddSeveralOffersToCartUseCase
import ru.yandex.market.clean.domain.usecase.OfferAffectingInformationUseCase
import ru.yandex.market.clean.domain.usecase.cart.UpdateCartItemUseCase
import ru.yandex.market.clean.domain.usecase.catalog.GetNavigationNodeUseCase
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.clean.domain.usecase.pickup.renewal.CanShowPickupRenewalButtonUseCase
import ru.yandex.market.clean.domain.usecase.product.GetDetailedSkuUseCase
import ru.yandex.market.clean.domain.usecase.product.GetReviewsDeeplinkDataUseCase
import ru.yandex.market.clean.domain.usecase.sis.AddDirectSisBusinessIdUseCase
import ru.yandex.market.clean.domain.usecase.upselllanding.SetSessionPageViewUniqueIdUseCase
import ru.yandex.market.common.featureconfigs.managers.LiveStreamToggleManager
import ru.yandex.market.common.featureconfigs.managers.UpsellLandingFeatureToggleManager
import ru.yandex.market.common.featureconfigs.managers.WebViewWhiteListConfigManager
import ru.yandex.market.common.featureconfigs.models.WebViewWhiteListConfig
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.data.deeplinks.links.BrowserDeeplink
import ru.yandex.market.data.deeplinks.links.CatalogDeeplink
import ru.yandex.market.data.deeplinks.links.MarketWebDeeplink
import ru.yandex.market.data.deeplinks.links.whitemarket.GetSkuIdDeeplinkUseCase
import ru.yandex.market.deeplinks.DeeplinkSource
import ru.yandex.market.domain.ecom.question.usecase.TriggerEcomQuestionUseCase
import ru.yandex.market.feature.manager.FmcgRedesignFeatureManager
import ru.yandex.market.manager.AuthManager
import ru.yandex.market.ui.view.mvp.cartcounterbutton.GetCartItemUseCase
import ru.yandex.market.web.MarketHostProvider
import ru.yandex.market.web.MarketWebUrlProviderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], application = NoOpMarketApplication::class)
class SimpleDeeplinkParserTest {

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    private val authManager = mock<AuthManager>()
    private val getNavigationNodeUseCase = mock<GetNavigationNodeUseCase>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val callCourierAnalytics = mock<CallCourierAnalytics>()

    private val whiteMarketDeeplinkUseCase = mock<GetSkuIdDeeplinkUseCase>()
    private val getOrderUseCase = mock<GetOrderUseCase>()
    private val liveStreamToggleManager = mock<LiveStreamToggleManager>()

    private val webViewWhiteListConfig: WebViewWhiteListConfig = mock {
        on { isEnabled } doReturn true
        on { safeHosts } doReturn listOf("market.yandex.ru")
    }
    private val webViewWhiteListConfigManager: WebViewWhiteListConfigManager = mock {
        on { get() } doReturn webViewWhiteListConfig
    }
    private val featureConfigsProvider: FeatureConfigsProvider = mock {
        on { webViewWhiteListConfigManager } doReturn webViewWhiteListConfigManager
    }

    private val marketHostProvider = mock<MarketHostProvider>()
    private val canShowPickupRenewalButtonUseCase = mock<CanShowPickupRenewalButtonUseCase>()
    private val miscellaneousAnalyticsFacade = mock<MiscellaneousAnalyticsFacade>()
    private val marketWebUrlProviderFactory = mock<dagger.Lazy<MarketWebUrlProviderFactory>>()
    private val ecomQuestionUseCase = mock<TriggerEcomQuestionUseCase>()
    private val getReviewsDeeplinkDataUseCase = mock<GetReviewsDeeplinkDataUseCase>()
    private val getCartItemUseCase = mock<GetCartItemUseCase>()
    private val updateCartItemUseCase = mock<UpdateCartItemUseCase>()
    private val offerAffectingInformationUseCase = mock<OfferAffectingInformationUseCase>()
    private val getDetailedSkuUseCase = mock<GetDetailedSkuUseCase>()
    private val addSeveralOffersToCartUseCase = mock<AddSeveralOffersToCartUseCase>()
    private val addDirectSisBusinessIdUseCase = mock<AddDirectSisBusinessIdUseCase>()
    private val fmcgRedesignFeatureManager = mock<FmcgRedesignFeatureManager>()
    private val upsellLandingFeatureToggleManager = mock<UpsellLandingFeatureToggleManager>()
    private val setSessionPageViewUniqueIdUseCase = mock<SetSessionPageViewUniqueIdUseCase>()

    @Test
    fun `Send event if scheme doesn't equals with uri scheme and source is push`() {
        val scheme = "some_scheme"
        val parser = createSimpleDeeplinkParser(scheme)
        val uri = Uri.EMPTY
        val source = DeeplinkSource.PUSH_DEEPLINK

        thrown.expect(IllegalArgumentException::class.java)
        val deeplink = parser.parse(uri, source)
        verify(analyticsService).report(argThat<HealthEvent> { this.name == HealthName.PUSH_DEEPLINK_UNKNOWN })
    }

    @Test
    fun `Don't send event if scheme doesn't equals with uri scheme and source is not push`() {
        val scheme = "some_scheme"
        val parser = createSimpleDeeplinkParser(scheme)
        val uri = Uri.EMPTY
        val source = DeeplinkSource.SIMPLE_DEEPLINK

        thrown.expect(IllegalArgumentException::class.java)
        val deeplink = parser.parse(uri, source)
        verify(analyticsService, never()).report(any<HealthEvent>())
    }

    @Test
    fun `Don't send event if uri matcher did't match any deeplink, but url transform deeplink was created`() {
        val scheme = "some_scheme"
        val parser = createSimpleDeeplinkParser(scheme)
        val uri = Uri.parse("$scheme://checkout")
        val source = DeeplinkSource.PUSH_DEEPLINK

        val deeplink = parser.parse(uri, source)
        verify(analyticsService, never()).report(any<HealthEvent>())
    }

    @Test
    fun `Send event if uri matcher did't match any deeplink, and url transform deeplink wasn't created`() {
        val scheme = "some_scheme"
        val parser = createSimpleDeeplinkParser(scheme)
        val uri = Uri.Builder()
            .scheme(scheme)
            .build()
        val source = DeeplinkSource.PUSH_DEEPLINK

        val deeplink = parser.parse(uri, source)
        verify(analyticsService).report(argThat<HealthEvent> { this.name == HealthName.PUSH_DEEPLINK_UNKNOWN })
    }

    @Test
    fun `Market browser url with double masked characters in inner url - BrowserDeeplink was created - No unmasking`() {
        val scheme = "yamarket"
        val parser = createSimpleDeeplinkParser(scheme)
        val uri = Uri.parse("yamarket://browser?hybrid-mode=1&url=https%3A%2F%2Fmarket.yandex.ru%2Fspecial%2Fcheapest-as-gift-4-5-landing%3FshopPromoId%3D%252318268%26pokupki%3D1%26cpa%3D1%26utm_campaign%3Dpromo_beauty_p_sale54_25_06_a%26clid%3D621%26utm_referrer%3D621%26utm_source%3Dpush_android%26utm_medium%3Dmassmail")
        val source = DeeplinkSource.PUSH_DEEPLINK

        val deeplink = parser.parse(uri, source)

        assertTrue(deeplink is BrowserDeeplink)

        assertEquals(
            "yamarket://browser?hybrid-mode=1&url=https%3A%2F%2Fmarket.yandex.ru%2Fspecial%2Fcheapest-as-gift-4-5-landing%3FshopPromoId%3D%252318268%26pokupki%3D1%26cpa%3D1%26utm_campaign%3Dpromo_beauty_p_sale54_25_06_a%26clid%3D621%26utm_referrer%3D621%26utm_source%3Dpush_android%26utm_medium%3Dmassmail",
            deeplink?.uri.toString()
        )
    }

    @Test
    fun `Web url with double masked characters in inner url - MarketWebDeeplink was created - No unmasking`() {
        val scheme = "yamarket"
        val parser = createSimpleDeeplinkParser(scheme)
        val uri = Uri.parse("https://market.yandex.ru/special/cheapest-as-gift-4-5-landing?shopPromoId=%2318268&pokupki=1&cpa=1&utm_campaign=promo_beauty_p_sale54_25_06_a&clid=621&utm_referrer=621&utm_source=push_android&utm_medium=massmail")
        val source = DeeplinkSource.PUSH_DEEPLINK

        val deeplink = parser.parse(uri, source)

        assertTrue(deeplink is MarketWebDeeplink)

        assertEquals(
            "https://market.yandex.ru/special/cheapest-as-gift-4-5-landing?shopPromoId=%2318268&pokupki=1&cpa=1&utm_campaign=promo_beauty_p_sale54_25_06_a&clid=621&utm_referrer=621&utm_source=push_android&utm_medium=massmail",
            deeplink?.uri.toString()
        )
    }

    @Test
    fun `Market url without masket characters - Correct deeplink type was created`() {
        val scheme = "yamarket"
        val parser = createSimpleDeeplinkParser(scheme)
        val uri = Uri.parse("yamarket://catalog/21448850")
        val source = DeeplinkSource.PUSH_DEEPLINK

        val deeplink = parser.parse(uri, source)

        assertTrue(deeplink is CatalogDeeplink)
        assertEquals("yamarket://catalog/21448850", deeplink?.uri.toString())
    }

    @Test
    fun `Market url with masket characters - Correct deeplink type was created - Unmasking masked characters`() {
        val scheme = "yamarket"
        val parser = createSimpleDeeplinkParser(scheme)
        val uri = Uri.parse("yamarket://catalog%2F21448850")
        val source = DeeplinkSource.PUSH_DEEPLINK

        val deeplink = parser.parse(uri, source)

        assertTrue(deeplink is CatalogDeeplink)
        assertEquals("yamarket://catalog/21448850", deeplink?.uri.toString())
    }

    @Test
    fun `Market url with double masked characters - Correct deeplink type was created - Unmasking performed single time`() {
        val scheme = "yamarket"
        val parser = createSimpleDeeplinkParser(scheme)
        val uri = Uri.parse("yamarket://catalog%2F%2521448850")
        val source = DeeplinkSource.PUSH_DEEPLINK

        val deeplink = parser.parse(uri, source)

        assertTrue(deeplink is CatalogDeeplink)
        assertEquals("yamarket://catalog/%21448850", deeplink?.uri.toString())
    }

    private fun createSimpleDeeplinkParser(scheme: String): SimpleDeeplinkParser {
        return SimpleDeeplinkParser(
            scheme,
            authManager,
            getNavigationNodeUseCase,
            analyticsService,
            callCourierAnalytics,
            whiteMarketDeeplinkUseCase,
            liveStreamToggleManager,
            getOrderUseCase,
            featureConfigsProvider,
            marketHostProvider,
            canShowPickupRenewalButtonUseCase,
            marketWebUrlProviderFactory,
            miscellaneousAnalyticsFacade,
            getReviewsDeeplinkDataUseCase,
            ecomQuestionUseCase,
            getCartItemUseCase,
            updateCartItemUseCase,
            offerAffectingInformationUseCase,
            getDetailedSkuUseCase,
            addSeveralOffersToCartUseCase,
            addDirectSisBusinessIdUseCase,
            fmcgRedesignFeatureManager,
            upsellLandingFeatureToggleManager,
            setSessionPageViewUniqueIdUseCase,
        )
    }
}
