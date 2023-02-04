package ru.yandex.realty.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.LISTING;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.page.SaleAdsPage.SHOW_NEXT;
import static ru.yandex.realty.mobile.page.SaleAdsPage.SHOW_PREV;
import static ru.yandex.realty.step.UrlSteps.PAGE_SIZE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.PAGE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.RELEVANCE_SORT_VALUE;
import static ru.yandex.realty.step.UrlSteps.SORT_URL_PARAM;

@Issue("VERTISTEST-1352")
@Feature(LISTING)
@DisplayName("Пейджинг")
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
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проставление первого пейджа при скроллинге")
    public void shouldSeePageOneAfterScroll() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onMobileSaleAdsPage().lastOffer().hover();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_URL_PARAM, "1")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проставление второго пейджа при скроллинге")
    public void shouldSeePageSecondAfterScroll() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.scrollingUntil(() -> basePageSteps.getDriver().getCurrentUrl(), equalTo(
                urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_URL_PARAM, "2")
                        .toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка в «Показать предыдущие» без параметра page")
    public void shouldSeePrevPageWithoutPageParamUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_URL_PARAM, "1").open();

        basePageSteps.onMobileSaleAdsPage().link(SHOW_PREV).should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_SIZE_URL_PARAM, "10")
                        .queryParam(SORT_URL_PARAM, RELEVANCE_SORT_VALUE).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка в «Показать предыдущие» с параметром page")
    public void shouldSeePrevPageWithPageParamUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_URL_PARAM, "2").open();

        basePageSteps.onMobileSaleAdsPage().link(SHOW_PREV).should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_SIZE_URL_PARAM, "10")
                        .queryParam(PAGE_URL_PARAM, "1")
                        .queryParam(SORT_URL_PARAM, RELEVANCE_SORT_VALUE).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка в «Показать ещё»")
    public void shouldSeeNextPageUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();

        basePageSteps.onMobileSaleAdsPage().link(SHOW_NEXT).should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(PAGE_SIZE_URL_PARAM, "10")
                        .queryParam(SORT_URL_PARAM, RELEVANCE_SORT_VALUE)
                        .queryParam(PAGE_URL_PARAM, "1").toString())));
    }

}
