package ru.auto.tests.desktop.listing.sortbar;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - сортировки - отображение")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SortScreenshotMotoTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {MOTORCYCLE, ALL}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchMotoBreadcrumbs",
                "desktop/SearchMotoAll").post();

        urlSteps.testing().path(category).path(section).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение панели сортировок")
    public void shouldSeeSortBar() {
        basePageSteps.onListingPage().sortBar().should(hasText("Сортировка\nВремя размещения\nПродажа с НДС"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение выпадушки сортировок")
    public void shouldSeeSortPopup() {
        basePageSteps.onListingPage().sortBar().select("Сортировка").click();
        basePageSteps.onListingPage().sortBar().selectPopup().waitUntil(isDisplayed())
                .should(hasText("По актуальности\nПо дате размещения\nПо возрастанию цены\nПо убыванию цены\n" +
                        "По году: новее\nПо году: старше\nПо пробегу\nПо названию"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение выпадушки периодов")
    public void shouldSeeTopDaysPopup() {
        basePageSteps.onListingPage().sortBar().select("Время размещения").hover().click();
        basePageSteps.onListingPage().sortBar().selectPopup().waitUntil(isDisplayed())
                .should(hasText("За все время\nЗа сутки\nЗа 2 дня\nЗа 3 дня\nЗа неделю\nЗа 2 недели\nЗа 3 недели\n" +
                        "За месяц"));
    }
}