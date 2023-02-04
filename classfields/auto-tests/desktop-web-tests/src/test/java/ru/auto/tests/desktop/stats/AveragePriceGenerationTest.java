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

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EVALUATION;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.STATS)
@DisplayName("Статистика - средняя цена")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AveragePriceGenerationTest {

    private static final String MARK = "audi";
    private static final String MODEL = "a3";
    private static final String GENERATION = "20785010";

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
        mockRule.newMock().with("desktop/StatsSummaryGeneration",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Средняя цена")
    public void shouldSeeAveragePrice() {
        basePageSteps.onStatsPage().averagePrice().should(hasText("Средняя цена Audi A3 на Авто.ру\n" +
                "Без учёта кузова и комплектации. На основе 30 объявлений от 1 080 000 до 1 849 000 ₽\n" +
                "Оценить мой авто\n1 474 943 ₽"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Оценить мой авто»")
    @Category({Regression.class, Testing.class})
    public void shouldClickEvaluationButton() {
        basePageSteps.onStatsPage().evaluationButton().should(isDisplayed()).click();
        urlSteps.testing().path(CARS).path(EVALUATION).addParam("from", "price_stat").shouldNotSeeDiff();
    }
}