package ru.auto.tests.desktopreviews.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.REVIEWS_LISTING_SIZE;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.COMMENTS;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.DATE;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.LIKE;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.RATING_ASC;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.RATING_DESC;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.RELEVANCE_DESC;

@DisplayName("Листинг отзывов - сортировки")
@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.SORT)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingSortTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public ReviewsListingPage.SortBy sortBy;

    @Parameterized.Parameters(name = "{0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, LIKE},
                {CARS, DATE},
                {CARS, COMMENTS},
                {CARS, RATING_DESC},
                {CARS, RATING_ASC},

                {MOTO, LIKE},
                {MOTO, COMMENTS},
                {MOTO, RATING_DESC},
                {MOTO, RATING_ASC},

                {TRUCKS, LIKE},
                {TRUCKS, COMMENTS},
                {TRUCKS, RATING_DESC},
                {TRUCKS, RATING_ASC}
        });
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сортировки")
    public void shouldSort() {
        urlSteps.testing().path(REVIEWS).path(category).path(ALL).open();

        basePageSteps.onReviewsListingPage().selectItem(RELEVANCE_DESC.getName(), sortBy.getName());
        urlSteps.addParam("sort", sortBy.getAlias()).addParam("page", "1").shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE));
    }
}