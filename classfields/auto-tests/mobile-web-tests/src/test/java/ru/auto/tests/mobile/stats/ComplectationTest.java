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
@DisplayName("Статистика по комплектации")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ComplectationTest {

    private static final String MARK = "audi";
    private static final String MODEL = "a3";
    private static final String GENERATION = "IV (8Y)";
    private static final String GENERATION_ID = "21837610";
    private static final String OTHER_GENERATION = "II (8P) Рестайлинг 1";
    private static final String OTHER_GENERATION_ID = "6295465";
    private static final String BODY = "хэтчбек 5 дв. sportback";
    private static final String BODY_ID = "21837711";
    private static final String OTHER_BODY = "седан";
    private static final String OTHER_BODY_ID = "21995218";
    private static final String COMPLECTATION = "1.4 AT 150 л.c. – Sport 35 TFSI tiptronic";
    private static final String COMPLECTATION_ID = "21837711_22870723_22848425";
    private static final String OTHER_COMPLECTATION = "1.0 AMT 110 л.c. ";
    private static final String OTHER_COMPLECTATION_ID = "21837711__22684092";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID)
                .path(COMPLECTATION_ID).open();
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
    @DisplayName("Выбор другого кузова в фильтре")
    @Category({Regression.class})
    public void shouldSelectBody() {
        basePageSteps.onStatsPage().filter().selectMarkModelButton().should(isDisplayed()).click();
        //basePageSteps.selectOption(basePageSteps.onCatalogPage().filter().select(BODY), OTHER_BODY);
        basePageSteps.onCatalogPage().filter().select(BODY).should(isDisplayed()).click();
        basePageSteps.onCatalogPage().dropdown().item(OTHER_BODY).waitUntil(isDisplayed()).click();
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID)
                .path(OTHER_BODY_ID).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор другой комплектации в фильтре")
    @Category({Regression.class})
    public void shouldSelectComplectation() {
        //basePageSteps.selectOption(basePageSteps.onCatalogPage().filter().select(COMPLECTATION), OTHER_COMPLECTATION);
        basePageSteps.onCatalogPage().filter().select(COMPLECTATION).should(isDisplayed()).click();
        basePageSteps.onCatalogPage().dropdown().item(OTHER_COMPLECTATION).waitUntil(isDisplayed()).click();
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID)
                .path(BODY_ID).path(OTHER_COMPLECTATION_ID).path("/").shouldNotSeeDiff();
    }
}
