package ru.auto.tests.publicapi.billing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiBillingSchedulesScheduleResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.*;


@DisplayName("GET /billing/schedules")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetSchedulesTest {

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
    public void shouldSee403WithNoAuth() {
        api.billingSchedules().getSchedules().executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee401WithoutSession() {
        AutoApiErrorResponse response = api.billingSchedules().getSchedules().reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR).hasError(NO_AUTH)
                .hasDetailedError("Expected dealer user. Provide valid session_id");
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSeeEmptySchedulesList() {
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiBillingSchedulesScheduleResponse scheduleResponse = api.billingSchedules().getSchedules()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess()));
        Assertions.assertThat(scheduleResponse.getOffers()).isNull();
    }
}
