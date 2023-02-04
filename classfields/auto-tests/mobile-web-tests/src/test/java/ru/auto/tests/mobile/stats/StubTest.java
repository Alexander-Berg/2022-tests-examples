package ru.auto.tests.mobile.stats;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.STATS)
@DisplayName("Статистика - заглушка")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class StubTest {

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

    @Parameterized.Parameter
    public String path;

    @Parameterized.Parameter(1)
    public String stubText;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/toyota/", "Чтобы увидеть статистику цен,\nвыберите конкретную модель.\nПерейти в каталог"},
                {"/toyota/corolla/", "У нас недостаточно данных\nВыберите другую марку или модель\n" +
                        "Перейти в каталог"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/AutoruBreadcrumbsToyota",
                "desktop/AutoruBreadcrumbsToyotaCorolla",
                "desktop/StatsSummaryMarkEmpty",
                "desktop/StatsSummaryModelEmpty",
                "desktop/ProxySearcher").post();

        urlSteps.testing().path(STATS).path(CARS).path(path).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заглушка")
    public void shouldSeeStub() {
        basePageSteps.onStatsPage().stub().should(hasText(stubText));
        basePageSteps.onStatsPage().stubCatalogUrl().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(path).shouldNotSeeDiff();
    }
}
