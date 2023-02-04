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
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.UCHASTOK;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.element.main.FilterPopup.PER_ARE;
import static ru.yandex.realty.mobile.element.main.FilterPopup.PER_METER;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.LOT_OPTION;

@DisplayName("Базовые фильтры. Параметры в урле от/до/м²")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BasePriceFiltersTest {

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
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KURAU)
    @DisplayName("Параметр цена «от»")
    public void shouldSeePriceMinInUrl() {
        String priceMin = valueOf(getRandomShortInt());
        user.onMobileMainPage().searchFilters().priceMin().sendKeys(priceMin);
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();

        urlSteps.path(KUPIT).path(KVARTIRA).queryParam("priceMin", priceMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KURAU)
    @DisplayName("Параметр цена «до»")
    public void shouldSeePriceMaxInUrl() {
        String priceMax = valueOf(getRandomShortInt() * 10000000);
        user.onMobileMainPage().searchFilters().priceMax().sendKeys(priceMax);
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();

        urlSteps.path(KUPIT).path(KVARTIRA).queryParam("priceMax", priceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Mobile.class, Smoke.class})
    @Owner(KURAU)
    @DisplayName("Параметр цена «за м²»")
    public void shouldSeePricePerM2() {
        user.onMobileMainPage().searchFilters().button(PER_METER).click();
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).queryParam("priceType", "PER_METER").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Mobile.class, Smoke.class})
    @Owner(KURAU)
    @DisplayName("Параметр цена «за сот.»")
    public void shouldSeePricePerAre() {
        user.selectOption(user.onMobileMainPage().searchFilters().selector(KVARTIRU_OPTION), LOT_OPTION);
        user.onMobileMainPage().searchFilters().button(PER_ARE).click();
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(UCHASTOK).queryParam("priceType", "PER_ARE").shouldNotDiffWithWebDriverUrl();
    }
}
