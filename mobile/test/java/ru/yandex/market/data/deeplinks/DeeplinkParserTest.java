package ru.yandex.market.data.deeplinks;

import android.net.Uri;

import com.annimon.stream.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import ru.yandex.market.BaseTest;
import ru.yandex.market.activity.order.CallCourierAnalytics;
import ru.yandex.market.analytics.facades.MiscellaneousAnalyticsFacade;
import ru.yandex.market.cart.cases.AddSeveralOffersToCartUseCase;
import ru.yandex.market.clean.domain.usecase.OfferAffectingInformationUseCase;
import ru.yandex.market.clean.domain.usecase.cart.UpdateCartItemUseCase;
import ru.yandex.market.clean.domain.usecase.catalog.GetNavigationNodeUseCase;
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase;
import ru.yandex.market.clean.domain.usecase.pickup.renewal.CanShowPickupRenewalButtonUseCase;
import ru.yandex.market.clean.domain.usecase.product.GetDetailedSkuUseCase;
import ru.yandex.market.clean.domain.usecase.product.GetReviewsDeeplinkDataUseCase;
import ru.yandex.market.clean.domain.usecase.sis.AddDirectSisBusinessIdUseCase;
import ru.yandex.market.clean.domain.usecase.upselllanding.SetSessionPageViewUniqueIdUseCase;
import ru.yandex.market.clean.presentation.feature.cart.CartDeeplink;
import ru.yandex.market.common.featureconfigs.managers.LiveStreamToggleManager;
import ru.yandex.market.common.featureconfigs.managers.UpsellLandingFeatureToggleManager;
import ru.yandex.market.common.featureconfigs.managers.WebViewWhiteListConfigManager;
import ru.yandex.market.common.featureconfigs.models.WebViewWhiteListConfig;
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider;
import ru.yandex.market.data.deeplinks.links.BrowserDeeplink;
import ru.yandex.market.data.deeplinks.links.CatalogDeeplink;
import ru.yandex.market.data.deeplinks.links.CatalogListDeeplink;
import ru.yandex.market.data.deeplinks.links.Deeplink;
import ru.yandex.market.data.deeplinks.links.MainScreenDeeplink;
import ru.yandex.market.data.deeplinks.links.MarketWebDeeplink;
import ru.yandex.market.data.deeplinks.links.SearchDeeplink;
import ru.yandex.market.data.deeplinks.links.SmartCoinInfoDeeplink;
import ru.yandex.market.data.deeplinks.links.SmartCoinListDeeplink;
import ru.yandex.market.data.deeplinks.links.order.OrderDeeplink;
import ru.yandex.market.data.deeplinks.links.order.TrackingOrderDeeplink;
import ru.yandex.market.data.deeplinks.links.product.ProductDeeplink;
import ru.yandex.market.data.deeplinks.links.whitemarket.GetSkuIdDeeplinkUseCase;
import ru.yandex.market.data.deeplinks.params.QueryParam;
import ru.yandex.market.data.deeplinks.params.QueryType;
import ru.yandex.market.deeplinks.DeeplinkSource;
import ru.yandex.market.domain.ecom.question.usecase.TriggerEcomQuestionUseCase;
import ru.yandex.market.events.navigation.NavigationTarget;
import ru.yandex.market.feature.manager.FmcgRedesignFeatureManager;
import ru.yandex.market.manager.AuthManager;
import ru.yandex.market.ui.view.mvp.cartcounterbutton.GetCartItemUseCase;
import ru.yandex.market.web.MarketHostProvider;
import ru.yandex.market.web.MarketWebUrlProviderFactory;

