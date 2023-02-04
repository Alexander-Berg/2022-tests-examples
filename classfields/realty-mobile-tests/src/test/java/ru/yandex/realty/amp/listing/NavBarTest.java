package ru.yandex.realty.amp.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.Pages.FILTERS;
import static ru.yandex.realty.consts.Pages.KARTA;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;
import static ru.yandex.realty.mobile.page.SaleAdsPage.MAP;
import static ru.yandex.realty.mobile.page.SaleAdsPage.PARAMETERS;
import static ru.yandex.realty.mobile.page.SaleAdsPage.PIXELS_TO_FLOAT_NAV_BAR;
import static ru.yandex.realty.step.UrlSteps.AMP_PARAMETER;
import static ru.yandex.realty.step.UrlSteps.APARTMENT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.CATEGORY_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.MOSCOW_RGID;
import static ru.yandex.realty.step.UrlSteps.RGID;
import static ru.yandex.realty.step.UrlSteps.SELL_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.TYPE_URL_PARAM;

@Link("VERTISTEST-1618")
@Feature(AMP_FEATURE)
@DisplayName("amp. Навбар")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class NavBarTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход на «Параметры»")
    public void shouldGoToParametersAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onAmpSaleAdsPage().link(PARAMETERS).click();

        urlSteps.testing().path(FILTERS).queryParam(RGID, MOSCOW_RGID).queryParam(TYPE_URL_PARAM, SELL_URL_PARAM)
                .ignoreParam(AMP_PARAMETER).queryParam(CATEGORY_URL_PARAM, APARTMENT_URL_PARAM)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход на «Карта»")
    public void shouldGoToMapAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onAmpSaleAdsPage().link(MAP).click();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).ignoreParam(AMP_PARAMETER)
                .ignoreMapCoordinate().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход на «Параметры» с залипшего навбара")
    public void shouldGoToParametersFloatNavbarAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.scroll(PIXELS_TO_FLOAT_NAV_BAR);
        basePageSteps.onAmpSaleAdsPage().link(PARAMETERS).click();

        urlSteps.testing().path(FILTERS).queryParam(RGID, MOSCOW_RGID).queryParam(TYPE_URL_PARAM, SELL_URL_PARAM)
                .ignoreParam(AMP_PARAMETER).queryParam(CATEGORY_URL_PARAM, APARTMENT_URL_PARAM)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход на «Карта» с залипшего навбара")
    public void shouldGoToMapFloatNavbarAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.scroll(PIXELS_TO_FLOAT_NAV_BAR);
        basePageSteps.onAmpSaleAdsPage().link(MAP).click();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).ignoreParam(AMP_PARAMETER)
                .ignoreMapCoordinate().shouldNotDiffWithWebDriverUrl();
    }

}
