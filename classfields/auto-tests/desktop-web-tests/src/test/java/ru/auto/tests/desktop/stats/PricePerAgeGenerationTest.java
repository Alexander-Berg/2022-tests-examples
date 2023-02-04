package ru.auto.tests.desktop.stats;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.STATS)
@DisplayName("Статистика - как дешеевеет авто с возрастом")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PricePerAgeGenerationTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/StatsSummaryGeneration",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        urlSteps.testing().path(STATS).path(CARS).path("/audi/a3/20785010/").open();
        basePageSteps.onStatsPage().pricePerAge().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Как дешевеет это авто с возрастом")
    public void shouldSeePricePerAge() {
        basePageSteps.onStatsPage().pricePerAge().should(hasText("Как дешевеет это авто с возрастом, ₽\n" +
                "Это поколение дешевеет в среднем на 10% в год\n1.92 млн\n1.69 млн\n-12%\n1.46 млн\n-13%\n1.36 млн\n" +
                "-6%\n1.21 млн\n-10%\nНовая\n1 год\n2 года\n3 года\n4 года"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по точке на графике")
    @Category({Regression.class, Testing.class})
    public void shouldClickGraphDot() {
        basePageSteps.onStatsPage().graphDot().waitUntil(isDisplayed()).hover();
        basePageSteps.onStatsPage().popup().waitUntil(isDisplayed());
        basePageSteps.onStatsPage().popup().button().waitUntil(isDisplayed()).click();
        urlSteps.shouldUrl(anyOf(equalTo(format("%s/cars/new/group/audi/a3/20785010-20785541/?from=single_group_snippet_listing",
                        urlSteps.getConfig().getTestingURI())),
                equalTo(format("%s/moskva/cars/audi/a3/new/", urlSteps.getConfig().getTestingURI()))));
    }
}