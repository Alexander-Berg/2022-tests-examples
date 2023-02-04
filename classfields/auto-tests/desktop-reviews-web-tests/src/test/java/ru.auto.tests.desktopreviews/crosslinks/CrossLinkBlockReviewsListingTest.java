package ru.auto.tests.desktopreviews.crosslinks;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг отзывов - блок перелинковки")
@Feature(AutoruFeatures.REVIEWS)
@Story("Блок перелинковки")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CrossLinkBlockReviewsListingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String mark;

    @Parameterized.Parameter(1)
    public String model;

    @Parameterized.Parameter(2)
    public String title;

    @Parameterized.Parameter(3)
    public String rating;

    @Parameterized.Parameter(4)
    public String catalogLinkName;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"toyota", "corolla", "Всё о Toyota Corolla", "Рейтинг модели — 4.6 / 5", "Каталог Toyota Corolla"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/SearchCarsBreadcrumbsToyotaCorolla",
                "desktop/ReviewsAutoCarsRatingToyotaCorolla").post();

        urlSteps.testing().path(REVIEWS).path(CARS).path(mark).path(model).open();
        basePageSteps.onReviewsListingPage().crossLinksBlock().waitUntil(isDisplayed());
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заголовок блока перелинковки")
    public void shouldSeeBlockTitle() {
        basePageSteps.onReviewsListingPage().crossLinksBlock().title().should(hasText(title));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Рейтинг марки / модели")
    public void shouldSeeRating() {
        basePageSteps.onReviewsListingPage().crossLinksBlock().rating().should(hasText(rating));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке в блоке")
    public void shouldClickLink() {
        basePageSteps.onReviewsListingPage().crossLinksBlock().getLink(0).should(hasText(catalogLinkName)).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(mark).path(model).path("/").shouldNotSeeDiff();
    }
}