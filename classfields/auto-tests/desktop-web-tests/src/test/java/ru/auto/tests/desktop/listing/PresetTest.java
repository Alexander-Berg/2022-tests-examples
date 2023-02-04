package ru.auto.tests.desktop.listing;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.KOPITSA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.element.SortBar.SortBy.DATE_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг объявлений - Просмотр пресетов")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PresetTest {

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

    @Parameterized.Parameters(name = "name = {index}: {1} {2}")
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
    @Owner(KOPITSA)
    @DisplayName("Видим плашку пресетов на листинге")
    public void shouldSeePresetBar() {
        basePageSteps.onListingPage().presetBar()
                .should(hasText(format("Вы смотрите подборку «%s»Сбросить", presetName)));
    }

    @Test
    @Category({Regression.class})
    @Owner(KOPITSA)
    @DisplayName("Закрываем плашку пресета")
    public void shouldClosePreset() {
        basePageSteps.onListingPage().presetBar().waitUntil(isDisplayed());
        basePageSteps.onListingPage().presetBar().button("Сбросить").click();
        basePageSteps.onListingPage().presetBar().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пресет не должен сбрасываться при изменении фильтра")
    public void shouldNotResetPresetAfterFilterChange() {
        basePageSteps.onListingPage().filter().inputGroup("Цена").input("от").sendKeys("1000");
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.addParam("price_from", "1000").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пресет должен сбрасываться при сбросе фильтра")
    public void shouldNotResetPresetAfterFilterReset() {
        basePageSteps.onListingPage().filter().inputGroup("Цена").input("от").sendKeys("1000");
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().filter().resetButton().should(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пресет не должен сбрасываться при изменении сортировки")
    public void shouldNotResetPresetAfterSortChange() {
        basePageSteps.onListingPage().sortBar().selectItem("Сортировка", DATE_DESC.getName());
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.addParam("sort", DATE_DESC.getAlias().toLowerCase()).shouldNotSeeDiff();
    }
}
