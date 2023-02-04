package ru.auto.tests.poffer.user;

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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.poffer.beta.BetaContactsBlock.CONFIRM_NUMBER;
import static ru.auto.tests.desktop.element.poffer.beta.BetaContactsBlock.PHONE_NUMBER;
import static ru.auto.tests.desktop.element.poffer.beta.BetaContactsBlock.SMS_CODE;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.unAuthUserDraftExample;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;

@DisplayName("Частник - ленивая авторизация")
@Feature(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class LazyAuthTest {

    private static final String DRAFT_TEMPLATE = "drafts/user_used_lazy_auth.json";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BetaPofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth"),
                stub("desktop/AuthLoginOrRegister"),
                stub("desktop/UserPhones"),
                stub("desktop/UserConfirm"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(unAuthUserDraftExample().getBody())
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();

        mockRule.overwriteStub(0, stub("desktop/SessionAuthUser"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ленивая авторизация")
    public void shouldAuth() {
        pofferSteps.onBetaPofferPage().contactsBlock().input(PHONE_NUMBER, "9111111111");
        pofferSteps.onBetaPofferPage().contactsBlock().button(CONFIRM_NUMBER).click();
        pofferSteps.onBetaPofferPage().contactsBlock().input(SMS_CODE, "1234");
        waitSomething(3, TimeUnit.SECONDS);

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveDraftFormsToPublicApi/",
                pofferHasJsonBody(DRAFT_TEMPLATE)
        ));
    }

}
