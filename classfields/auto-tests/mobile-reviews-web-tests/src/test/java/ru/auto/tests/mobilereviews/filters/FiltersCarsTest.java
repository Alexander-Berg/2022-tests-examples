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

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MARKS;
import static ru.auto.tests.desktop.consts.Pages.MODELS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;

@Feature(FILTERS)
@DisplayName("Фильтры в легковых")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FiltersCarsTest {

    private static final String MARK = "audi";
    private static final String MODEL = "a3";
    private static final String GENERATION = "I (8L)";
    private static final String GENERATION_CODE = "3473199";
    private static final String SECOND_GENERATION = "II (8P) Рестайлинг 2";
    private static final String SECOND_GENERATION_CODE = "2305282";

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
        urlSteps.testing().path(REVIEWS).path(CARS).path(ALL).open();

        steps.onReviewsListingPage().filters().selectMarkButton().click();
        urlSteps.testing().path(REVIEWS).path(MARKS).path(CARS).shouldNotSeeDiff();
        steps.onMarksAndModelsPage().marksOrModelsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Выбрать модель»")
    public void shouldClickSelectModelButton() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).open();

        steps.onReviewsListingPage().filters().selectModelButton().click();
        urlSteps.testing().path(REVIEWS).path(MODELS).path(CARS)
                .addParam("mark", MARK).shouldNotSeeDiff();
        steps.onMarksAndModelsPage().marksOrModelsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор поколения")
    public void shouldSelectGeneration() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).open();

        steps.onReviewsListingPage().filters().generationSelect().selectGeneration(GENERATION);
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(GENERATION_CODE).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор поколений")
    public void shouldMultiSelectGenerations() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL)
                .addParam("super_gen", GENERATION_CODE).open();

        steps.onReviewsListingPage().filters().filtersMinimized().click();
        steps.onReviewsListingPage().filters().generationSelect().selectGeneration(SECOND_GENERATION);
        urlSteps.testing().path(REVIEWS).path(CARS).path(ALL).path("/")
                .addParam("catalog_filter",
                        format("mark=%s,model=%s,generation=%s", MARK.toUpperCase(), MODEL.toUpperCase(), GENERATION_CODE))
                .addParam("catalog_filter",
                        format("mark=%s,model=%s,generation=%s", MARK.toUpperCase(), MODEL.toUpperCase(), SECOND_GENERATION_CODE))
                .shouldNotSeeDiff();
    }
}
