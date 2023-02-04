package ru.auto.tests.mobile.filters;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Расширенные фильтры - сброс ненужных параметров при смене секции")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsSectionsResetParamsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String params;

    @Parameterized.Parameter(3)
    public String sectionTitle;

    @Parameterized.Parameter(4)
    public String sectionUrl;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "dealer_org_type=4&is_clear=true&search_tag=certificate&km_age_from=100" +
                        "&exchange_status=1&warranty_status=1&pts_status=1", "Новые", NEW},
                {CARS, NEW, "only_official=false", "Все", ALL},
                {LCV, ALL, "dealer_org_type=4&is_clear=true&search_tag=certificate&km_age_from=100" +
                        "&exchange_status=1&pts_status=1", "Новые", NEW},
                {MOTORCYCLE, ALL, "exchange_status=1&dealer_org_type=1_2_3_5", "Новые", NEW}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).replaceQuery(params).open();
        basePageSteps.onListingPage().filters().paramsButton().click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс ненужных параметров при смене секции")
    public void shouldResetParams() {
        basePageSteps.onListingPage().paramsPopup().section(sectionTitle).click();
        urlSteps.testing().path(MOSKVA).path(category).path(sectionUrl).ignoreParam("sort").shouldNotSeeDiff();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(category).path(sectionUrl).ignoreParam("sort").shouldNotSeeDiff();
    }
}
