package ru.auto.tests.desktop.video;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.VIDEO)
@DisplayName("Видео на карточке объявления")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SaleTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final int VIDEOS_COUNT = 2;

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameter(2)
    public String videoMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser", "desktop/VideoSearchCarsLandRover"},
                {TRUCK, "desktop/OfferTrucksUsedUser", "desktop/VideoSearchTrucksZil"},
                {MOTORCYCLE, "desktop/OfferMotoUsedUser", "desktop/VideoSearchMotoHarleyDavidson"},
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(saleMock,
                videoMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение блока")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVideoBlock() {
        basePageSteps.onCardPage().videos().videosList().should(hasSize(VIDEOS_COUNT))
                .forEach(video -> video.should(isDisplayed()));
        basePageSteps.onCardPage().videos().title().should(hasText("Видео о модели"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по видео")
    @Category({Regression.class, Testing.class})
    public void shouldClickVideo() {
        basePageSteps.onCardPage().videos().getVideo(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().activePopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Закрытие видео")
    @Category({Regression.class, Testing.class})
    public void shouldCloseVideo() {
        basePageSteps.onCardPage().videos().getVideo(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().activePopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().activePopupCloser().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().activePopup().waitUntil(not(isDisplayed()));
    }
}