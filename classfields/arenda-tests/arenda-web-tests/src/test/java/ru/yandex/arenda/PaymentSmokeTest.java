package ru.yandex.arenda;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.arenda.constants.UriPath.LK;
import static ru.yandex.arenda.constants.UriPath.LK_SDAM;
import static ru.yandex.arenda.constants.UriPath.LK_SNIMU;
import static ru.yandex.arenda.constants.UriPath.OWNER;
import static ru.yandex.arenda.constants.UriPath.PAYMENT_METHODS;
import static ru.yandex.arenda.element.lk.tenantlk.TenantListingFlatSnippet.PAY_BUTTON;
import static ru.yandex.arenda.pages.LkPaymentMethodsPage.ADD_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1832")
@DisplayName("[Arenda] Смоук тест на оплату")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class PaymentSmokeTest {

    private static final String LONG_DATE_EXPIRATION = "1122";
    private static final String CARD_FOR_OWNER = "5000000000000447";
    private static final String TINKOFF_SUCCESS_PAYMENT = "2201382000000112";

    String payUrl;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account accountOwner;

    @Inject
    private Account accountTenant;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        String uidOwner = accountOwner.getId();
        String uidTenant = accountTenant.getId();
        retrofitApiSteps.createUser(uidOwner);
        retrofitApiSteps.createUser(uidTenant);
        String createdFlatId = retrofitApiSteps.createConfirmedFlat(uidOwner);
        retrofitApiSteps.postModerationFlatsQuestionnaire(createdFlatId);
        String userIdTenant = retrofitApiSteps.getUserId(uidTenant);
        retrofitApiSteps.assignToUser(createdFlatId, userIdTenant, "TENANT");
        String contractId = retrofitApiSteps.postModerationFlatContract(createdFlatId);
        retrofitApiSteps.okHouseService(createdFlatId);
        retrofitApiSteps.flatContractUpdateStatus(createdFlatId, contractId);
        passportSteps.login(accountOwner);
        urlSteps.testing().path(LK).path(OWNER).path(PAYMENT_METHODS).open();
        lkSteps.onLkPaymentMethodsPage().button(ADD_BUTTON).click();

        urlSteps.shouldUrl("Должны быть на странице привязки карты",
                containsString("https://securepay.tinkoff.ru/e2c/AddCard/"));
        lkSteps.onLkPaymentMethodsPage().cardNumberInput().click();
        lkSteps.onLkPaymentMethodsPage().cardNumberInput().sendKeys(CARD_FOR_OWNER);
        lkSteps.onLkPaymentMethodsPage().h2().click();
        lkSteps.onLkPaymentMethodsPage().cardExpiration().click();
        lkSteps.onLkPaymentMethodsPage().cardExpiration().sendKeys(LONG_DATE_EXPIRATION);
        lkSteps.onLkPaymentMethodsPage().button("Добавить").click();
        urlSteps.queryParam("tinkoffCardBindingStatus", "success").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Полный цикл оплаты. Должны видеть оплату от жильца")
    public void shouldSeePayment() {
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton();

        passportSteps.logoff();
        passportSteps.login(accountTenant);
        urlSteps.testing().path(LK_SNIMU).open();
        String flatUrl = urlSteps.getCurrentUrl();
        lkSteps.onLkTenantFlatListingPage().toAppPopup().closeCross().click();
        lkSteps.onLkTenantFlatListingPage().button(PAY_BUTTON).click();
        urlSteps.shouldUrl(containsString("rest-api-test.tinkoff.ru/new/"));
        payUrl = urlSteps.getCurrentUrl();
        lkSteps.performInNewSession(this::tinkoffPagePayment);

        urlSteps.fromUri(flatUrl).open();
        lkSteps.refreshUntil(() -> lkSteps.onLkTenantFlatListingPage().mainContent(),
                hasText(containsString("Аренда оплачена")), 30);

        passportSteps.logoff();
        passportSteps.login(accountOwner);
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().insuranceModal().closeCrossModal().click();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton()
                .should(hasText(containsString("Арендная плата успешно отправлена на вашу карту 500000******0447.")));
    }

    private void tinkoffPagePayment() {
        urlSteps.fromUri(payUrl).open();
        lkSteps.onTinkoffPayPage().cardNumber().waitUntil(isDisplayed(), 30).click();
        lkSteps.onTinkoffPayPage().cardNumber().sendKeys(TINKOFF_SUCCESS_PAYMENT);
        lkSteps.onTinkoffPayPage().cvc().click();
        lkSteps.onTinkoffPayPage().cvc().sendKeys("123");
        lkSteps.onTinkoffPayPage().expDate().click();
        lkSteps.onTinkoffPayPage().expDate().sendKeys(LONG_DATE_EXPIRATION);
        lkSteps.onTinkoffPayPage().button("Оплатить").click();
        urlSteps.waitForUrl("https://arenda.test.vertis.yandex.ru/lk/flat/", 51);
    }
}
