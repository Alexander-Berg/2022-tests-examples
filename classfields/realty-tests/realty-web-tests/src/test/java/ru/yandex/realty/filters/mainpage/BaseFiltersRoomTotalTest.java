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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;

/**
 * @author kantemirov
 */
@DisplayName("Главная страница. Базовые фильтры.")
@Feature(MAINFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersRoomTotalTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;


    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Студия", "studiya/"},
                {"1", "odnokomnatnaya/"},
                {"2", "dvuhkomnatnaya/"},
                {"3", "tryohkomnatnaya/"},
                {"4", "4-i-bolee/"}
        });
    }

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().filters().deselectCheckBox("1");
        basePageSteps.onMainPage().filters().deselectCheckBox("2");
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. Параметр «количество комнат»")
    public void shouldSeeRoomTotalParamInUrl() {
        basePageSteps.onMainPage().filters().selectCheckBox(name);
        basePageSteps.onMainPage().filters().submitButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(expected).shouldNotDiffWithWebDriverUrl();
    }
}
