package ru.auto.tests.publicapi.billing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiBillingSchedulesScheduleRequest.ScheduleTypeEnum.ONCE_AT_TIME;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400IncorrectOfferIdError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401AuthError;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomTime;


@DisplayName("DELETE /billing/schedules/{category}/{offerId}/{product}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DeleteSchedulesTest {

    private static final String DEFAULT_PRODUCT = "all_sale_fresh";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private Account account;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithWrongOfferId() {
        String sessionId = adaptor.login(account).getSession().getId();
        String incorrectOfferId = getRandomString();

        api.billingSchedules().deleteSchedule().categoryPath(CARS.name()).offerIdPath(incorrectOfferId).productPath(DEFAULT_PRODUCT)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBe400IncorrectOfferIdError(incorrectOfferId)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectCategory() {
        String sessionId = adaptor.login(account).getSession().getId();
        String incorrectCategory = getRandomString();
        String incorrectOfferId = getRandomString();

        api.billingSchedules().deleteSchedule().categoryPath(incorrectCategory).offerIdPath(incorrectOfferId).productPath(DEFAULT_PRODUCT)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectProduct() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        String incorrectProduct = getRandomString();
        adaptor.createSchedule(sessionId, CARS, offerId, DEFAULT_PRODUCT, getRandomTime(), ONCE_AT_TIME);

        AutoApiErrorResponse response = api.billingSchedules().deleteSchedule().categoryPath(CARS.name()).offerIdPath(offerId).productPath(incorrectProduct)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);
        AutoruApiModelsAssertions.assertThat(response).hasError(AutoApiErrorResponse.ErrorEnum.BAD_REQUEST).hasStatus(AutoApiErrorResponse.StatusEnum.ERROR).hasDetailedError(String.format("Unknown product %s", incorrectProduct));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee401WithNoAuth() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        api.billingSchedules().deleteSchedule().categoryPath(CARS.name()).offerIdPath(offerId).productPath(DEFAULT_PRODUCT)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe401AuthError()));
    }
}
