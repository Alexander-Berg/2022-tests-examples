package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Owners.SCROOGE;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.KUPIT_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.SNYAT_OPTION;

@DisplayName("Базовые фильтры. Количество комнат")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseRoomsFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA_I_MO).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "Ищем по количеству комнат «{0}»")
    public static Collection<Object[]> getRoom() {
        return asList(new Object[][]{
                {"Студия", "studiya/"},
                {"1", "odnokomnatnaya/"},
                {"2", "dvuhkomnatnaya/"},
                {"3", "tryohkomnatnaya/"},
                {"4", "4-i-bolee/"}
        });
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KURAU)
    public void shouldSeeRoomForCategorySell() {
        basePageSteps.onMobileMainPage().searchFilters().room().button(label).click();
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path(expected).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(SCROOGE)
    @Category({Regression.class, Mobile.class})
    public void shouldSeeRoomForCategoryRent() {
        basePageSteps.selectOption(basePageSteps.onMobileMainPage().searchFilters().selector(KUPIT_OPTION), SNYAT_OPTION);
        basePageSteps.onMobileMainPage().searchFilters().room().button(label).click();
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.path(SNYAT).path(KVARTIRA).path(expected).shouldNotDiffWithWebDriverUrl();
    }
}
