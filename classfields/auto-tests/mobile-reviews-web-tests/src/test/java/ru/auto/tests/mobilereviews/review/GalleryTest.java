package ru.auto.tests.mobilereviews.review;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.REVIEWS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(REVIEWS)
@DisplayName("Страница отзыва - галерея")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GalleryTest {

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
    public String category;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "/uaz/patriot/2309645/4014660/"},
                {MOTO, "/motorcycle/honda/cb_400/8131894731002395272/"},
                {TRUCKS, "/truck/foton/aumark_10xx/4033391/"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "reviews/ReviewsAutoCars",
                "reviews/ReviewsAutoMoto",
                "reviews/SearchMotoBreadcrumbsHondaCb400",
                "reviews/ReviewsAutoTrucks",
                "reviews/SearchTrucksBreadcrumbsFotonAumark10xx",
                "reviews/SearchCarsBreadcrumbsUazPatriot").post();

        urlSteps.testing().path(REVIEW).path(category).path(path).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Галерея - переход в полноэкранный режим по клику на фото")
    public void shouldOpenFullscreenGallery() {
        String firstImageSrc = basePageSteps.onReviewPage().gallery().currentImage().getAttribute("src");
        basePageSteps.onReviewPage().gallery().currentImage().should(isDisplayed()).click();
        basePageSteps.onReviewPage().fullScreenGallery().waitUntil(isDisplayed());
        basePageSteps.onReviewPage().fullScreenGallery().currentImage().should(hasAttribute("src", firstImageSrc));
    }
}
