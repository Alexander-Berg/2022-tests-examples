package ru.auto.tests.mobilereviews.filters;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MARKS;
import static ru.auto.tests.desktop.consts.Pages.MODELS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;

@Feature(FILTERS)
@DisplayName("Фильтры в комтрансе")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FiltersTrucksTest {

    private static final String CATEGORY = "truck";
    private static final String MARK = "baw";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps steps;

    @Inject
    public UrlSteps urlSteps;

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Выбрать марку»")
    public void shouldClickSelectMarkButton() {
        urlSteps.testing().path(REVIEWS).path(TRUCKS).path(CATEGORY).open();

        steps.onReviewPage().filters().selectMarkButton().click();
        urlSteps.testing().path(REVIEWS).path(MARKS).path(TRUCKS).path(CATEGORY).path("/")
                .shouldNotSeeDiff();
        steps.onMarksAndModelsPage().marksOrModelsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Выбрать модель»")
    public void shouldClickSelectModelButton() {
        urlSteps.testing().path(REVIEWS).path(TRUCKS).path(CATEGORY).path(MARK).open();

        steps.onReviewPage().filters().selectModelButton().click();
        urlSteps.testing().path(REVIEWS).path(MODELS).path(TRUCKS).path(CATEGORY).path("/")
                .addParam("mark", MARK).shouldNotSeeDiff();
        steps.onMarksAndModelsPage().marksOrModelsList().waitUntil(hasSize(greaterThan(0)));
    }
}
