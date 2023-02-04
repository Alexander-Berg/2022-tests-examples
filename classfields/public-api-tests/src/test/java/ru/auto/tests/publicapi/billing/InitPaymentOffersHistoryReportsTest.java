package ru.auto.tests.publicapi.billing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentResponse;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequest;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequestProduct;
import ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequestSubscribePurchase;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequestSubscribePurchase.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiBillingInitPaymentRequestSubscribePurchase.SectionEnum.USED;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("POST /billing/{salesmanDomain}/payment/init")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class InitPaymentOffersHistoryReportsTest {

    private static final String DOMAIN = "autoru";
    private static final String OFFERS_HISTORY_REPORTS = "offers-history-reports";
    private static final String VIN = "SALWA2FK7HA135034";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager accountManager;

    @Parameterized.Parameter
    public Integer daysCount;

    @Parameterized.Parameters
    public static List<Integer> getParameters() {
        return newArrayList(15, 30, 45);
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSeeTicketIdForSingleVinHistoryReport() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiBillingInitPaymentResponse response = api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingInitPaymentRequest()
                        .addProductItem(new AutoApiBillingInitPaymentRequestProduct().name(OFFERS_HISTORY_REPORTS).count(1))
                        .subscribePurchase(new AutoApiBillingInitPaymentRequestSubscribePurchase().count(1).vinOrLicensePlate(VIN)))
                .executeAs(validatedWith(shouldBe200OkJSON()));

        Assertions.assertThat(response.getTicketId()).isNotNull();
        Assertions.assertThat(response.getPaymentMethods()).isNotNull().isNotEmpty();
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSeeTicketIdForVinHistoryReportPackageWithoutCarInfo() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiBillingInitPaymentResponse response = api.billing().initPayment().reqSpec(defaultSpec())
                .salesmanDomainPath(DOMAIN)
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingInitPaymentRequest()
                        .addProductItem(new AutoApiBillingInitPaymentRequestProduct().name(OFFERS_HISTORY_REPORTS).count(1))
                        // не передаём ни offerId, ни vinOrLicensePlate -- пакеты можно покупать без них
                        .subscribePurchase(new AutoApiBillingInitPaymentRequestSubscribePurchase().count(10)))
                .executeAs(validatedWith(shouldBe200OkJSON()));

        Assertions.assertThat(response.getTicketId()).isNotNull();
        Assertions.assertThat(response.getPaymentMethods()).isNotNull().isNotEmpty();
    }
}
