package ru.auto.tests.forms.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.auto.tests.desktop.step.LoginSteps;
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

@DisplayName("Частник, мотоциклы - добавление бесплатного объявления")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotorcyclesFreeSaleTest {

    private static final String OFFER_TEMPLATE = "offers/motoOffer.json";
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

    @Before
    public void before() throws IOException {
        account = am.create();
        loginSteps.loginAs(account);
        formsSteps.createMotorcyclesForm();

        urlSteps.testing().path(MOTO).path(ADD).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @Issue("AUTORUBACK-2610")
    @DisplayName("Добавление бесплатного объявления")
    public void shouldAddFreeSale() throws IOException {
        formsSteps.fillForm(formsSteps.getPhoto().getBlock());
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
