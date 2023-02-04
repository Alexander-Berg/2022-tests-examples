package ru.auto.tests.mobile.vin;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(VIN)
@DisplayName("Про авто - страница истории автомобиля")
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryNotFoundTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/CarfaxReportRawVinNotFound",
                "desktop/CarfaxReportRawLicensePlateNotFound").post();

        urlSteps.testing().path(HISTORY).open();
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Открытие истории по VIN. Нет такого автомобиля")
    public void shouldOpenHistoryByVINNoSuchCar() {
        steps.onHistoryPage().input("Госномер или VIN", "WP0ZZZ97ZEL221611");
        steps.onHistoryPage().findButton().click();
        steps.onHistoryPage().error().waitUntil(hasText("Нет такого автомобиля\nНе нашли историю по VIN. " +
                "Убедитесь, что он написан без ошибок. Если опечаток нет – попробуйте найти историю по госномеру."));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Открытие истории по госномеру. Нет такого автомобиля")
    public void shouldOpenHistoryByNumberNoSuchCar() {
        steps.onHistoryPage().input("Госномер или VIN", "K000OC777");
        steps.onHistoryPage().findButton().click();
        steps.onHistoryPage().error().waitUntil(hasText("Нет такого автомобиля\nНе нашли историю по госномеру. " +
                "Убедитесь, что он написан без ошибок. Если опечаток нет – попробуйте найти историю по VIN."));
    }
}
