package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER_MARK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.element.SortBar.TopDaysBy;
import static ru.auto.tests.desktop.element.SortBar.TopDaysBy.DAYS_1;
import static ru.auto.tests.desktop.element.SortBar.TopDaysBy.DAYS_2;
import static ru.auto.tests.desktop.element.SortBar.TopDaysBy.DAYS_3;
import static ru.auto.tests.desktop.element.SortBar.TopDaysBy.MONTH_1;
import static ru.auto.tests.desktop.element.SortBar.TopDaysBy.WEEK_1;
import static ru.auto.tests.desktop.element.SortBar.TopDaysBy.WEEK_2;
import static ru.auto.tests.desktop.element.SortBar.TopDaysBy.WEEK_3;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Период отображения объявлений")
@Feature(DEALERS)
@Story(DEALER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardTopDaysTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public TopDaysBy topDaysBy;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {DAYS_1},
                {DAYS_2},
                {DAYS_3},
                {WEEK_1},
                {WEEK_2},
                {WEEK_3},
                {MONTH_1}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsBreadcrumbsMercedes"),
                stub("desktop/SessionUnauth"),
                stub("desktop/Salon"),
                stub("desktop/SearchCarsCountDealerId"),
                stub("desktop/SearchCarsMarkModelFiltersAllDealerIdOneMark"),
                stub("desktop/SearchCarsAllDealerId")
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL)
                .path(CARS_OFFICIAL_DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Проверка периодов (кроме дефолтного)")
    public void shouldSeeTopDaysInUrl() {
        basePageSteps.onListingPage().sortBar().selectItem("Время размещения", topDaysBy.getName());

        urlSteps.path(CARS_OFFICIAL_DEALER_MARK).path("/")
                .addParam("top_days", topDaysBy.getAlias().toLowerCase())
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().sortBar().select(topDaysBy.getName()).should(isDisplayed());
    }
}