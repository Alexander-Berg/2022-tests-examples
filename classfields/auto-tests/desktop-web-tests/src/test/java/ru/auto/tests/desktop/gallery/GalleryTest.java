package ru.auto.tests.desktop.gallery;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES)
@DisplayName("Объявление - галерея")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GalleryTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String mock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser"},
                {TRUCK, "desktop/OfferTrucksUsedUser"},
                {MOTORCYCLE, "desktop/OfferMotoUsedUser"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(stub(mock)).create();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Галерея - переключение фото по ховеру")
    public void shouldSwitchPhotosByHover() {
        String firstImageSrc = basePageSteps.onCardPage().gallery().currentImage().getAttribute("src");
        basePageSteps.onCardPage().gallery().getThumb(1).should(isDisplayed()).hover();
        basePageSteps.onCardPage().gallery().currentImage().waitUntil(not(hasAttribute("src", firstImageSrc)));
        basePageSteps.onCardPage().gallery().getThumb(0).should(isDisplayed()).hover();
        basePageSteps.onCardPage().gallery().currentImage().waitUntil(hasAttribute("src", firstImageSrc));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Галерея - листание фото с помощью стрелок")
    public void shouldSwitchPhotosByArrows() {
        String firstImageSrc = basePageSteps.onCardPage().gallery().currentImage().getAttribute("src");
        basePageSteps.onCardPage().gallery().currentImage().hover();
        basePageSteps.onCardPage().gallery().nextButton().waitUntil(isDisplayed()).hover().click();
        basePageSteps.onCardPage().gallery().currentImage().waitUntil(not(hasAttribute("src", firstImageSrc)));
        basePageSteps.onCardPage().gallery().prevButton().waitUntil(isDisplayed()).hover().click();
        basePageSteps.onCardPage().gallery().currentImage().waitUntil(hasAttribute("src", firstImageSrc));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Галерея - переход в полноэкранный режим по клику на фото")
    public void shouldOpenAndCloseFullscreenMode() {
        String firstImageSrc = basePageSteps.onCardPage().gallery().currentImage().getAttribute("src");
        basePageSteps.onCardPage().gallery().currentImage().should(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().waitUntil(isDisplayed());
        basePageSteps.onCardPage().fullScreenGallery().currentImage().
                should(hasAttribute("src", firstImageSrc));
        basePageSteps.onCardPage().fullScreenGallery().closeButton().should(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().waitUntil(not(isDisplayed()));
    }
}
