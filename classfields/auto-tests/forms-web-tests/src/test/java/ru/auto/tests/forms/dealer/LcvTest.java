package ru.auto.tests.forms.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.step.CreateOfferSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Дилер, лёгкие коммерческие - добавление объявлений")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class LcvTest {

    private Account account;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private CreateOfferSteps createOfferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() throws IOException {
        account = formsSteps.linkUserToDealer();
        loginSteps.loginAs(account);

        urlSteps.testing().path(TRUCKS).path(ADD).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Добавление объявления о продаже нового ТС")
    public void shouldAddNewSale() throws IOException {
        formsSteps.createLcvDealerNewForm();
        formsSteps.fillForm(formsSteps.getPhoto().getBlock());
        formsSteps.submitForm();

        formsSteps.onCardPage().cardHeader().title()
                .waitUntil("Объявление не добавилось", hasText("ГАЗ ГАЗель (2705)"), 10);

        createOfferSteps.setOfferTemplate("offers/lcvDealerNewOffer.json");
        createOfferSteps.getUserOffer(account,
                AutoApiOffer.CategoryEnum.TRUCKS, formsSteps.getOfferId());
        createOfferSteps.shouldSeeSameOffer(
                createOfferSteps.getUserOffer(account,
                        AutoApiOffer.CategoryEnum.TRUCKS, formsSteps.getOfferId()), createOfferSteps.getOffer());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Добавление объявления о продаже б/у ТС")
    public void shouldAddUsedSale() throws IOException {
        formsSteps.createLcvDealerUsedForm();
        formsSteps.fillForm(formsSteps.getPhoto().getBlock());
        formsSteps.submitForm();

        formsSteps.onCardPage().cardHeader().title()
                .waitUntil("Объявление не добавилось", hasText("ГАЗ ГАЗель (2705)"), 10);

        createOfferSteps.setOfferTemplate("offers/lcvDealerUsedOffer.json");
        createOfferSteps.getUserOffer(account,
                AutoApiOffer.CategoryEnum.TRUCKS, formsSteps.getOfferId());
        createOfferSteps.shouldSeeSameOffer(
                createOfferSteps.getUserOffer(account,
                        AutoApiOffer.CategoryEnum.TRUCKS, formsSteps.getOfferId()), createOfferSteps.getOffer());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Добавление объявления о продаже нового ТС с услугами")
    public void shouldAddNewSaleWithServices() throws IOException, InterruptedException {
        formsSteps.createLcvDealerNewForm();
        formsSteps.fillForm(formsSteps.getPhoto().getBlock());
        formsSteps.onFormsPage().dealerVas().vas("Турбо-продажа").click();
        formsSteps.onFormsPage().dealerVas().vas("Премиум-объявление").click();
        formsSteps.onFormsPage().dealerVas().vas("Спецпредложение").click();
        formsSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/forms/saveDraftFormsToPublicApi",
                hasJsonBody("drafts/lcvDealerNewOfferWithServices.json")
        ));
        urlSteps.shouldUrl(containsString("/lcv/new/sale/"));
        formsSteps.onCardPage().cardHeader().title()
                .waitUntil("Объявление не добавилось", hasText("ГАЗ ГАЗель (2705)"), 10);
    }

    @After
    public void after() {
        formsSteps.deleteOffers(AutoApiOffer.CategoryEnum.TRUCKS.toString().toLowerCase());
    }
}
