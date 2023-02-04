package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.DOM_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;
import static ru.yandex.realty.step.UrlSteps.OBJECT_TYPE_URL_PARAM;

@DisplayName("Фильтры Дома. Тип загородной недвижимости")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BaseHouseGroupTypeTest {


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA_I_MO).open();
        user.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        user.selectOption(user.onMobileMainPage().searchFilters().selector(KVARTIRU_OPTION), DOM_OPTION);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «Коттеджный пос.»")
    public void shouldSelectHouseGroupTypeKp() {
        user.onMobileMainPage().searchFilters().button("Коттеджный пос.").click();
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();

        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam("category", "HOUSE")
                .queryParam(OBJECT_TYPE_URL_PARAM, "VILLAGE").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «Вторичка»")
    public void shouldSelectHouseGroupTypeSecondary() {
        user.onMobileMainPage().searchFilters().button("Вторичка").click();
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();

        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(DOM).queryParam(OBJECT_TYPE_URL_PARAM, "OFFER")
                .shouldNotDiffWithWebDriverUrl();
    }
}
