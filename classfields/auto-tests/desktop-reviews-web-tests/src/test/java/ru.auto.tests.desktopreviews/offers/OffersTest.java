package ru.auto.tests.desktopreviews.offers;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.SALES_WIGET)
@DisplayName("«Предложения о продаже»")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OffersTest {

    private static final int VISIBLE_ITEMS_CNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String path;

    @Parameterized.Parameter(1)
    public String listingPath;

    @Parameterized.Parameters(name = "{0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/reviews/cars/all/", "%s/moskva/cars/all/"},
                {"/reviews/moto/all/", "%s/moskva/motorcycle/all/?customs_state_group=DOESNT_MATTER"},
                {"/reviews/trucks/all/", "%s/moskva/lcv/all/?customs_state_group=DOESNT_MATTER"},
                {"/reviews/cars/hyundai/solaris/", "%s/moskva/cars/hyundai/solaris/all/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(path).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onReviewsListingPage().footer(), 0, -500);
        basePageSteps.onReviewsListingPage().offers().waitUntil(isDisplayed()).hover();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по предложению")
    public void shouldClickOffer() {
        String saleTitle = basePageSteps.onReviewsListingPage().offers().getItem(0).title().getText();
        basePageSteps.onReviewsListingPage().offers().getItem(0).click();
        basePageSteps.switchToNextTab();
        basePageSteps.onCardPage().cardHeader().title().waitUntil(hasText(anyOf(startsWith(saleTitle))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Смотреть все»")
    @Category({Regression.class})
    public void shouldClickShowAllButton() {
        basePageSteps.onReviewsListingPage().offers().allUrl().click();
        basePageSteps.switchToNextTab();
        urlSteps.fromUri(format(listingPath, urlSteps.getConfig().getTestingURI())).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Листание предложений")
    public void shouldSlideOffers() {
        basePageSteps.onReviewsListingPage().offers().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onReviewsListingPage().offers().prevButton().should(not(isDisplayed()));
        basePageSteps.onReviewsListingPage().offers().nextButton().click();
        basePageSteps.onReviewsListingPage().offers().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onReviewsListingPage().offers().prevButton().waitUntil(isDisplayed()).click();
        basePageSteps.onReviewsListingPage().offers().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> isDisplayed());
        basePageSteps.onReviewsListingPage().offers().nextButton().waitUntil(isDisplayed());
        basePageSteps.onReviewsListingPage().offers().prevButton().waitUntil(not(isDisplayed()));
    }
}