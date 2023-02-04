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
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;

@DisplayName("Базовые фильтры. Категория (дом, участок...)")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseCategoryFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String option;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "Ищем по категории «{0}»")
    public static Collection<Object[]> categoryOfRealty() {
        return asList(new Object[][]{
                {"Квартиру", "kvartira/"},
                {"Комнату", "komnata/"},
                {"Дом", "dom/"},
                {"Участок", "uchastok/"},
                {"Гараж или машиноместо", "garazh/"},
                {"Коммерческую недвижимость", "kommercheskaya-nedvizhimost/"}
        });
    }

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        user.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KURAU)
    public void shouldSeeCategory() {
        user.selectOption(user.onMobileMainPage().searchFilters().selector(KVARTIRU_OPTION), option);
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(expected).shouldNotDiffWithWebDriverUrl();
    }
}
