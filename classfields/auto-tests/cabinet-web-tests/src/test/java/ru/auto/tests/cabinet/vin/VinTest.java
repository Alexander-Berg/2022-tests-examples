package ru.auto.tests.cabinet.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import pazone.ashot.Screenshot;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.AVGRIBANOV;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.VIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

/**
 * @author Artem Gribanov (avgeibanov)
 * @date 14.11.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. История автомобиля")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/ApiServiceAutoruPrice").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(VIN).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Пустое поле VIN. Предупреждение")
    public void shouldNotAllowEmptyVIN() {
        steps.onCabinetHistoryPage().vin().vinButton().click();
        steps.onCabinetHistoryPage().vin().errorMessage().waitUntil(hasText("Введите правильный VIN/госномер"));
    }

    @Test
    @Category({Regression.class})
    @Owner(AVGRIBANOV)
    @DisplayName("VIN больше 17 символов. Предупреждение")
    public void shouldNotAllowLongVINs() {
        steps.onCabinetHistoryPage().vin().input("KMHE341FBJA3920201");
        steps.onCabinetHistoryPage().vin().vinButton().click();
        steps.onCabinetHistoryPage().vin().errorMessage().waitUntil(hasText("Введите правильный VIN/госномер"));
    }

    @Test
    @Category({Regression.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Вводим VIN меньше 17 символов. Предупреждение")
    public void shouldNotAllowShortVIN() {
        steps.onCabinetHistoryPage().vin().input("Госномер или VIN", "KMHE341FBJA39202");
        steps.onCabinetHistoryPage().vin().vinButton().click();
        steps.onCabinetHistoryPage().vin().errorMessage().waitUntil(hasText("Введите правильный VIN/госномер"));
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Отображение страницы истории автомобиля")
    public void shouldSeeVinHistory() {
        screenshotSteps.setWindowSizeForScreenshot();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetHistoryPage().vin());

        urlSteps.setProduction().open();
        screenshotSteps.setWindowSizeForScreenshot();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetHistoryPage().vin());
        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Открытие истории по VIN.")
    public void shouldSeeVINHistory() {
        addVinOrNumber("WP0ZZZ97ZEL001611");
        urlSteps.testing().path(HISTORY).path("/WP0ZZZ97ZEL001611/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Открытие истории по госномеру")
    public void shouldSeeNumberHistory() {
        addVinOrNumber("А565МР178");
        urlSteps.testing().path(HISTORY).path("/A565MP178/").shouldNotSeeDiff();
    }

    @Step("Ввод VIN или госномера и переход на страницу истории")
    private void addVinOrNumber(String num) {
        steps.onCabinetHistoryPage().vin().input("Госномер или VIN", num);
        steps.onCabinetHistoryPage().vin().vinButton().click();
        waitSomething(1, TimeUnit.SECONDS);
        steps.switchToNextTab();
    }
}
