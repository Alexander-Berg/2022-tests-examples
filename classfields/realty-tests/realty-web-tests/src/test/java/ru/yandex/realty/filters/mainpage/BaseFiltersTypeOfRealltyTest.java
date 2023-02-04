package ru.yandex.realty.filters.mainpage;

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
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.KVARTIRU_BUTTON;

/**
 * @author kantemirov
 */
@DisplayName("Главная страница. Базовые фильтры.")
@Feature(MAINFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersTypeOfRealltyTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;


    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Квартиру", "/kvartira/"},
                {"Комнату", "/komnata/"},
                {"Дом", "/dom/"},
                {"Участок", "/uchastok/"},
                {"Гараж или машиноместо", "/garazh/"},
                {"Коммерческую недвижимость", "/kommercheskaya-nedvizhimost/"}
        });
    }

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onOffersSearchPage().filters().waitUntil(isDisplayed());
        basePageSteps.onMainPage().filters().deselectCheckBox("1");
        basePageSteps.onMainPage().filters().deselectCheckBox("2");
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. Параметр типа недвижимости")
    public void shouldSeeBuyTypeOfRealtyUrl() {
        basePageSteps.onMainPage().filters().button(KVARTIRU_BUTTON).click();
        basePageSteps.onMainPage().filters().selectPopup().item(label).click();
        basePageSteps.onMainPage().filters().submitButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(expected).shouldNotDiffWithWebDriverUrl();
    }
}