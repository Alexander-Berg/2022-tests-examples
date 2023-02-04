package ru.auto.tests.desktop.listing.filters;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Базовые фильтры поиска - селекторы, ЧПУ")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersSelectorsSeoTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    //@Parameter("Секция")
    @Parameterized.Parameter(1)
    public String section;

    //@Parameter("Селект")
    @Parameterized.Parameter(2)
    public String selectName;

    //@Parameter("Опция в селекте")
    @Parameterized.Parameter(3)
    public String selectItem;

    //@Parameter("Параметр")
    @Parameterized.Parameter(4)
    public String param;

    @Parameterized.Parameters(name = "{0} {1} {4}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Кузов", "Седан ", "/body-sedan/"},
                {CARS, ALL, "Кузов", "Хэтчбек ", "/body-hatchback/"},
                {CARS, ALL, "Кузов", "\u00a0\u00a0Хэтчбек 3 дв. ", "/body-hatchback_3_doors/"},
                {CARS, ALL, "Двигатель", "Бензин", "engine-benzin"},
                {CARS, ALL, "Двигатель", "Газобаллонное оборудование", "/engine-lpg/"},
                {CARS, ALL, "Привод", "Передний", "drive-forward_wheel"},
                {CARS, ALL, "Коробка", "Автомат ", "/transmission-automatic/"},
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор опции в селекте")
    public void shouldSelectItem() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
        basePageSteps.onListingPage().filter().selectItem(selectName, selectItem);
        urlSteps.path(param).path("/").shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().select(selectItem.replaceAll("\u00a0", "").trim())
                .click();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().waitForListingReload();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select(selectItem.replaceAll("\u00a0", "").trim())
                .should(isDisplayed());
    }
}
