package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES)
@DisplayName("Объявление - галерея")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GalleryTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameter(2)
    public String photoCounter;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser", "5"},
                {TRUCK, "desktop/OfferTrucksUsedUser", "5"},
                {MOTORCYCLE, "desktop/OfferMotoUsedUser", "5"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(saleMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @DisplayName("Открытие полноэкранной галереи")
    public void shouldOpenFullscreenGallery() {
        basePageSteps.setWindowMaxHeight();

        basePageSteps.onCardPage().gallery().getItem(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().counter().waitUntil(hasText(format("1 / %s", photoCounter)));
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCardPage().fullScreenGallery());

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.onCardPage().gallery().getItem(0).waitUntil(isDisplayed()).click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCardPage().fullScreenGallery());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Открытие правильного изображения")
    public void shouldSeeCorrectImage() {
        String image = basePageSteps.onCardPage().gallery().getItem(1).image().getAttribute("src");
        basePageSteps.onCardPage().gallery().getItem(1).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().counter().waitUntil(hasText(format("2 / %s", photoCounter)));
        basePageSteps.onCardPage().fullScreenGallery().getItem(1).image()
                .waitUntil(hasAttribute("src", image.replace("456x342", "1200x900n")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрытие полноэкранной галереи")
    public void shouldCloseFullscreenGallery() {
        basePageSteps.onCardPage().gallery().getItem(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().closeIcon().waitUntil(isDisplayed()).hover().click();
        basePageSteps.onCardPage().fullScreenGallery().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Позвонить» в полноэкранной галерее")
    public void shouldClickCallButton() {
        mockRule.with("desktop/OfferCarsPhones",
                "desktop/OfferMotoPhones",
                "desktop/OfferTrucksPhones").update();

        basePageSteps.onCardPage().gallery().getItem(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Телефон\n+7 916 039-84-27\nс 10:00 до 23:00\n+7 916 039-84-28\nс 12:00 до 20:00"));
    }
}
