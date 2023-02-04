package ru.auto.tests.desktopreviews.pager;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PAGER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.REVIEWS_LISTING_SIZE;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.RELEVANCE_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Листинг отзывов - пагинация")
@Feature(PAGER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReviewsListingPagerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS},
                {MOTO},
                {TRUCKS}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).path(category).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на следующую страницу по кнопке «Следующая»")
    public void shouldClickNextButton() {
        basePageSteps.onReviewsListingPage().pager().next().click();
        urlSteps.addParam("page", "2").addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на предыдущую страницу по кнопке «Предыдущая»")
    public void shouldClickPrevButton() {
        urlSteps.addParam("page", "2").open();
        basePageSteps.onReviewsListingPage().pager().prev().click();
        urlSteps.replaceParam("page", "1").addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на страницу")
    public void shouldClickPage() {
        basePageSteps.onReviewsListingPage().pager().page("2").click();
        urlSteps.addParam("page", "2").addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «...»")
    public void shouldClickSkipButton() {
        basePageSteps.onReviewsListingPage().pager().threeDotsFirst().click();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE));
        basePageSteps.onReviewsListingPage().pager().currentPage().waitUntil(hasText("10"));
        urlSteps.addParam("page", "10").addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().pager().threeDotsLast().click();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE));
        basePageSteps.onReviewsListingPage().pager().currentPage().waitUntil(hasText("14"));
        urlSteps.replaceParam("page", "14").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Показать ещё»")
    public void shouldClickMoreButton() {
        basePageSteps.onReviewsListingPage().pager().button("Показать ещё").click();
        urlSteps.addParam("page", "2").addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE * 2));
    }
}