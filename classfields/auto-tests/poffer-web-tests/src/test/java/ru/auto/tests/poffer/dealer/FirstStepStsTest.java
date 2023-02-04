package ru.auto.tests.poffer.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.PofferSteps;
import ru.auto.tests.passport.account.Account;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Заполнение формы по СТС под незарегом")
@Feature(POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class FirstStepStsTest {

    private static final String OFFER_TEMPLATE = "offers/cars_used_dealer_draft.json";
    private static final String IMAGE_FILE = new File("src/main/resources/images/sts.jpg").getAbsolutePath();
    private static final String VIN = "Z94CT51DBFR118510";
    private Account account;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        account = pofferSteps.linkUserToDealer();
        loginSteps.loginAs(account);
        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заполнение формы по СТС")
    public void shouldFillFormBySts() {
        pofferSteps.onPofferPage().firstStepStsVinBlock().stsPhoto().sendKeys(IMAGE_FILE);
        pofferSteps.onPofferPage().firstStepStsVinBlock().input("VIN / № кузова")
                .waitUntil(hasAttribute("value", VIN));
        waitSomething(3, TimeUnit.SECONDS);
        pofferSteps.onPofferPage().firstStepStsVinBlock().button("Далее").waitUntil(isDisplayed()).click();

        pofferSteps.onPofferPage().breadcrumbs()
                .waitUntil(hasText("Hyundai/Solaris Очистить"));
        pofferSteps.fillBody("Седан");
        pofferSteps.fillGeneration("I");
        pofferSteps.fillEngine("Бензин");
        pofferSteps.fillDrive("Передний");
        pofferSteps.fillGearbox("Механическая");
        pofferSteps.fillModification("107\u00a0л.с.");

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveDraftPofferToPublicApi/",
                pofferHasJsonBody(OFFER_TEMPLATE)
        ));
    }

    @After
    public void after() {
        pofferSteps.unlinkUserFromDealer(account.getId());
    }
}
