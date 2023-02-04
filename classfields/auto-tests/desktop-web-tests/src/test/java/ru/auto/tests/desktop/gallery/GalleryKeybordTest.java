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
import org.openqa.selenium.Keys;
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
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES)
@DisplayName("Объявление - галерея, листание фото с помощью клавиатуры")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GalleryKeybordTest {

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
                {TRUCK, "desktop/OfferTrucksUsedUser"}
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
    @DisplayName("Галерея - листание фото с помощью клавиатуры")
    public void shouldSwitchPhotosByKeyboard() {
        basePageSteps.onCardPage().gallery().currentImage().should(isDisplayed()).click();
        String firstImageSrc = basePageSteps.onCardPage().fullScreenGallery().currentImage().getAttribute("src");

        basePageSteps.onCardPage().body().sendKeys(Keys.ARROW_RIGHT);
        basePageSteps.onCardPage().fullScreenGallery().currentImage()
                .waitUntil(not(hasAttribute("src", firstImageSrc)));
        basePageSteps.onCardPage().body().sendKeys(Keys.ARROW_LEFT);
        basePageSteps.onCardPage().fullScreenGallery().currentImage()
                .waitUntil(hasAttribute("src", firstImageSrc));

        basePageSteps.onCardPage().body().sendKeys(Keys.ARROW_DOWN);
        basePageSteps.onCardPage().fullScreenGallery().currentImage()
                .waitUntil(not(hasAttribute("src", firstImageSrc)));
        basePageSteps.onCardPage().body().sendKeys(Keys.ARROW_UP);
        basePageSteps.onCardPage().fullScreenGallery().currentImage()
                .waitUntil(hasAttribute("src", firstImageSrc));
    }
}
