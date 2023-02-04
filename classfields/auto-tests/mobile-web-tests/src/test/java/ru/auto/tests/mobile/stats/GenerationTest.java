package ru.auto.tests.mobile.stats;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MARKS;
import static ru.auto.tests.desktop.consts.Pages.MODELS;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.STATS)
@DisplayName("Статистика по поколению")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GenerationTest {

    private static final String MARK = "audi";
    private static final String MODEL = "a3";
    private static final String GENERATION = "IV (8Y)";
    private static final String GENERATION_ID = "21837610";
    private static final String OTHER_GENERATION = "II (8P) Рестайлинг 1";
    private static final String OTHER_GENERATION_ID = "6295465";
    private static final String BODY = "хэтчбек 5 дв. sportback";
    private static final String BODY_ID = "21837711";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).open();
        screenshotSteps.setWindowSizeForScreenshot();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Переход к выбору марки")
    @Category({Regression.class})
    public void shouldSelectMark() {
        basePageSteps.onStatsPage().filter().selectMarkModelButton().should(isDisplayed()).click();
        basePageSteps.onStatsPage().filter().selectMarkButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(STATS).path(CARS).path(MARKS).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Переход к выбору модели")
    @Category({Regression.class})
    public void shouldSelectModel() {
        basePageSteps.onStatsPage().filter().selectMarkModelButton().should(isDisplayed()).click();
        basePageSteps.onStatsPage().filter().selectModelButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODELS).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор другого поколения в фильтре")
    @Category({Regression.class})
    public void shouldSelectGeneration() {
        basePageSteps.onStatsPage().filter().selectMarkModelButton().should(isDisplayed()).click();
        //basePageSteps.selectOption(basePageSteps.onCatalogPage().filter().select(GENERATION), OTHER_GENERATION);
        basePageSteps.onCatalogPage().filter().select(GENERATION).should(isDisplayed()).click();
        basePageSteps.onCatalogPage().dropdown().item(OTHER_GENERATION).waitUntil(isDisplayed()).click();
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(OTHER_GENERATION_ID)
                .path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор кузова в фильтре")
    @Category({Regression.class})
    public void shouldSelectBody() {
        //basePageSteps.selectOption(basePageSteps.onCatalogPage().filter().select("Выбрать кузов"), BODY);
        basePageSteps.onCatalogPage().filter().select("Выбрать кузов").should(isDisplayed()).click();
        basePageSteps.onCatalogPage().dropdown().item(BODY).waitUntil(isDisplayed()).click();
        urlSteps.path(BODY_ID).path("/").shouldNotSeeDiff();
    }
}
