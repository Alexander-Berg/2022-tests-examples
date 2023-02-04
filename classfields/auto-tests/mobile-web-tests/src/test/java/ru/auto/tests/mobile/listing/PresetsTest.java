package ru.auto.tests.mobile.listing;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.DATE_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.FRESH_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг объявлений - пресеты")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PresetsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    //@Parameter("Название пресета")
    @Parameterized.Parameter
    public String presetName;

    //@Parameter("Параметр пресета")
    @Parameterized.Parameter(1)
    public String presetParamName;

    //@Parameter("Значение параметра пресета")
    @Parameterized.Parameter(2)
    public String presetParamValue;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Спецпредложения", "special", "true"},
                {"Сниженная цена", "search_tag", "history_discount"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam(presetParamName, presetParamValue).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сброс пресета")
    public void shouldResetPreset() {
        basePageSteps.onListingPage().filters().button(presetName).resetButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button(presetName).waitUntil(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пресет не должен сбрасываться при изменении сортировки")
    public void shouldNotResetPresetAfterSortChange() {
        basePageSteps.onListingPage().sortBar().sort(format("По %s", FRESH_DESC.getName())).click();
        basePageSteps.onListingPage().sortPopup().sort(DATE_DESC.getName()).click();
        urlSteps.addParam("sort", DATE_DESC.getAlias().toLowerCase()).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button(presetName).waitUntil(isDisplayed());
    }
}
