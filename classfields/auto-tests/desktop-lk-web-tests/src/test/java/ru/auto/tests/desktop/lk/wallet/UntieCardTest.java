package ru.auto.tests.desktop.lk.wallet;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кошелёк - отвязка карты")
@Feature(LK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class UntieCardTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserWithTiedCards",
                "desktop/BillingAutoruTiedCardsDelete").post();

        urlSteps.testing().path(MY).path(WALLET).open();

        mockRule.overwriteStub(1, "desktop/User");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отвязка карты")
    public void shouldUntieCard() {
        basePageSteps.onWalletPage().tiedCards().removeCardButton().click();
        basePageSteps.acceptAlert();
        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed()).waitUntil(hasText("Карта отвязана"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отвязка карты после выбора её из выпадушки")
    public void shouldUntieCardFromSelect() {
        String firstCardNumber = "4111 11** **** 1111";
        String secondCardNumber = "4111 11** **** 1112";

        basePageSteps.onWalletPage().tiedCards().select(firstCardNumber).click();
        basePageSteps.onWalletPage().tiedCards().selectPopup().itemsList().should(hasSize(2));
        basePageSteps.onWalletPage().tiedCards().selectPopup().getItem(0).should(hasText(firstCardNumber));
        basePageSteps.onWalletPage().tiedCards().selectPopup().getItem(1).should(hasText(secondCardNumber));
        basePageSteps.onWalletPage().tiedCards().selectPopup().item(firstCardNumber).click();
        basePageSteps.onWalletPage().tiedCards().removeCardButton().click();
        basePageSteps.acceptAlert();
        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed()).waitUntil(hasText("Карта отвязана"));
    }
}