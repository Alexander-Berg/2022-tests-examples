package ru.auto.tests.publicapi.billing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiBillingSchedulesScheduleRequest;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static com.google.common.collect.Lists.newArrayList;
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


@DisplayName("POST /billing/schedules/{category}/{offerId}/{product}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class CreateSchedulesTest {

    private static String DEFAULT_PRODUCT = "all_sale_fresh";
    private List<Integer> INCORRECT_WEEKDAYS = newArrayList(8);

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
        api.billingSchedules().upsertSchedule().categoryPath(CARS).offerIdPath(incorrectOfferId).productPath(DEFAULT_PRODUCT)
                .body(getDefaultSchelduleRequest())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBe400IncorrectOfferIdError(incorrectOfferId)));

    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectCategory() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        String incorrectCategory = getRandomString();
        api.billingSchedules().upsertSchedule()
                .categoryPath(incorrectCategory)
                .offerIdPath(offerId)
                .productPath(DEFAULT_PRODUCT)
                .body(getDefaultSchelduleRequest())
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectProduct() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        String incorrectProduct = getRandomString();
        api.billingSchedules().upsertSchedule().categoryPath(CARS.name()).offerIdPath(offerId).productPath(incorrectProduct)
                .body(getDefaultSchelduleRequest())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithNoBody() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        api.billingSchedules().upsertSchedule().categoryPath(CARS.name()).offerIdPath(offerId).productPath(DEFAULT_PRODUCT)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectWeekdays() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        api.billingSchedules().upsertSchedule().categoryPath(CARS.name()).offerIdPath(offerId).productPath(DEFAULT_PRODUCT)
                .body(new AutoApiBillingSchedulesScheduleRequest()
                        .timezone(getRandomTimeZone()).weekdays(INCORRECT_WEEKDAYS).time(getRandomTime()).scheduleType(ONCE_AT_TIME))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee401WithNoAuth() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        api.billingSchedules().upsertSchedule().categoryPath(CARS.name()).offerIdPath(offerId).productPath(DEFAULT_PRODUCT)
                .body(getDefaultSchelduleRequest())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe401AuthError()));
    }


    static String getRandomTimeZone() {
        Random rnd = new Random();
        String sign;
        if ((RandomUtils.nextInt(0, 10) - 5) > 0) {
            sign = "+";
        } else {
            sign = "-";
        }
        Date date = new Date(Math.abs(System.currentTimeMillis() - rnd.nextLong()));
        SimpleDateFormat sdf = new SimpleDateFormat(sign + "HH:00");
        return sdf.format(date);
    }

    public static AutoApiBillingSchedulesScheduleRequest getDefaultSchelduleRequest() {
        return new AutoApiBillingSchedulesScheduleRequest().time(getRandomTime()).scheduleType(ONCE_AT_TIME);
    }
}