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

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.LISTING;
import static ru.yandex.realty.step.UrlSteps.SORT_URL_PARAM;

@Issue("VERTISTEST-1352")
@Feature(LISTING)
@DisplayName("Сортировка по актуальности")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SortActualTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сортировка по актуальности")
    public void shouldSeeSortActual() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(SORT_URL_PARAM, "DATE_DESC").open();
        basePageSteps.onMobileSaleAdsPage().sortSelect().click();
        basePageSteps.onMobileSaleAdsPage().sortOption("по актуальности").click();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam(SORT_URL_PARAM, "RELEVANCE")
                .shouldNotDiffWithWebDriverUrl();
    }
}
