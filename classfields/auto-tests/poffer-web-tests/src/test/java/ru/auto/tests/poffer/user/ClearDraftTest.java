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
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.poffer.beta.LeftNavigationMenu.CLEAR_FORM;
import static ru.auto.tests.desktop.matchers.AlertMatcher.hasAlertWithText;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.emptyUserDraftExample;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Частник - очистка черновика")
@Feature(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ClearDraftTest {

    private static final String DRAFT_ID = "4848705651719180864-7ac6416a";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private WebDriverManager webDriverManager;

    @Inject
    private BetaPofferSteps pofferSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(userDraftExample().getBody()),

                stub().withDeleteDeepEquals(format("/1.0/user/draft/CARS/%s", DRAFT_ID))
                        .withStatusSuccessResponse()
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
        pofferSteps.setWideWindowSize();

        pofferSteps.hideElement(pofferSteps.onBetaPofferPage().discountTimer());
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны увидеть алерт с текстом")
    public void shouldSeeAlertWithWarningText() {
        pofferSteps.onBetaPofferPage().leftNavigationMenu().button(CLEAR_FORM).click();

        assertThat(webDriverManager.getDriver(),
                hasAlertWithText("Все заполненные данные исчезнут. Вы уверены, что хотите сбросить всё?"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Очистка черновика")
    public void shouldClearDraft() {
        mockRule.overwriteStub(1,
                stub().withGetDeepEquals(USER_DRAFT_CARS).withResponseBody(emptyUserDraftExample().getBody())
        );

        pofferSteps.onBetaPofferPage().leftNavigationMenu().button(CLEAR_FORM).click();
        pofferSteps.acceptAlert();

        pofferSteps.onBetaPofferPage().wizardMarkSection().should(hasText("Продайте свой автомобиль\n" +
                "Объявление смогут увидеть 3 000 000 человек ежедневно\nМарка"));
    }
}
