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


@DisplayName("GET /reports/{report_type}/source/client/{client_id}/category/{category}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetReportsFrontCategoryTest {

    private static final String DATE_REPORT = "2019-08";
    private static final String USER_ID = "11913489";
    private static final String DEALER_ID = "20101";
    private static final String AGENCY_DEALER_ID = "4254"; //агент Макспостер
    private static final String COMPANY_DEALER_ID = "16011"; //ГК 24авто
    private static final String CATEGORY = "total";
    private static final String TYPE_REPORT = "pdf";

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetReportFrontCategoryStatusOk() {
        api.reports().getReportSource().reportTypePath(TYPE_REPORT).clientIdPath(DEALER_ID).categoryPath(CATEGORY)
                .dateQuery(DATE_REPORT).xAutoruOperatorUidHeader(USER_ID).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportFrontCategoryStatusNotFound() {
        api.reports().getReportSource().reportTypePath(TYPE_REPORT).clientIdPath("2010").categoryPath(CATEGORY)
                .dateQuery(DATE_REPORT).xAutoruOperatorUidHeader(USER_ID).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldGetReportFrontCategoryStatusBadRequest() {
        api.reports().getReportSource().reportTypePath(TYPE_REPORT).clientIdPath(DEALER_ID).categoryPath(CATEGORY)
                .dateQuery("2019-0").xAutoruOperatorUidHeader(USER_ID).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldGetReportFrontCategoryStatusForbidden() {
        api.reports().getReportSource().reportTypePath(TYPE_REPORT).clientIdPath(DEALER_ID).categoryPath(CATEGORY)
                .dateQuery(DATE_REPORT).xAutoruOperatorUidHeader("1191348").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetReportFrontCategoryForAgencyStatusOk() {
        api.reports().getReportSource().reportTypePath(TYPE_REPORT).clientIdPath(AGENCY_DEALER_ID).categoryPath(CATEGORY)
                .dateQuery(DATE_REPORT).xAutoruOperatorUidHeader("14439810").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportFrontCategoryForAgencyStatusForbidden() {
        api.reports().getReportSource().reportTypePath(TYPE_REPORT).clientIdPath(AGENCY_DEALER_ID).categoryPath(CATEGORY)
                .dateQuery(DATE_REPORT).xAutoruOperatorUidHeader("19144653").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetReportFrontCategoryForManagerStatusOk() {
        api.reports().getReportSource().reportTypePath(TYPE_REPORT).clientIdPath(DEALER_ID).categoryPath(CATEGORY)
                .dateQuery(DATE_REPORT).xAutoruOperatorUidHeader("19565983").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportFrontCategoryForCompanyStatusOk() {
        api.reports().getReportSource().reportTypePath(TYPE_REPORT).clientIdPath(COMPANY_DEALER_ID).categoryPath(CATEGORY)
                .dateQuery(DATE_REPORT).xAutoruOperatorUidHeader("23480672").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetReportFrontCategoryForCompanyStatusForbidden() {
        api.reports().getReportSource().reportTypePath(TYPE_REPORT).clientIdPath(COMPANY_DEALER_ID).categoryPath(CATEGORY)
                .dateQuery(DATE_REPORT).xAutoruOperatorUidHeader("40416938").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

}