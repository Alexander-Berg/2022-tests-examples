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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.page.SaleAdsPage.SHOW_PREV;
import static ru.yandex.realty.step.UrlSteps.PAGE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.RELEVANCE_SORT_VALUE;
import static ru.yandex.realty.step.UrlSteps.SORT_URL_PARAM;

@Link("VERTISTEST-1618")
@Feature(AMP_FEATURE)
@DisplayName("amp. Пейджинг")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class PagingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проставление первого пейджа при скроллинге")
    public void shouldSeePageOneAfterScrollAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onAmpSaleAdsPage().ampPredLastOffer().hover();
        basePageSteps.scrolling(1000, 100);

        basePageSteps.scrollingUntil(() -> basePageSteps.onAmpSaleAdsPage().ampNextPages(), hasSize(1));
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_URL_PARAM, "1")
                .ignoreParam(SORT_URL_PARAM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Ссылка в «Показать предыдущие» для 1-ой страницы")
    public void shouldSeePrevPageWithoutPageParamUrlAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_URL_PARAM, "1").open();

        basePageSteps.onAmpSaleAdsPage().link(SHOW_PREV).should(hasHref(equalTo(
                urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_URL_PARAM, "0")
                        .queryParam(SORT_URL_PARAM, RELEVANCE_SORT_VALUE).toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Ссылка в «Показать предыдущие» для 2-ой страницы")
    public void shouldSeePrevPageWithPageParamUrlAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_URL_PARAM, "2").open();

        basePageSteps.onAmpSaleAdsPage().link(SHOW_PREV).should(hasHref(equalTo(
                urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_URL_PARAM, "1")
                        .queryParam(SORT_URL_PARAM, RELEVANCE_SORT_VALUE).toString())));
    }
}
