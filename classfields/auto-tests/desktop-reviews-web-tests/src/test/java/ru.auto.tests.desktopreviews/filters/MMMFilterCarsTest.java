package ru.auto.tests.desktopreviews.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.RELEVANCE_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.FILTERS)
@DisplayName("Фильтр МММ в легковых")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MMMFilterCarsTest {

    private static final String MARK = "Audi";
    private static final String MODEL = "A3";
    private static final String GENERATION = "I (8L) Рестайлинг";
    private static final String GENERATION_CODE = "4927403";
    private static final String SECOND_GENERATION = "III (8V)";
    private static final String SECOND_GENERATION_CODE = "7979586";

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
    @DisplayName("Выбор марки")
    public void shouldSelectMark() {
        urlSteps.testing().path(REVIEWS).open();

        steps.onReviewPage().filters().mmmFilter().modelSelect().should(not(isEnabled()));
        steps.onReviewPage().filters().mmmFilter().generationSelect().should(not(isEnabled()));
        steps.onReviewPage().filters().mmmFilter().selectMark(MARK);
        steps.onReviewPage().filters().mmmFilter().modelSelect().waitUntil(isEnabled());
        steps.onReviewPage().filters().mmmFilter().generationSelect().should(not(isEnabled()));
        urlSteps.path(CARS).path(MARK.toLowerCase()).path("/").addParam("sort", RELEVANCE_DESC.getAlias())
                .shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс марки")
    public void shouldResetMark() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK.toLowerCase()).open();

        steps.onReviewPage().filters().mmmFilter().selectMark("Любая");
        steps.onReviewPage().filters().mmmFilter().modelSelect().waitUntil(not(isEnabled()));
        steps.onReviewPage().filters().mmmFilter().generationSelect().waitUntil(not(isEnabled()));
        urlSteps.testing().path(REVIEWS).path(CARS).path(ALL).addParam("sort", RELEVANCE_DESC.getAlias())
                .shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели")
    public void shouldSelectModel() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK.toLowerCase()).open();

        steps.onReviewPage().filters().mmmFilter().generationSelect().should(not(isEnabled()));
        steps.onReviewPage().filters().mmmFilter().selectModel(MODEL);
        steps.onReviewPage().filters().mmmFilter().generationSelect().waitUntil(isEnabled());
        urlSteps.path(MODEL.toLowerCase()).path("/").addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс модели")
    public void shouldResetModel() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .open();

        steps.onReviewPage().filters().mmmFilter().selectModel("Любая");
        steps.onReviewPage().filters().mmmFilter().generationSelect().waitUntil(not(isEnabled()));
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK.toLowerCase()).path("/")
                .addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор поколения")
    public void shouldSelectGeneration() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .open();

        steps.onReviewPage().filters().mmmFilter().selectGenerationInPopup(GENERATION);
        urlSteps.path(GENERATION_CODE).path("/").addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс поколения")
    public void shouldResetGeneration() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION_CODE).open();

        steps.onReviewPage().filters().mmmFilter().resetGeneration();
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path("/").addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор поколений")
    public void shouldMultiSelectGenerations() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .open();

        steps.onReviewPage().filters().mmmFilter().selectGenerationInPopup(GENERATION);
        steps.onReviewPage().filters().mmmFilter().generationSelect().click();
        steps.onReviewPage().filters().mmmFilter().selectGenerationInPopup(SECOND_GENERATION);
        urlSteps.testing().path(REVIEWS).path(CARS).path(ALL)
                .addParam("catalog_filter", format("mark=AUDI,model=A3,generation=%s", GENERATION_CODE))
                .addParam("catalog_filter", format("mark=AUDI,model=A3,generation=%s", SECOND_GENERATION_CODE))
                .addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки в списке отзывов по всем маркам")
    public void shouldSelectMarkInAllMarksList() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(ALL).open();

        steps.onReviewPage().filters().mmmFilter().modelSelect().should(not(isEnabled()));
        steps.onReviewPage().filters().mmmFilter().generationSelect().should(not(isEnabled()));
        steps.onReviewPage().filters().mmmFilter().selectMark(MARK);
        steps.onReviewPage().filters().mmmFilter().modelSelect().waitUntil(isEnabled());
        steps.onReviewPage().filters().mmmFilter().generationSelect().should(not(isEnabled()));
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK.toLowerCase()).path("/")
                .addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }
}