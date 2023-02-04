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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник - шапка")
@Feature(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HeaderTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BetaPofferSteps pofferSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(userDraftExample().getBody())
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны видеть шапку")
    public void shouldSeeHeader() {
        pofferSteps.onBetaPofferPage().header().should(hasText("Авто.ру\nЗакрыть"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик в лого")
    public void shouldClickLogo() {
        pofferSteps.onBetaPofferPage().header().logo().waitUntil(isDisplayed())
                .waitUntil(hasAttribute("href", "https://test.avto.ru/moskva/"))
                .click();

        urlSteps.testing().path(MOSKVA).shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик в кнопку закрытия")
    public void shouldClickCloseButton() {
        pofferSteps.onBetaPofferPage().header().button("Закрыть").waitUntil(isDisplayed())
                .waitUntil(hasAttribute("href", urlSteps.testing().path(MOSKVA).toString()))
                .click();

        urlSteps.testing().path(MOSKVA).shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик в кнопку закрытия после другой страницы")
    public void shouldClickCloseButtonAfterAnotherPage() {
        urlSteps.testing().path(MY).open();
        pofferSteps.onLkSalesPage().header().button("Разместить бесплатно").click();

        pofferSteps.onBetaPofferPage().header().button("Закрыть").waitUntil(isDisplayed())
                .waitUntil(hasAttribute("href", urlSteps.testing().path(MY).path(ALL).toString()))
                .click();

        urlSteps.testing().path(MY).path(ALL).shouldNotSeeDiff();
    }
}