package ru.auto.tests.mobile.my;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mobile.page.LkCreditsPage.SBERBANK_HOST_MOBILE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кредитный брокер")
@Feature(AutoruFeatures.LK)
@Story(AutoruFeatures.CREDITS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CreditsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/SharkBankList"),
                stub("desktop/SharkCreditApplicationActiveWithOffersWithPersonProfilesActive"),
                stub("desktop/SharkCreditApplicationListWithOffers"),
                stub("desktop/SharkCreditProductListWithoutParams"),
                stub("mobile/SharkCreditProductListByCreditApplication"),
                stub("mobile/SharkCreditApplicationActiveWithOffers")
        ).create();

        urlSteps.testing().path(MY).path(CREDITS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отмена заявки")
    public void shouldCancelApplication() {
        mockRule.overwriteStub(2, stub("desktop-lk/SharkCreditApplicationActiveWithOffersCanceledClaim"));
        mockRule.overwriteStub(3, stub("desktop-lk/SharkCreditApplicationCancelProducts"));

        basePageSteps.onLkCreditsPage().creditsClaimsList().get(0).button("Отменить").click();
        basePageSteps.onLkCreditsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Вы успешно отменили кредитную заявку в банк"));
        basePageSteps.onLkCreditsPage().creditsClaimsList().should(hasSize(11));
        basePageSteps.onLkCreditsPage().getCreditClaim(0).status()
                .should(hasText("Вы отказались от этого предложения"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Клик по кнопке «Редактировать заявку»")
    public void shouldEditApplication() {
        basePageSteps.onLkCreditsPage().button("Редактировать заявку").click();
        urlSteps.testing().path(MY).path(CREDITS).path(EDIT).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по кнопке «Перейти в Сбербанк Онлайн»")
    public void shouldClickSberbankButton() {
        mockRule.setStubs(stub("mobile/SharkCreditApplicationAddProductSber")).update();

        basePageSteps.onLkCreditsPage().button("Перейти в Сбербанк Онлайн").click();
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.switchToNextTab();

        urlSteps.shouldSeeHost(is(SBERBANK_HOST_MOBILE));
    }
}
