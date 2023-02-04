package ru.auto.tests.cabinet.reports;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static org.assertj.core.api.Java6Assertions.assertThat;


@DisplayName("GET /reports/offers-activations/client/{client_id}/link")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetReportsLinkTest {

    private static final String DEALER_ID = "20101";
    private static final String DATE_FROM = "2017-01-01";
    private static final String DATE_TO = "2018-01-01";


    @Inject
    private ApiClient api;

    @Test
    public void shouldGetReportsLinkForDowland() {
        JsonObject response = api.reports().offersExpenses().clientIdPath(DEALER_ID)
                .fromDateQuery(DATE_FROM).toDateQuery(DATE_TO).execute(validatedWith(shouldBeCode(SC_OK)))
                .as(JsonObject.class, ObjectMapperType.GSON);

        assertThat(response.get("processStatus").getAsString()).isEqualTo("READY_TO_BE_DOWNLOADED");
        assertThat(response.get("reportDownloadLink").getAsString()).isNotEmpty();
    }

    @Test
    public void shouldGetReportsLinkMessageEmail() {
        JsonObject response = api.reports().offersExpenses().clientIdPath(DEALER_ID).collectorTimeoutSecondsQuery("1")
                .fromDateQuery(DATE_FROM).toDateQuery("2019-09-19").execute(validatedWith(shouldBeCode(SC_OK)))
                .as(JsonObject.class, ObjectMapperType.GSON);

        assertThat(response.get("processStatus").getAsString()).isEqualTo("WILL_BE_SENT_TO_EMAIL");
    }

    @Test
    public void shouldGetReportsLinkStatusNotFoundBadClient() {
       api.reports().offersExpenses().clientIdPath("20101r")
               .fromDateQuery(DATE_FROM).toDateQuery(DATE_TO)
               .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldGetReportsLinkStatusBadRequest() {
        api.reports().offersExpenses().clientIdPath(DEALER_ID)
                .fromDateQuery(DATE_FROM).toDateQuery("плохой запрос")
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}