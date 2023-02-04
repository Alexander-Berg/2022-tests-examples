package ru.auto.tests.desktopreviews.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.REVIEWS_LISTING_SIZE;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.LIKE;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.RELEVANCE_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.FILTERS)
@DisplayName("Мультивыбор в отзывах")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MultiSelectCarsTest {

    private static final String FIRST_MARK_MODEL_GEN = "mark=AUDI,model=A3,generation=20785010";
    private static final String SECOND_MARK_MODEL_GEN = "mark=AUDI,model=A3,generation=7979586";
    private static final String PARAM_NAME = "catalog_filter";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps steps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(ALL)
                .addParam(PARAM_NAME, FIRST_MARK_MODEL_GEN)
                .addParam(PARAM_NAME, SECOND_MARK_MODEL_GEN).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Сортировка")
    public void shouldSort() {
        steps.onReviewsListingPage().selectItem(RELEVANCE_DESC.getName(), LIKE.getName());
        urlSteps.addParam("page", "1").addParam("sort", LIKE.getAlias()).shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE));
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class})
    @DisplayName("Перейти на 2 страницу")
    public void shouldSelectSecondPage() {
        steps.onReviewsListingPage().pager().page("2").click();
        urlSteps.addParam("sort", RELEVANCE_DESC.getAlias()).addParam("page", "2").shouldNotSeeDiff();
        steps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE));
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class})
    @DisplayName("Отсутствие перехода на видео")
    public void shouldNotSeeVideo() {
        steps.onReviewsListingPage().subHeader().button("Видео").should(not(isDisplayed()));
    }

}