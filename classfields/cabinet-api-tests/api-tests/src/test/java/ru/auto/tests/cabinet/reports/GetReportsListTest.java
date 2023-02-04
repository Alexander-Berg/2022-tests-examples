package ru.auto.tests.cabinet.reports;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonArray;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("GET /reports/{report_type}/client/{client_id}/category")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetReportsListTest {

    private static final String USER_ID = "11913489";
    private static final String DEALER_ID = "20101";
    private static final String AGENCY_DEALER_ID = "4254"; //агент Макспостер
    private static final String COMPANY_DEALER_ID = "16011"; //ГК 24авто
    private static final String TYPE_REPORT = "pdf";


    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetReportsListHasNoDiffWithProduction() {
        Function<ApiClient, JsonArray> request = apiClient -> apiClient.reports().getAvailableCategories()
                .clientIdPath(DEALER_ID).reportTypePath(TYPE_REPORT).xAutoruOperatorUidHeader(USER_ID)
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonArray.class);
        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    public void shouldGetReportsListStatusNotFound() {
        api.reports().getAvailableCategories().clientIdPath(DEALER_ID).reportTypePath("pd")
                .xAutoruOperatorUidHeader(USER_ID)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldGetReportsListErrorForbidden() {
        api.reports().getAvailableCategories().clientIdPath("20101e").reportTypePath(TYPE_REPORT)
                .xAutoruOperatorUidHeader("23123123").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetReportsListBadRequest() {
        api.reports().getAvailableCategories().clientIdPath("20101e").reportTypePath(TYPE_REPORT)
                .xAutoruOperatorUidHeader(USER_ID).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldGetReportsListForAgencyStatusOk() {
        api.reports().getAvailableCategories().clientIdPath(AGENCY_DEALER_ID).reportTypePath(TYPE_REPORT)
                .xAutoruOperatorUidHeader("14439810").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportsListForAgencyStatusForbidden() {
        api.reports().getAvailableCategories().clientIdPath(AGENCY_DEALER_ID).reportTypePath(TYPE_REPORT)
                .xAutoruOperatorUidHeader("19144653").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportsListForCompanyStatusOk() {
        api.reports().getAvailableCategories().clientIdPath(COMPANY_DEALER_ID).reportTypePath(TYPE_REPORT)
                .xAutoruOperatorUidHeader("23480672").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportsListForComapanyStatusForbidden() {
        api.reports().getAvailableCategories().clientIdPath(COMPANY_DEALER_ID).reportTypePath(TYPE_REPORT)
                .xAutoruOperatorUidHeader("40416938").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportsListForManagerStatusOk() {
        api.reports().getAvailableCategories().clientIdPath(DEALER_ID).reportTypePath(TYPE_REPORT)
                .xAutoruOperatorUidHeader("19565983").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }
}