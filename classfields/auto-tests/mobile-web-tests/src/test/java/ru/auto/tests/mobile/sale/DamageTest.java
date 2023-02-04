package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - развёртка с повреждениями")
@Feature("Карточка объявления")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DamageTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @DisplayName("Отображение повреждений")
    public void shouldSeeDamages() {
        basePageSteps.setWindowMaxHeight();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCardPage().damages());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot prodScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCardPage().damages());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, prodScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение выбранного повреждения")
    public void shouldSeeSelectedDamage() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().damages().damage("1"));
        basePageSteps.onCardPage().damages().selectedDamage().waitUntil(isDisplayed())
                .should(hasText("1\nПередний бампер\nЦарапина/Скол"));
        basePageSteps.onCardPage().damages().selectedDamage().button("Царапина/Скол").click();
        basePageSteps.onCardPage().popup().waitUntil(hasText("сколото крепление спойлера, расколота рамка " +
                "радиатора"));

        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().damages().damage("5"));
        basePageSteps.onCardPage().damages().selectedDamage().waitUntil(isDisplayed())
                .should(hasText("5\nЗаднее правое крыло\nЦарапина/Скол"));
        basePageSteps.onCardPage().damages().selectedDamage().button("Царапина/Скол").click();
        basePageSteps.onCardPage().popup().waitUntil(hasText("царапина вдоль всего борта"));
    }
}
