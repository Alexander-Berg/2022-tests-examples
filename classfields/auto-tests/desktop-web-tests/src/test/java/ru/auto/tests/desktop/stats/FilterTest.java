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

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Feature(AutoruFeatures.STATS)
@DisplayName("Статистика - фильтр")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FilterTest {

    private static final String MARK = "Audi";
    private static final String SECOND_MARK = "bmw";
    private static final String MODEL = "A3";
    private static final String GENERATION = "IV (8Y)";
    private static final String GENERATION_SEARCHER = "21837610";
    private static final String BODY = "Хэтчбек 5 дв. sportback";
    private static final String BODY_SEARCHER = "21837711";
    private static final String COMPLECTATION = "1.4 AT 150 л.c. – Sport 35 TFSI tiptronic";
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
    @DisplayName("Выбор марки")
    public void shouldSelectMark() {
        urlSteps.testing().path(STATS).path(CARS).path(SECOND_MARK).open();
        basePageSteps.onStatsPage().mmmFilter().selectMark(MARK);
        urlSteps.testing().path(STATS).path(CARS).path(MARK.toLowerCase()).path("/").shouldNotSeeDiff();
        basePageSteps.onStatsPage().mmmFilter().modelSelect().waitUntil(isEnabled());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели")
    public void shouldSelectModel() {
        urlSteps.testing().path(STATS).path(CARS).path(MARK.toLowerCase()).open();
        basePageSteps.onStatsPage().mmmFilter().selectModel(MODEL);
        urlSteps.testing().path(STATS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path("/").shouldNotSeeDiff();
        basePageSteps.onStatsPage().mmmFilter().generationSelect().waitUntil(isEnabled());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор поколения")
    public void shouldSelectGeneration() {
        urlSteps.testing().path(STATS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase()).open();
        basePageSteps.onStatsPage().mmmFilter().selectGenerationInDropdown(GENERATION);
        urlSteps.testing().path(STATS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION_SEARCHER).path("/").shouldNotSeeDiff();
        basePageSteps.onStatsPage().mmmFilter().bodySelect().waitUntil(isEnabled());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор кузова")
    public void shouldSelectBody() {
        urlSteps.testing().path(STATS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION_SEARCHER).open();
        basePageSteps.onStatsPage().mmmFilter().selectBody(BODY);
        urlSteps.testing().path(STATS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION_SEARCHER).path(BODY_SEARCHER).path("/").shouldNotSeeDiff();
        basePageSteps.onStatsPage().mmmFilter().complectationSelect().waitUntil(isEnabled());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор комплектации")
    public void shouldSelectComplectation() {
        urlSteps.testing().path(STATS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION_SEARCHER).path(BODY_SEARCHER).open();
        basePageSteps.onStatsPage().mmmFilter().selectComplectation(COMPLECTATION);
        urlSteps.testing().path(STATS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION_SEARCHER).path(BODY_SEARCHER).path(COMPLECTATION_SEARCHER).path("/")
                .shouldNotSeeDiff();
    }
}