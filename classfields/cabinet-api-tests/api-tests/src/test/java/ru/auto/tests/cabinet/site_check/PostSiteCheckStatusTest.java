package ru.auto.tests.cabinet.site_check;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.model.DealerSiteCheckEntity;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.time.OffsetDateTime;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /site-check/client/{client_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class PostSiteCheckStatusTest {

    private static final String DEALER_ID = "20101";
    private static final String MANAGER_ID = "19565983";
    private static final String COMMENT = "comment";
    private static final String TICKET = "ticket";
    private static final Boolean RESOLUTION = true;
    private static final OffsetDateTime TIME = OffsetDateTime.now();

    @Inject
    private ApiClient api;

    @Test
    public void shouldGetStatusOkForManager() {
        api.siteCheck().addOrUpdateSiteCheck().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader(MANAGER_ID)
                .reqSpec(defaultSpec()).body(new DealerSiteCheckEntity().checkDate(TIME)
                .resolution(RESOLUTION).comment(COMMENT).ticket(TICKET))
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbedden() {
        api.siteCheck().addOrUpdateSiteCheck().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader("121122")
                .reqSpec(defaultSpec()).body(new DealerSiteCheckEntity().checkDate(TIME)
                .resolution(RESOLUTION).comment(COMMENT).ticket(TICKET))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetStatusBadRequest() {
        api.siteCheck().addOrUpdateSiteCheck().clientIdPath(DEALER_ID).xAutoruOperatorUidHeader(MANAGER_ID)
                .reqSpec(defaultSpec()).body(new DealerSiteCheckEntity()
                .resolution(RESOLUTION).comment(COMMENT).ticket(TICKET))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldGetStatusNotFound() {
        api.siteCheck().addOrUpdateSiteCheck().clientIdPath("q").xAutoruOperatorUidHeader(MANAGER_ID)
                .reqSpec(defaultSpec()).body(new DealerSiteCheckEntity()
                .resolution(RESOLUTION).comment(COMMENT).ticket(TICKET))
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }
}