package ru.yandex.market.data.redirect;

import org.apache.tools.ant.filters.StringInputStream;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.List;

import ru.yandex.market.BaseTest;
import ru.yandex.market.ResourceHelper;
import ru.yandex.market.data.navigation.ISimplifiedFilterValue;
import ru.yandex.market.data.navigation.SimplifiedFilterValue;
import ru.yandex.market.data.navigation.TextSimplifiedFilterValue;
import ru.yandex.market.net.RedirectResponse;
import ru.yandex.market.net.parsers.SimpleApiV2JsonParser;
import ru.yandex.market.net.sku.SkuDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class RedirectParserTest extends BaseTest {

    static RedirectResponse getRedirectResponse(String responseId) {
        String response = ResourceHelper.getResponse("/responses/" + responseId + ".json");
        SimpleApiV2JsonParser<RedirectResponse> parser
                = new SimpleApiV2JsonParser(RedirectResponse.class);
        return parser.parse(new StringInputStream(response));
    }

    @Test
    public void testSearchRedirectParse() {
        RedirectResponse parse
                = getRedirectResponse("redirect/search_redirect");
        RedirectCapiDto redirect = parse.getRedirect();
        assertThat(SearchRedirectCapiDto.class, equalTo(redirect.getClass()));
        assertThat("83745оцриаолцкнпе28973", equalTo(redirect.getSearchText()));
        assertThat(RedirectType.SEARCH, equalTo(redirect.getType()));
    }

    @Test
    public void testUnsupportedRedirectParse() {
        RedirectResponse parse
                = getRedirectResponse("redirect/unsupported_redirect");
        RedirectCapiDto redirect = parse.getRedirect();
        assertThat(UnsupportedRedirectCapiDto.class, equalTo(redirect.getClass()));
        assertThat("bosh_unsupported", equalTo(redirect.getSearchText()));
        assertThat(RedirectType.UNSUPPORTED, equalTo(redirect.getType()));
    }

    @Test
    public void testModelRedirectParse() {
        RedirectResponse parse
                = getRedirectResponse("redirect/model_redirect");
        ModelRedirectCapiDto redirect = (ModelRedirectCapiDto) parse.getRedirect();
        assertThat(RedirectType.MODEL, equalTo(redirect.getType()));
        assertThat(14209841L, equalTo(redirect.getModelId().getId()));
        assertThat("14209841", equalTo(redirect.getModelInfo().getId()));
        assertThat("Apple iPhone 6S 32Gb", equalTo(redirect.getModelInfo().getTitle()));
    }

    @Test
    public void testOfferRedirectParse() {
        RedirectResponse parse
                = getRedirectResponse("redirect/offer_url_transform");
        OfferRedirectCapiDto redirect = (OfferRedirectCapiDto) parse.getRedirect();
        assertThat(RedirectType.OFFER, equalTo(redirect.getType()));
        assertThat("yDpJekrrgZGiN_M4fU_DMMmc8ds_Xukt1RJZ7i-fgyqYuK5CjtC9VA", equalTo(redirect
                .getOfferInfo().getId()));
        assertThat("Мультитул LEATHERMAN Wave Black", equalTo(redirect.getOfferInfo().getName()));
        assertThat("Мультитул LEATHERMAN Wave", equalTo(redirect.getOfferInfo().getModelAwareTitle()));
    }

    @Test
    public void testSkuRedirectParse() {
        RedirectResponse parse
                = getRedirectResponse("redirect/sku_url_transform");
        SkuRedirectCapiDto redirect = (SkuRedirectCapiDto) parse.getRedirect();
        assertThat(RedirectType.SKU, equalTo(redirect.getType()));
        assertThat("100210863677", equalTo(redirect.getSku().map(SkuDto::id).orElse("")));
    }

    @Test
    public void testVendorRedirectParse() {
        RedirectResponse parse
                = getRedirectResponse("redirect/vendor_redirect");

        VendorRedirectCapiDto redirect = (VendorRedirectCapiDto) parse.getRedirect();
        assertThat(RedirectType.VENDOR, equalTo(redirect.getType()));
        assertThat("152900", equalTo(redirect.getVendorId()));
    }

    @Test
    public void testShopOpinionRedirectParse() {
        RedirectResponse parse
                = getRedirectResponse("redirect/shop_opinions_redirect");

        ShopOpinionsRedirectCapiDto redirect = (ShopOpinionsRedirectCapiDto) parse.getRedirect();
        assertThat(RedirectType.SHOP_OPINION, equalTo(redirect.getType()));
        assertThat("234", equalTo(redirect.getShopId()));
    }

    @Test
    public void testPromoRedirectParse() {
        RedirectResponse parse
                = getRedirectResponse("redirect/promo_redirect");
        PromoRedirectCapiDto redirect = (PromoRedirectCapiDto) parse.getRedirect();
        assertThat(RedirectType.PROMO_PAGE, equalTo(redirect.getType()));
        assertThat("54419", equalTo(redirect.getNavigationNodeDto().getId()));
    }


    @Test
    public void testCatalogRedirectParse() {
        RedirectResponse parse
                = getRedirectResponse("redirect/catalog_redirect");
        CatalogRedirectCapiDto redirect = (CatalogRedirectCapiDto) parse.getRedirect();
        assertThat(RedirectType.CATALOG, equalTo(redirect.getType()));
        assertThat("7812157", equalTo(redirect.getHid()));

        List<ISimplifiedFilterValue> filters = redirect.getFilters();
        assertThat(3, equalTo(filters.size()));

        assertThat(filters, Matchers.containsInAnyOrder(
                new SimplifiedFilterValue("7925349", "7925376"),
                new SimplifiedFilterValue("-11", "7299239"),
                new TextSimplifiedFilterValue("-8",
                        "синие джинсы levis",
                        "eJwTcuKSvth4YceFvUC8VeHClgvbLuy92HixWyEntSyzWEgWXXIHsrQUCwe_gKCSORd-ZQJP7zxmFjj26CGzEgsHgwArkOQVENJgAADtfj3-",
                        "[синие] джинсы [levis]")));
    }

}
