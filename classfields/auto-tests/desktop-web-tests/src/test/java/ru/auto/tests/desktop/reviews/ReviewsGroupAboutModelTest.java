package ru.auto.tests.desktop.reviews;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@DisplayName("Отзывы на группе новых - о модели")
@Feature(AutoruFeatures.VIDEO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ReviewsGroupAboutModelTest {

    private static final String MARK = "kia";
    private static final String MODEL = "optima";
    private static final String GENERATION = "21342050";
    private static final String PATH = "/21342050-21342121/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsGroupContextGroup",
                "desktop/SearchCarsGroupContextListing",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechInfo",
                "desktop/ReferenceCatalogCarsTechParam",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "desktop/ReviewsAutoCarsCounterKiaOptima21342050",
                "desktop/ReviewsAutoListingCarsKiaOptima21342050",
                "desktop/ReviewsAutoCarsRatingKiaOptima21342050").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL).path(PATH).open();
        basePageSteps.onGroupPage().tab("О модели").click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все отзывы»")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onGroupPage().reviews().button("Смотреть все отзывы").should(hasAttribute("href",
                urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(GENERATION).path("/")
                        .addParam("from", "card").toString())).click();
        urlSteps.shouldUrl(startsWith(urlSteps.testing().path(REVIEWS).toString()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по отзыву")
    public void shouldClickReview() {
        basePageSteps.onGroupPage().reviews().getReview(0).click();
        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GENERATION).path("/8244718709293817439/")
                .addParam("from", "card").shouldNotSeeDiff();
    }
}