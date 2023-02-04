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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.RELEVANCE_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.FILTERS)
@DisplayName("Фильтр МММ в комтрансе")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MMMFilterCommercialTest {

    private static final String CATEGORY = "Грузовик";
    private static final String MARK = "BAW";
    private static final String MODEL = "Fenix";

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
    @DisplayName("Выбор категории")
    public void shouldSelectCategory() {
        urlSteps.testing().path(REVIEWS).path(TRUCKS).open();

        steps.onReviewPage().filters().mmmFilter().markSelect().should(not(isEnabled()));
        steps.onReviewPage().filters().mmmFilter().modelSelect().should(not(isEnabled()));
        steps.onReviewPage().filters().mmmFilter().selectCategory(CATEGORY);
        steps.onReviewPage().filters().mmmFilter().markSelect().waitUntil(isEnabled());
        steps.onReviewPage().filters().mmmFilter().modelSelect().should(not(isEnabled()));
        urlSteps.path(TRUCK).path("/").addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки")
    public void shouldSelectMark() {
        urlSteps.testing().path(REVIEWS).path(TRUCKS).path(TRUCK).path("/").open();
        steps.onReviewPage().filters().mmmFilter().modelSelect().should(not(isEnabled()));
        steps.onReviewPage().filters().mmmFilter().selectMark(MARK);
        steps.onReviewPage().filters().mmmFilter().modelSelect().waitUntil(isEnabled());
        urlSteps.path(MARK.toLowerCase()).path("/").addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели")
    public void shouldSelectModel() {
        urlSteps.testing().path(REVIEWS).path(TRUCKS).path(TRUCK).path(MARK.toLowerCase())
                .path("/").open();
        steps.onReviewPage().filters().mmmFilter().selectModel(MODEL);
        urlSteps.path(MODEL.toLowerCase()).path("/").addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }
}