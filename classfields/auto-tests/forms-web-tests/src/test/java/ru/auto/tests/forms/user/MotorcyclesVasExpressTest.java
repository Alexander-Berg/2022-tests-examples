package ru.auto.tests.forms.user;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.CreateOfferSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник, мотоциклы - добавление объявлений с услугами")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotorcyclesVasExpressTest {

    private Account account;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AccountManager am;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private CreateOfferSteps createOfferSteps;

    @Inject
    private YaKassaSteps yaKassaSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() throws IOException {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        account = am.create();
        loginSteps.loginAs(account);

        formsSteps.createMotorcyclesForm();
        formsSteps.getPhone().setValue(account.getLogin());

        urlSteps.testing().path(MOTO).path(ADD).addXRealIP(MOSCOW_IP).open();
        formsSteps.fillForm(formsSteps.getPhoto().getBlock());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Billing.class})
    @DisplayName("Добавление объявления с пакетом услуг «Экспресс-продажа»")
    public void shouldAddSaleWithExpressPackage() {
        formsSteps.onFormsPage().userVas().getSnippet(0).submitButton().click();
        formsSteps.onFormsPage().billingPopupFrame().waitUntil(isDisplayed());
        formsSteps.onFormsPage().switchToBillingFrame();
        formsSteps.onFormsPage().billingPopup().waitUntil(isDisplayed());
        formsSteps.onFormsPage().billingPopup().header().waitUntil(hasText("Экспресс продажа"));
        formsSteps.onFormsPage().billingPopup().priceHeader().waitUntil(hasText("1 257 ₽"));
        formsSteps.onFormsPage().billingPopup().checkbox("Запомнить карту").click();
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        formsSteps.onCardPage().billingPopupCloseButton().click();
        formsSteps.onCardPage().billingPopup().waitUntil(not(isDisplayed()));

        formsSteps.onCardPage().cardHeader().title()
                .waitUntil("Объявление не добавилось", hasText("ABM Alpha 110"), 10);

        createOfferSteps.setOfferTemplate("offers/motoExpressOffer.json");
        createOfferSteps.getUserOffer(account,
                AutoApiOffer.CategoryEnum.MOTO, formsSteps.getOfferId());
        createOfferSteps.shouldSeeSameOffer(
                createOfferSteps.getUserOffer(account,
                        AutoApiOffer.CategoryEnum.MOTO, formsSteps.getOfferId()), createOfferSteps.getOffer());
    }
}
