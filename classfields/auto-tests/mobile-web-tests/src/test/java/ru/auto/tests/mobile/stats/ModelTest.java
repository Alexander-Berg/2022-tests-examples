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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MARKS;
import static ru.auto.tests.desktop.consts.Pages.MODELS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.STATS)
@DisplayName("Статистика по модели")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ModelTest {

    private static final String MARK = "audi";
    private static final String MODEL = "a3";
    private static final String GENERATION = "IV (8Y)";
    private static final String GENERATION_ID = "21837610";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(STATS).path(CARS).path(MARK).path(MODEL).open();
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
    @DisplayName("Выбор поколения в фильтре")
    @Category({Regression.class})
    public void shouldSelectGeneration() {
        //basePageSteps.selectOption(basePageSteps.onCatalogPage().filter().select("Выбрать поколение"), GENERATION);
        basePageSteps.onCatalogPage().filter().select("Выбрать поколение").should(isDisplayed()).click();
        basePageSteps.onCatalogPage().dropdown().item(GENERATION).waitUntil(isDisplayed()).click();
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path("/").shouldNotSeeDiff();
    }
}
