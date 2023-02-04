package ru.auto.tests.cabinet.reports;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("GET /reports/{report_type}/client/{client_id}/limit/{limit}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetReportsListCategoryTest {

    private static final String USER_ID = "11913489";
    private static final String DEALER_ID = "20101";
    private static final String AGENCY_DEALER_ID = "4254"; //агент Макспостер
    private static final String COMPANY_DEALER_ID = "16011"; //ГК 24авто
    private static final String LIMIT = "9999999";
    private static final String TYPE_REPORT = "pdf";
    private static final String CATEGORY = "cars:used";


    @Inject
    private ApiClient api;

    @Test
    public void shouldGetReportsListCategoryStatusNotFound() {
        api.reports().getReportLinksByClient().reportTypePath(TYPE_REPORT).categoryQuery(CATEGORY)
                .clientIdPath("20101e").limitPath(LIMIT).xAutoruOperatorUidHeader(USER_ID)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldGetReportsListCategoryStatusOk() {
        api.reports().getReportLinksByClient().reportTypePath(TYPE_REPORT).categoryQuery(CATEGORY)
                .clientIdPath(DEALER_ID).limitPath(LIMIT).xAutoruOperatorUidHeader(USER_ID)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportsListCategoryStatusBadRequest() {
        api.reports().getReportLinksByClient().reportTypePath(TYPE_REPORT).categoryQuery("cars:use")
                .clientIdPath(DEALER_ID).limitPath(LIMIT).xAutoruOperatorUidHeader(USER_ID)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldGetReportsListCategoryStatusForbidden() {
        api.reports().getReportLinksByClient().reportTypePath(TYPE_REPORT).categoryQuery(CATEGORY)
                .clientIdPath(DEALER_ID).limitPath(LIMIT).xAutoruOperatorUidHeader("12")
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetReportsListCategoryForAgencyStatusOk() {
        api.reports().getReportLinksByClient().reportTypePath(TYPE_REPORT).categoryQuery(CATEGORY)
                .clientIdPath(AGENCY_DEALER_ID).limitPath(LIMIT).xAutoruOperatorUidHeader("14439810")
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportsListCategoryForAgencyStatusForbidden() {
        api.reports().getReportLinksByClient().reportTypePath(TYPE_REPORT).categoryQuery(CATEGORY)
                .clientIdPath(AGENCY_DEALER_ID).limitPath(LIMIT).xAutoruOperatorUidHeader("19144653")
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetReportsListCategoryForManagerStatusOk() {
        api.reports().getReportLinksByClient().reportTypePath(TYPE_REPORT).categoryQuery(CATEGORY)
                .clientIdPath(DEALER_ID).limitPath(LIMIT).xAutoruOperatorUidHeader("19565983")
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportsListCategoryForCompanyStatusOk() {
        api.reports().getReportLinksByClient().reportTypePath(TYPE_REPORT).categoryQuery(CATEGORY)
                .clientIdPath(COMPANY_DEALER_ID).limitPath(LIMIT).xAutoruOperatorUidHeader("23480672")
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportsListCategoryForCompanyStatusForbidden() {
        api.reports().getReportLinksByClient().reportTypePath(TYPE_REPORT).categoryQuery(CATEGORY)
                .clientIdPath(COMPANY_DEALER_ID).limitPath(LIMIT).xAutoruOperatorUidHeader("40416938")
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}