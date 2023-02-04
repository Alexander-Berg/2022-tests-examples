package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplateData;
import ru.auto.tests.publicapi.model.AutoApiDraftResponse;
import ru.auto.tests.publicapi.model.AutoApiOffersSaveSuccessResponse;
import ru.auto.tests.publicapi.model.AutoApiPaidServicePrice;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomLicensePlate;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomVin;

@DisplayName("GET /user/draft/{category}/{offerID}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DraftByOfferIdActivationServiceTest {

    private static final int POLL_INTERVAL = 2;
    private static final int TIMEOUT = 30;
    private final static String SERVICE_ALIAS = "all_sale_activate";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    @Owner(TIMONDL)
    @Description("Нулевая цена для Первого объявления легковые автомобили машина из премиума")
    public void shouldSeeNonZeroPriceForPremiumOffer() {
        Account account = am.create();
        OfferTemplateData data = (new OfferTemplateData()).withPhone(account.getLogin())
                .withLicensePlate(getRandomLicensePlate())
                .withVin(getRandomVin());

        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDraftForLuxuryCars(data, sessionId).getOfferId();

        AutoApiDraftResponse response = api.draft().getDraft().categoryPath(CARS).offerIdPath(offerId)
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiPaidServicePrice activationPrice = getActivationPrice(response);

        Assertions.assertThat(activationPrice.getPrice()).isEqualTo(0);
        //Пока в рамках задачи VSMONEY-3288 размещение премиум тачек сделать бесплатно везде
        // закоментировал код так как есть веротность что это все придется вернуть
        //Assertions.assertThat(activationPrice.getPaidReason()).isEqualTo(AutoApiPaidServicePrice.PaidReasonEnum.PREMIUM_OFFER);
        //Assertions.assertThat(activationPrice.getPaymentReason()).isEqualTo(AutoApiPaidServicePrice.PaymentReasonEnum.PREMIUM_OFFER);
    }

    @Test
    @Owner(TIMONDL)
    @Description("Цена и причина для Похожего объявления лекговые")
    public void shouldSeeNonZeroPriceForSameOffer() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.createOffer(account.getLogin(), sessionId, CARS);
        String offerId = adaptor.createDraft(account.getLogin(), sessionId, CARS).getOfferId();

        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS)
                .ignoreExceptions()
                .until(() -> getActivationPrice(api.draft().getDraft().categoryPath(CARS).offerIdPath(offerId)
                        .xSessionIdHeader(sessionId)
                        .reqSpec(defaultSpec())
                        .executeAs(validatedWith(shouldBeSuccess()))).getPrice(), is(not(equalTo(0))));

        AutoApiDraftResponse response = api.draft().getDraft().categoryPath(CARS).offerIdPath(offerId)
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiPaidServicePrice activationPrice = getActivationPrice(response);

        Assertions.assertThat(activationPrice.getPrice()).isGreaterThan(0);
        Assertions.assertThat(activationPrice.getPaidReason()).isEqualTo(AutoApiPaidServicePrice.PaidReasonEnum.FREE_LIMIT);
        Assertions.assertThat(activationPrice.getPaymentReason()).isEqualTo(AutoApiPaidServicePrice.PaymentReasonEnum.FREE_LIMIT_EXCEED);
    }

    @Test
    @Owner(TIMONDL)
    @Description("Цена и причина для второго объявления легковые")
    public void shouldSeeNonZeroPriceForSecondOffer() {
        Account account = am.create();
        OfferTemplateData data = (new OfferTemplateData()).withPhone(account.getLogin())
                .withLicensePlate(getRandomLicensePlate())
                .withVin(getRandomVin());

        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.createOffer(account.getLogin(), sessionId, CARS);
        String draftId = adaptor.createDraft(data, sessionId, CARS, "offers/cars_bmw_3er.ftl").getOfferId();

        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS)
                .ignoreExceptions()
                .until(() -> getActivationPrice(api.draft().getDraft().categoryPath(CARS).offerIdPath(draftId)
                        .xSessionIdHeader(sessionId)
                        .reqSpec(defaultSpec())
                        .executeAs(validatedWith(shouldBeSuccess()))).getPrice(), is(not(equalTo(0))));

        AutoApiDraftResponse response = api.draft().getDraft().categoryPath(CARS).offerIdPath(draftId)
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiPaidServicePrice activationPrice = getActivationPrice(response);

        Assertions.assertThat(activationPrice.getPrice()).isGreaterThan(0);
        Assertions.assertThat(activationPrice.getPaidReason()).isEqualTo(AutoApiPaidServicePrice.PaidReasonEnum.FREE_LIMIT);
        Assertions.assertThat(activationPrice.getPaymentReason()).isEqualTo(AutoApiPaidServicePrice.PaymentReasonEnum.FREE_LIMIT_EXCEED);
    }

    @Test
    @Owner(TIMONDL)
    @Description("Цена и причина для Первого объявления грузовиков")
    public void shouldSeeNonZeroPriceForFirstTruckOffer() {
        Account account = am.create();
        OfferTemplateData data = (new OfferTemplateData()).withPhone(account.getLogin());

        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDraft(data, sessionId, TRUCKS, "offers/truck_offer.ftl").getOfferId();

        AutoApiDraftResponse response = api.draft().getDraft().categoryPath(TRUCKS).offerIdPath(offerId)
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiPaidServicePrice activationPrice = getActivationPrice(response);

        Assertions.assertThat(activationPrice.getPrice()).isGreaterThan(0);
        Assertions.assertThat(activationPrice.getPaidReason()).isEqualTo(AutoApiPaidServicePrice.PaidReasonEnum.FREE_LIMIT);
        Assertions.assertThat(activationPrice.getPaymentReason()).isEqualTo(AutoApiPaidServicePrice.PaymentReasonEnum.FREE_LIMIT_EXCEED);
    }

    @Test
    @Owner(TIMONDL)
    @Description("Нулевая цена для Первого объявления легкие коммерческие")
    public void shouldSeeZeroPriceForFirstLcvOffer() {
        Account account = am.create();
        OfferTemplateData data = (new OfferTemplateData()).withPhone(account.getLogin());

        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDraft(data, sessionId, TRUCKS, "offers/lcv_offer.ftl").getOfferId();

        AutoApiDraftResponse response = api.draft().getDraft().categoryPath(TRUCKS).offerIdPath(offerId)
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiPaidServicePrice activationPrice = getActivationPrice(response);

        Assertions.assertThat(activationPrice.getPrice()).isEqualTo(0);
    }

    @Test
    @Owner(TIMONDL)
    @Description("Нулевая цена для Первого объявления легковые автомобили")
    public void shouldSeeZeroPriceForFirstCarsOffer() {
        Account account = am.create();
        OfferTemplateData data = (new OfferTemplateData()).withPhone(account.getLogin())
                .withLicensePlate(getRandomLicensePlate())
                .withVin(getRandomVin());

        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDraft(data, sessionId, CARS).getOfferId();

        AutoApiDraftResponse response = api.draft().getDraft().categoryPath(CARS).offerIdPath(offerId)
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiPaidServicePrice activationPrice = getActivationPrice(response);

        Assertions.assertThat(activationPrice.getPrice()).isEqualTo(0);
    }

    private AutoApiPaidServicePrice getActivationPrice(AutoApiDraftResponse response) {
        return response.getServicePrices().stream()
                .filter(item -> item.getService().equals(SERVICE_ALIAS))
                .findFirst()
                .get();
    }
}
