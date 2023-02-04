package ru.auto.tests.mobile.vas;

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
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Предпочитаемая карта для оплаты")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class PreferredCardTest {

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
        mockRule.newMock().with("desktop/User",
                "desktop/SessionAuthUser",
                "desktop/UserOffersCarsActive",
                "mobile/BillingAutoruPaymentInitLkTurboTiedCards",
                "desktop/BillingAutoruTiedCardsPut").post();

        urlSteps.testing().path(MY).path(CARS).open();

        mockRule.overwriteStub(1, "desktop/UserOffersCarsActiveWithServices");
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор карты для постоянной оплаты")
    public void shouldSeePreferredCard() {
        basePageSteps.openVasPopup("Турбо-продажа");
        basePageSteps.switchToPaymentMethodsFrame();
        basePageSteps.onBasePage().paymentMethodsFrameContent().should(hasText("Турбо продажа\n" +
                "Автоматически продлевать\n897 ₽ каждые 3 дня\nMasterCard **** 4444\nИзменить\nОплатить 897 ₽\n" +
                "Совершая платеж, вы соглашаетесь с условиями Оферты"));
        basePageSteps.onBasePage().paymentMethodsFrameContent().button("Изменить").click();
        basePageSteps.onBasePage().paymentMethodsFrameContent().should(hasText("Турбо продажа\nАвтоматически " +
                "продлевать\n897 ₽ каждые 3 дня\nMasterCard **** 4444\nVisa **** 1111\nСбербанк Онлайн\n" +
                "ЮMoney\nQIWI Кошелек\nWebmoney\nНовая карта\nОплатить 897 ₽\nСовершая платеж, " +
                "вы соглашаетесь с условиями Оферты"));
        basePageSteps.switchToDefaultFrame();
    }
}
