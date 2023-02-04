package ru.auto.tests.desktop.stats;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Feature(AutoruFeatures.STATS)
@DisplayName("Статистика - сброс фильтра")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FilterResetTest {

    private static final String MARK = "audi";
    private static final String MODEL = "a3";
    private static final String GENERATION_SEARCHER = "21837610";
    private static final String BODY_SEARCHER = "21837711";
    private static final String COMPLECTATION_SEARCHER = "21837711_22870723_22848425";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс модели")
    public void shouldResetModel() {
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).open();
        basePageSteps.onStatsPage().mmmFilter().selectModel("Сбросить");
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path("/").shouldNotSeeDiff();
        basePageSteps.onStatsPage().mmmFilter().generationSelect().waitUntil(not(isEnabled()));
        basePageSteps.onStatsPage().mmmFilter().bodySelect().waitUntil(not(isEnabled()));
        basePageSteps.onStatsPage().mmmFilter().complectationSelect().waitUntil(not(isEnabled()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс поколения")
    public void shouldResetGeneration() {
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION_SEARCHER).open();
        basePageSteps.onStatsPage().mmmFilter().selectGenerationInDropdown("Сбросить");
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path("/")
                .path(MODEL).path("/").shouldNotSeeDiff();
        basePageSteps.onStatsPage().mmmFilter().bodySelect().waitUntil(not(isEnabled()));
        basePageSteps.onStatsPage().mmmFilter().complectationSelect().waitUntil(not(isEnabled()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс кузова")
    public void shouldResetBody() {
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION_SEARCHER)
                .path(BODY_SEARCHER).open();
        basePageSteps.onStatsPage().mmmFilter().selectBody("Сбросить");
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path("/")
                .path(MODEL).path(GENERATION_SEARCHER).path("/").shouldNotSeeDiff();
        basePageSteps.onStatsPage().mmmFilter().complectationSelect().waitUntil(not(isEnabled()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс комплектации")
    public void shouldResetComplectation() {
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION_SEARCHER)
                .path(BODY_SEARCHER).path(COMPLECTATION_SEARCHER).open();
        basePageSteps.onStatsPage().mmmFilter().selectComplectation("Сбросить");
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path("/")
                .path(MODEL).path(GENERATION_SEARCHER).path(BODY_SEARCHER).path("/").shouldNotSeeDiff();
    }
}