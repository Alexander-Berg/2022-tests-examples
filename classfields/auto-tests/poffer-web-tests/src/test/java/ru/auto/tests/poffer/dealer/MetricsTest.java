package ru.auto.tests.poffer.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.PofferSteps;
import ru.auto.tests.passport.account.Account;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;

@DisplayName("Дилер - метрики")
@Feature(POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class MetricsTest {

    private Account account;

    @Rule
    @Inject
    public RuleChain defaultRules;

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
        urlSteps.testing().path(CARS).path(NEW).path(ADD).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики")
    public void shouldSeeMetrics() {
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"open_short\":{\"with_vin\":{}}}}}}");
        pofferSteps.onPofferPage().firstStepStsVinBlock().button("Пропустить").click();
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"vin_1_step\":{\"missed\":{}}}}}}");
        pofferSteps.fillMark("Toyota");
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"short_form\":{\"mark\":{}}}}}}");
        pofferSteps.fillModel("Corolla");
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"short_form\":{\"model\":{}}}}}}");
        pofferSteps.fillYear("2021");
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"short_form\":{\"year\":{}}}}}}");
        pofferSteps.fillBody("Седан");
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"short_form\":{\"body_type\":{}}}}}}");
        pofferSteps.fillGeneration("XI (E160, E170) Рестайлинг");
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"short_form\":{\"super_gen\":{}}}}}}");
        pofferSteps.fillEngine("Бензин");
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"short_form\":{\"engine_type\":{}}}}}}");
        pofferSteps.fillDrive("Передний");
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"short_form\":{\"drive\":{}}}}}}");
        pofferSteps.fillGearbox("Механическая");
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"short_form\":{\"gearbox\":{}}}}}}");
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"short_form\":{\"tech_param\":{\"autofill\":{}}}}}}}");
        pofferSteps.hideElements();
        pofferSteps.fillColor("FAFBFB");
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"long_form\":{\"required\":{\"color\":{\"success\":{\"Белый\":{}}}}}}}}}");
        pofferSteps.onPofferPage().priceBlock().checkbox("Возможен обмен").click();
        shouldSeePostData("{\"ADD_FORM_DEALER\":{\"cars\":{\"add\":{\"long_form\":{\"optional\":{\"exchange\":{\"on\":{}}}}}}}}");
    }

    @Step("Проверяем метрики")
    private void shouldSeePostData(String postData) {
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(postData)));
    }

    @After
    public void after() {
        pofferSteps.unlinkUserFromDealer(account.getId());
    }
}
