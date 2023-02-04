package ru.auto.tests.desktopreviews.filters;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.RELEVANCE_DESC;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.REVIEW)
@DisplayName("Фильтр МММ в легковых на странице отзыва")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MMMFilterCarsReviewTest {

    private static final String MARK_CODE = "uaz";
    private static final String MODEL = "Patriot";
    private static final String OTHER_MODEL = "Hunter";
    private static final String GENERATION = "I";
    private static final String OTHER_GENERATION = "I Рестайлинг";
    private static final String OTHER_GENERATION_CODE = "10417370";
    private static final String OTHER_MARK = "Audi";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "reviews/ReviewsAutoCars",
                "reviews/SearchCarsBreadcrumbsUazPatriot",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        urlSteps.testing().path(REVIEW).path(CARS).path("/uaz/patriot/2309645/4014660/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор другой марки")
    @Category({Regression.class})
    public void shouldSelectOtherMark() {
        basePageSteps.onReviewPage().filters().mmmFilter().selectMark(OTHER_MARK);
        urlSteps.testing().path(REVIEWS).path(CARS).path(OTHER_MARK.toLowerCase()).path("/")
                .addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор другой модели")
    @Category({Regression.class})
    public void shouldSelectOtherModel() {
        basePageSteps.onReviewPage().filters().mmmFilter().selectModel(OTHER_MODEL);
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK_CODE.toLowerCase())
                .path(OTHER_MODEL.toLowerCase()).path("/").addParam("sort", RELEVANCE_DESC.getAlias())
                .shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор другого поколения")
    @Category({Regression.class})
    public void shouldSelectOtherGeneration() {
        basePageSteps.onReviewPage().filters().mmmFilter().selectGenerationInPopup(GENERATION);
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK_CODE).path(MODEL.toLowerCase())
                .path("/").addParam("sort", RELEVANCE_DESC.getAlias())
                .shouldNotSeeDiff();
        basePageSteps.onReviewPage().filters().mmmFilter().selectGenerationInPopup(OTHER_GENERATION);
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK_CODE).path(MODEL.toLowerCase())
                .path(OTHER_GENERATION_CODE).path("/").addParam("sort", RELEVANCE_DESC.getAlias())
                .shouldNotSeeDiff();
    }
}