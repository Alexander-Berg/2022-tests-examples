package ru.auto.tests.desktop.metrics;

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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Метрики - цели - телефон на групповой карточке c запиненным оффером")
@Feature(METRICS)
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GroupSalePhoneGoalsTest {

    private static final String PATH = "/kia/optima/21342125/21342344/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String goal;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {"PHONE_CARS2_ALL"},
                {"PHONE_CL_CARS2"},
                {"PHONE_NEW_CL_CARS2"},
                {"CONTACT_CARS"},
                {"CONTACT_CARS_DESKTOP"},
                {"CONTACT_CARS_DEALER_NEW"},
                {"CONTACT_CARS_PHONE"},
                {"CONTACT_CARS_PHONE_NEW"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsNewDealer",
                "desktop/OfferCarsPhones").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики при клике на «Показать телефон»")
    public void shouldSendMetrics() {
        basePageSteps.onCardPage().contacts().showPhoneButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цели при клике на «Показать телефон» в плавающей панели")
    public void shouldSendStickyBarGoals() {
        basePageSteps.scrollDown(1000);
        basePageSteps.onCardPage().stickyBar().waitUntil(isDisplayed());
        basePageSteps.onCardPage().stickyBar().showPhoneButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цели при клике на «Показать телефон» в галерее")
    public void shouldSendGalleryGoals() {
        basePageSteps.setWideWindowSize(HEIGHT_1024);
        basePageSteps.onCardPage().gallery().currentImage().click();
        basePageSteps.onCardPage().fullScreenGallery().contacts().showPhoneButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }
}
