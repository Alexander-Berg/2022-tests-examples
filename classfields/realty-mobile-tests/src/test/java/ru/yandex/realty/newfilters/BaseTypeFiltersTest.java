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
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.KUPIT_OPTION;

@DisplayName("Базовые фильтры. Купить снять")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseTypeFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Parameterized.Parameter
    public String option;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "Ищем по типу «{0}»")
    public static Collection<Object[]> rentType() {
        return asList(new Object[][]{
                {"Купить", "kupit/kvartira/"},
                {"Снять", "snyat/kvartira/"},
                {"Посуточно", "snyat/kvartira/posutochno/"},
        });
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KURAU)
    public void shouldSeeRentType() {
        basePageSteps.selectOption(basePageSteps.onMobileMainPage().searchFilters().selector(KUPIT_OPTION), option);
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.path(expected).shouldNotDiffWithWebDriverUrl();
    }

}
