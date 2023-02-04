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
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.UCHASTOK;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.LOT_OPTION;

@DisplayName("Базовые фильтры. Параметры площадь участка в урле от/до")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BaseAreaLotFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        user.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        user.selectOption(user.onMobileMainPage().searchFilters().selector(KVARTIRU_OPTION), LOT_OPTION);
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KURAU)
    @DisplayName("Параметр площадь участка «от»")
    public void shouldSeeAreaLotMinInUrl() {
        String areaMin = valueOf(getRandomShortInt());
        user.onMobileMainPage().searchFilters().areaLotMin().sendKeys(areaMin);
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();

        urlSteps.path(KUPIT).path(UCHASTOK).queryParam("lotAreaMin", areaMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KURAU)
    @DisplayName("Параметр площадь участка «до»")
    public void shouldSeeAreaLotMaxInUrl() {
        String areaMax = valueOf(getRandomShortInt());
        user.onMobileMainPage().searchFilters().areaLotMax().sendKeys(areaMax);
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();

        urlSteps.path(KUPIT).path(UCHASTOK).queryParam("lotAreaMax", areaMax).shouldNotDiffWithWebDriverUrl();
    }
}
