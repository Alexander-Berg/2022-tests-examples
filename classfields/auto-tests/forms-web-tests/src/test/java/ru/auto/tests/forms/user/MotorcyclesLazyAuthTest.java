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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.CreateOfferSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import javax.inject.Inject;
import java.io.IOException;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник, мотоциклы - добавление объявления после ленивой авторизации")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotorcyclesLazyAuthTest {

    private Account account;
    private static final String OFFER_TEMPLATE = "offers/motoOffer.json";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CreateOfferSteps createOfferSteps;

    @Inject
    private AccountManager am;

    @Before
    public void before() throws IOException {
        urlSteps.testing().path(MOTO).path(ADD).addXRealIP(MOSCOW_IP).open();

        account = am.create();
        formsSteps.createMotorcyclesForm();
        formsSteps.setReg(false);
        formsSteps.getPhone().setValue(account.getLogin());
        formsSteps.fillForm(formsSteps.getPhoto().getBlock());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Добавление объявления после ленивой авторизации")
    public void shouldAddFreeSaleAfterLazyAuth() {
        formsSteps.onFormsPage().header().avatar().should(isDisplayed());
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getPhone().getBlock()).getPhone(0)
                .should(hasText(formsSteps.formatPhone(account.getLogin())));
        formsSteps.onFormsPage().userVas().getSnippet(2).submitButton().click();

        formsSteps.onCardPage().cardHeader().title()
                .waitUntil("Объявление не добавилось", hasText("ABM Alpha 110"), 10);

        createOfferSteps.setOfferTemplate(OFFER_TEMPLATE);
        createOfferSteps.getUserOffer(account,
                AutoApiOffer.CategoryEnum.MOTO, formsSteps.getOfferId());
        createOfferSteps.shouldSeeSameOffer(
                createOfferSteps.getUserOffer(account,
                        AutoApiOffer.CategoryEnum.MOTO, formsSteps.getOfferId()), createOfferSteps.getOffer());
    }
}
