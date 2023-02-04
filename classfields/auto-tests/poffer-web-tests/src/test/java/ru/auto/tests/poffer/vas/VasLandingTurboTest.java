package ru.auto.tests.poffer.vas;

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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;
import ru.auto.tests.desktop.step.poffer.PofferSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER_VAS_LANDING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.VAS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник - покупка пакета «Турбо-продажа» на лендинге VAS")
@Feature(POFFER_VAS_LANDING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VasLandingTurboTest {

    private static final String OFFER_ID = "/1076842087-f1e84/";
    private static final String VAS_TITLE = "Турбо-продажа";
    private static final String VAS_PRICE = "547 ₽";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private YaKassaSteps yaKassaSteps;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("poffer/UserOffersCars"),
                stub("poffer/BillingAutoruPaymentInitTurboLanding"),
                stub("poffer/BillingAutoruPaymentProcess"),
                stub("desktop/BillingAutoruPayment")
        ).create();

        urlSteps.testing().path(VAS).path(CARS).path(OFFER_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Покупка пакета «Турбо-продажа»")
    public void shouldBuy() {
        pofferSteps.onPofferPage().vasLanding().pack("Турбо").click();
        pofferSteps.onPofferPage().billingPopupFrame().waitUntil("Не появился поп-ап оплаты", isDisplayed(), 30);
        pofferSteps.onPofferPage().switchToBillingFrame();
        pofferSteps.onPofferPage().billingPopup().waitUntil(isDisplayed());
        pofferSteps.onPofferPage().billingPopup().header().waitUntil(hasText(VAS_TITLE));
        pofferSteps.onPofferPage().billingPopup().priceHeader().waitUntil(hasText(VAS_PRICE));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
    }
}
