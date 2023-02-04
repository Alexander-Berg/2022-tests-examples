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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Базовые фильтры. Вторичка/Новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BaseNewFlatFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        user.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Параметр «Вторичка»")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeNewFlatNoInUrl() {
        user.onMobileMainPage().searchFilters().newFlat().button("Вторичка").click();
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path("/vtorichniy-rynok/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Параметр «Новостройка»")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeNovostrojkaInUrl() {
        user.onMobileMainPage().searchFilters().newFlat().button("Новостройки").click();
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path("novostrojka/").queryParam("newFlat", "YES")
                .shouldNotDiffWithWebDriverUrl();
    }
}