import androidx.test.core.app.ApplicationProvider;
import dagger.Lazy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class DeeplinkParserTest extends BaseTest {

    private final DeeplinkParserComposite parser;

    private static void assertMainScreenDeeplink(final Deeplink deeplink,
            NavigationTarget target) {
        assertThat(deeplink, instanceOf(MainScreenDeeplink.class));
        MainScreenDeeplink mainScreenDeeplink = (MainScreenDeeplink) deeplink;
        assertThat(mainScreenDeeplink.getTab(), equalTo(target));
        assertThat(deeplink.getParams(), empty());
    }

    public DeeplinkParserTest() {
        final AuthManager authManager = Mockito.mock(AuthManager.class);
        final GetNavigationNodeUseCase getNavigationNodeUseCase = Mockito.mock(GetNavigationNodeUseCase.class);
        @SuppressWarnings("deprecation")
        final ru.yandex.market.analitycs.AnalyticsService analyticsService
                = Mockito.mock(ru.yandex.market.analitycs.AnalyticsService.class);
        final MiscellaneousAnalyticsFacade miscellaneousAnalyticsFacade =
                Mockito.mock(MiscellaneousAnalyticsFacade.class);
        final GetSkuIdDeeplinkUseCase getSkuIdDeeplinkUseCase = Mockito.mock(GetSkuIdDeeplinkUseCase.class);
        final GetOrderUseCase getOrderUseCase = Mockito.mock(GetOrderUseCase.class);
        final LiveStreamToggleManager liveStreamToggleManager = Mockito.mock(LiveStreamToggleManager.class);

        final WebViewWhiteListConfig webViewWhiteListConfig = Mockito.mock(WebViewWhiteListConfig.class);
        when(webViewWhiteListConfig.isEnabled()).thenReturn(true);
        when(webViewWhiteListConfig.getSafeHosts()).thenReturn(List.of("market.yandex.ru"));

        final WebViewWhiteListConfigManager webViewWhiteListConfigManager = Mockito.mock(WebViewWhiteListConfigManager.class);
        when(webViewWhiteListConfigManager.get()).thenReturn(webViewWhiteListConfig);

        final FeatureConfigsProvider featureConfigsProvider = Mockito.mock(FeatureConfigsProvider.class);
        when(featureConfigsProvider.getWebViewWhiteListConfigManager()).thenReturn(webViewWhiteListConfigManager);

        final MarketHostProvider marketHostProvider = Mockito.mock(MarketHostProvider.class);
        final CanShowPickupRenewalButtonUseCase canShowPickupRenewalButtonUseCase =
                Mockito.mock(CanShowPickupRenewalButtonUseCase.class);
        final Lazy<MarketWebUrlProviderFactory> marketWebUrlProviderFactory =
                Mockito.mock(Lazy.class);
        final TriggerEcomQuestionUseCase ecomQuestionUseCase = Mockito.mock(TriggerEcomQuestionUseCase.class);
        final GetReviewsDeeplinkDataUseCase getReviewsDeeplinkDataUseCase =
                Mockito.mock(GetReviewsDeeplinkDataUseCase.class);
        final GetCartItemUseCase getCartItemUseCase = Mockito.mock(GetCartItemUseCase.class);
        final UpdateCartItemUseCase updateCartItemUseCase = Mockito.mock(UpdateCartItemUseCase.class);
        final OfferAffectingInformationUseCase offerAffectingInformationUseCase = Mockito.mock(
                OfferAffectingInformationUseCase.class);
        final GetDetailedSkuUseCase getDetailedSkuUseCase = Mockito.mock(GetDetailedSkuUseCase.class);
        final AddSeveralOffersToCartUseCase addSeveralOffersToCartUseCase = Mockito.mock(
                AddSeveralOffersToCartUseCase.class);
        final AddDirectSisBusinessIdUseCase addDirectSisBusinessIdUseCase = Mockito.mock(
                AddDirectSisBusinessIdUseCase.class);
        final CallCourierAnalytics callCourierAnalytics = Mockito.mock(CallCourierAnalytics.class);
        final FmcgRedesignFeatureManager fmcgRedesignFeatureManager = Mockito.mock(FmcgRedesignFeatureManager.class);
        final UpsellLandingFeatureToggleManager upsellLandingFeatureToggleManager = Mockito.mock(UpsellLandingFeatureToggleManager.class);
        final SetSessionPageViewUniqueIdUseCase setSessionPageViewUniqueIdUseCase = Mockito.mock(SetSessionPageViewUniqueIdUseCase.class);

        parser = new DeeplinkParserComposite(
                new SimpleDeeplinkParser(DeeplinkUtils.SCHEME_BERU,
                        authManager, getNavigationNodeUseCase, analyticsService, callCourierAnalytics,
                        getSkuIdDeeplinkUseCase,
                        liveStreamToggleManager, getOrderUseCase,
                        featureConfigsProvider,
                        marketHostProvider, canShowPickupRenewalButtonUseCase, marketWebUrlProviderFactory,
                        miscellaneousAnalyticsFacade, getReviewsDeeplinkDataUseCase, ecomQuestionUseCase,
                        getCartItemUseCase, updateCartItemUseCase, offerAffectingInformationUseCase,
                        getDetailedSkuUseCase, addSeveralOffersToCartUseCase, addDirectSisBusinessIdUseCase,
                        fmcgRedesignFeatureManager, upsellLandingFeatureToggleManager, setSessionPageViewUniqueIdUseCase),
                new SimpleDeeplinkParser(DeeplinkUtils.SCHEME_BLUEMARKET,
                        authManager, getNavigationNodeUseCase, analyticsService, callCourierAnalytics,
                        getSkuIdDeeplinkUseCase,
                        liveStreamToggleManager, getOrderUseCase,
                        featureConfigsProvider,
                        marketHostProvider, canShowPickupRenewalButtonUseCase, marketWebUrlProviderFactory,
                        miscellaneousAnalyticsFacade, getReviewsDeeplinkDataUseCase, ecomQuestionUseCase,
                        getCartItemUseCase, updateCartItemUseCase, offerAffectingInformationUseCase,
                        getDetailedSkuUseCase, addSeveralOffersToCartUseCase, addDirectSisBusinessIdUseCase,
                        fmcgRedesignFeatureManager, upsellLandingFeatureToggleManager, setSessionPageViewUniqueIdUseCase),
                new SimpleDeeplinkParser(DeeplinkUtils.SCHEME_YAMARKET,
                        authManager, getNavigationNodeUseCase, analyticsService, callCourierAnalytics,
                        getSkuIdDeeplinkUseCase,
                        liveStreamToggleManager, getOrderUseCase,
                        featureConfigsProvider,
                        marketHostProvider, canShowPickupRenewalButtonUseCase, marketWebUrlProviderFactory,
                        miscellaneousAnalyticsFacade, getReviewsDeeplinkDataUseCase, ecomQuestionUseCase,
                        getCartItemUseCase, updateCartItemUseCase, offerAffectingInformationUseCase,
                        getDetailedSkuUseCase, addSeveralOffersToCartUseCase, addDirectSisBusinessIdUseCase,
                        fmcgRedesignFeatureManager, upsellLandingFeatureToggleManager, setSessionPageViewUniqueIdUseCase),
                new SimpleDeeplinkParser(DeeplinkUtils.SCHEME_POKUPKI,
                        authManager, getNavigationNodeUseCase, analyticsService, callCourierAnalytics,
                        getSkuIdDeeplinkUseCase,
                        liveStreamToggleManager, getOrderUseCase,
                        featureConfigsProvider,
                        marketHostProvider, canShowPickupRenewalButtonUseCase, marketWebUrlProviderFactory,
                        miscellaneousAnalyticsFacade, getReviewsDeeplinkDataUseCase, ecomQuestionUseCase,
                        getCartItemUseCase, updateCartItemUseCase, offerAffectingInformationUseCase,
                        getDetailedSkuUseCase, addSeveralOffersToCartUseCase, addDirectSisBusinessIdUseCase,
                        fmcgRedesignFeatureManager, upsellLandingFeatureToggleManager, setSessionPageViewUniqueIdUseCase),
                new YaccDeeplinkParser(),
                analyticsService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidScheme() {
        final Deeplink deeplink = parser.parse(Uri.parse("https://mail.yandex.ru"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, nullValue());
    }

    @Test
    public void testHttpsMain() {
        final Deeplink deeplink = parser.parse(Uri.parse("https://beru.ru"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertMainScreenDeeplink(deeplink, NavigationTarget.MAIN_PAGE);
    }

    @Test
    public void testHttpMain() {
        final Deeplink deeplink = parser.parse(Uri.parse("http://beru.ru"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertMainScreenDeeplink(deeplink, NavigationTarget.MAIN_PAGE);
    }

    @Test
    public void testMainScreenUri() {
        final Deeplink deeplink = parser.parse(Uri.parse("beru://"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertMainScreenDeeplink(deeplink, NavigationTarget.MAIN_PAGE);
    }

    @Test
    public void testCatalogScreen() {
        final Deeplink deeplink = parser.parse(Uri.parse("beru://catalog/55335/list?" +
                "glfilter=2142588918,6767&" +
                "pricefrom=500&" +
                "priceto=1000&" +
                "in_stock=1&" +
                "text=ttt&" +
                "show-book-now-only=1"), DeeplinkSource.SIMPLE_DEEPLINK);

        assertThat(deeplink, instanceOf(CatalogListDeeplink.class));
    }

    @Test
    public void testSimpleCatalogScreen() {
        final Deeplink deeplink = parser.parse(Uri.parse("yamarket://catalog/21448850"), DeeplinkSource.SIMPLE_DEEPLINK);

        assertThat(deeplink, instanceOf(CatalogDeeplink.class));
        assertThat(deeplink.getUri().toString(), equalTo("yamarket://catalog/21448850"));
    }

    @Test
    public void testCatalogScreenWithMaskedChars() {
        final Deeplink deeplink = parser.parse(Uri.parse("yamarket://catalog%2F21448850"), DeeplinkSource.SIMPLE_DEEPLINK);

        assertThat(deeplink, instanceOf(CatalogDeeplink.class));
        assertThat(deeplink.getUri().toString(), equalTo("yamarket://catalog/21448850"));
    }

    @Test
    public void testCatalogScreenWithDoubleMasketChars() {
        final Deeplink deeplink = parser.parse(Uri.parse("yamarket://catalog%2F%2521448850"), DeeplinkSource.SIMPLE_DEEPLINK);

        assertThat(deeplink, instanceOf(CatalogDeeplink.class));
        assertThat(deeplink.getUri().toString(), equalTo("yamarket://catalog/%21448850"));
    }

    @Test
    public void testModelScreen() {
        final Deeplink deeplink = parser.parse(Uri.parse("beru://product/12840640?hid=6427100"),
                DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(ProductDeeplink.class));
        assertThat(deeplink.getParams(), containsInAnyOrder(
                new QueryParam(QueryType.HID, "6427100"),
                new QueryParam(QueryType.PRODUCT_ID, "12840640")
        ));
    }

    @Test
    public void testOrdersScreen() {
        final Deeplink deeplink = parser.parse(Uri.parse("beru://my/orders"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertMainScreenDeeplink(deeplink, NavigationTarget.ORDERS);
    }

    @Test
    public void testConcreteOrder() {
        final Deeplink deeplink = parser.parse(Uri.parse("beru://my/orders/12345"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(OrderDeeplink.class));
        assertThat(deeplink.getParams(),
                containsInAnyOrder(new QueryParam(QueryType.ORDER_ID, "12345"))
        );
    }

    @Test
    public void testTrackingOrder() {
        final Deeplink deeplink = parser.parse(Uri.parse("beru://my/orders/12345/tracking"),
                DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(TrackingOrderDeeplink.class));
        assertThat(deeplink.getParams(),
                containsInAnyOrder(new QueryParam(QueryType.ORDER_ID, "12345"))
        );
    }

    @Test
    public void testCartScreen() {
        final Deeplink deeplink = parser.parse(Uri.parse("beru://my/cart"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(CartDeeplink.class));
    }

    @Test
    public void testTextIds() {
        Deeplink deeplink = parser.parse(Uri.parse("beru://product/fff"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(ProductDeeplink.class));
        assertThat(deeplink.getParams(), containsInAnyOrder(new QueryParam(QueryType.PRODUCT_ID, "fff")));

        deeplink = parser.parse(Uri.parse("beru://my/orders/aaaa"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(OrderDeeplink.class));
        assertThat(deeplink.getParams(),
                containsInAnyOrder(new QueryParam(QueryType.ORDER_ID, "aaaa")));
    }

    @Test
    public void testSearchDeeplink() {
        final Deeplink deeplink = parser.parse(Uri.parse("beru://search?text=пылесосы"),
                DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(SearchDeeplink.class));
    }

    @Test
    public void testCorrectHttpScheme() {
        final Deeplink deeplink = parser
                .parse(Uri.parse("http://beru.ru/product/12840640?hid=6427100"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(ProductDeeplink.class));
    }

    @Test
    public void testCorrectHttpMobileHostScheme() {
        final Deeplink deeplink = parser
                .parse(Uri.parse("http://m.beru.ru/product/12840640?hid=6427100"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(ProductDeeplink.class));
    }

    @Test
    public void testUnknownHttpSchemeUri() {
        final Deeplink deeplink = parser.parse(Uri.parse("http://beru.ru/incorrect/path"),
                DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(MarketWebDeeplink.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownHttpHostSchemeUri() {
        final Deeplink deeplink = parser
                .parse(Uri.parse("http://yandex.ru/product/12840640?hid=6427100"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, nullValue());
    }

    @Test
    public void testCorrectHttpsScheme() {
        final Deeplink deeplink = parser
                .parse(Uri.parse("https://beru.ru/product/12840640?hid=6427100"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(ProductDeeplink.class));
    }

    @Test
    public void testCorrectHttpsMobileHostScheme() {
        final Deeplink deeplink = parser
                .parse(Uri.parse("https://m.beru.ru/product/12840640?hid=6427100"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(ProductDeeplink.class));
    }

    @Test
    public void testUnknownHttpsSchemeUri() {
        final Deeplink deeplink = parser.parse(Uri.parse("https://beru.ru/incorrect/path"),
                DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(MarketWebDeeplink.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownHttpsHostSchemeUri() {
        final Deeplink deeplink = parser
                .parse(Uri.parse("https://yandex.ru/product/12840640?hid=6427100"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, nullValue());
    }

    @Test
    public void testBrowserLinkWithMaskedChars() {
        final Deeplink deeplink = parser.parse(Uri.parse("yamarket://browser?hybrid-mode=1&url=https%3A%2F%2Fmarket.yandex.ru%2Fspecial%2Fcheapest-as-gift-4-5-landing%3FshopPromoId%3D%252318268%26pokupki%3D1%26cpa%3D1%26utm_campaign%3Dpromo_beauty_p_sale54_25_06_a%26clid%3D621%26utm_referrer%3D621%26utm_source%3Dpush_android%26utm_medium%3Dmassmail"),
                DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(BrowserDeeplink.class));
        assertThat(deeplink.getUri().toString(), equalTo("yamarket://browser?hybrid-mode=1&url=https%3A%2F%2Fmarket.yandex.ru%2Fspecial%2Fcheapest-as-gift-4-5-landing%3FshopPromoId%3D%252318268%26pokupki%3D1%26cpa%3D1%26utm_campaign%3Dpromo_beauty_p_sale54_25_06_a%26clid%3D621%26utm_referrer%3D621%26utm_source%3Dpush_android%26utm_medium%3Dmassmail"));
    }

    @Test
    public void testSpecialLinkWithMaskedChars() {
        final Deeplink deeplink = parser.parse(Uri.parse("https://market.yandex.ru/special/cheapest-as-gift-4-5-landing?shopPromoId=%2318268&pokupki=1&cpa=1&utm_campaign=promo_beauty_p_sale54_25_06_a&clid=621&utm_referrer=621&utm_source=push_android&utm_medium=massmail"),
                DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(MarketWebDeeplink.class));
        assertThat(deeplink.getUri().toString(), equalTo("https://market.yandex.ru/special/cheapest-as-gift-4-5-landing?shopPromoId=%2318268&pokupki=1&cpa=1&utm_campaign=promo_beauty_p_sale54_25_06_a&clid=621&utm_referrer=621&utm_source=push_android&utm_medium=massmail"));
    }

    @Test
    public void testCorrectSmartCoins() {
        final String expectedCoinId = "12840640";
        final Deeplink deeplink = parser.parse(Uri.parse("beru://bonus/" + expectedCoinId),
                DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(SmartCoinInfoDeeplink.class));
        try {
            deeplink.resolve(ApplicationProvider.getApplicationContext());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        final String coinId = Optional.ofNullable(((SmartCoinInfoDeeplink) deeplink).getSmartCoinId()).orElseThrow();
        Assert.assertEquals(expectedCoinId, coinId);
    }

    @Test
    public void testCorrectSmartCoinsList() {
        final Deeplink deeplink = parser.parse(Uri.parse("beru://bonus"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(SmartCoinListDeeplink.class));
    }

    @Test
    public void testCorrectSmartCoinsListWithSlash() {
        final Deeplink deeplink = parser.parse(Uri.parse("beru://bonus/"), DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(SmartCoinListDeeplink.class));
    }

    @Test
    public void testProductDeeplinkWithOfferIdFromHttpAddress() {
        final Uri uri = Uri.parse("https://beru.ru/product/abc/100?show-uid=158&offerid=wLIPp48nPx1SD4T47TH5gg");
        final Deeplink deeplink = parser.parse(uri, DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(ProductDeeplink.class));

        final ProductDeeplink castedDeeplink = (ProductDeeplink) deeplink;
        assertThat(
                castedDeeplink.getParams(),
                containsInAnyOrder(
                        new QueryParam(QueryType.OFFER_ID, "wLIPp48nPx1SD4T47TH5gg"),
                        new QueryParam(QueryType.PRODUCT_ID, "100")
                )
        );
    }

    @Test
    public void testYaccLinkHttpAddress() {
        final Uri uri = Uri.parse("https://ya.cc/m/erfoijsdf");
        final Deeplink deeplink = parser.parse(uri, DeeplinkSource.SIMPLE_DEEPLINK);
        assertThat(deeplink, instanceOf(MarketWebDeeplink.class));
    }
}
