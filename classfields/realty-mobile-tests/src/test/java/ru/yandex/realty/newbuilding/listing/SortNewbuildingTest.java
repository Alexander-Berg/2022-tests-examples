package ru.yandex.realty.newbuilding.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.LISTING;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;
import static ru.yandex.realty.step.UrlSteps.CATEGORY_CODE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.COMMISSIONING_DATE_SORT_VALUE;
import static ru.yandex.realty.step.UrlSteps.KVARTIRA_VALUE;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.PRICE_SORT_VALUE;
import static ru.yandex.realty.step.UrlSteps.RELEVANCE_SORT_VALUE;
import static ru.yandex.realty.step.UrlSteps.SORT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@Issue("VERTISTEST-1352")
@Epic(NEWBUILDING)
@Feature(LISTING)
@DisplayName("Сортировки на новостройках")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SortNewbuildingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сортировка «Цена по возрастанию»")
    public void shouldSeePriceSort() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).open();

        basePageSteps.onNewBuildingPage().sortSelect().click();
        basePageSteps.onNewBuildingPage().option("цена по возрастанию").click();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(SORT_URL_PARAM, PRICE_SORT_VALUE)
                .queryParam(CATEGORY_CODE_URL_PARAM, KVARTIRA_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сортировка «По сроку сдачи»")
    public void shouldSeeComissioningDateSort() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).open();

        basePageSteps.onNewBuildingPage().sortSelect().click();
        basePageSteps.onNewBuildingPage().option("по сроку сдачи").click();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(SORT_URL_PARAM, COMMISSIONING_DATE_SORT_VALUE)
                .queryParam(CATEGORY_CODE_URL_PARAM, KVARTIRA_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сортировка «По актуальности»")
    public void shouldSeeRelevanceSort() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(SORT_URL_PARAM, PRICE_SORT_VALUE).open();

        basePageSteps.onNewBuildingPage().sortSelect().click();
        basePageSteps.onNewBuildingPage().option("по актуальности").click();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(SORT_URL_PARAM, RELEVANCE_SORT_VALUE)
                .queryParam(CATEGORY_CODE_URL_PARAM, KVARTIRA_VALUE).shouldNotDiffWithWebDriverUrl();
    }
}
