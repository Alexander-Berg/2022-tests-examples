package ru.auto.tests.cabinet.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /subscriptions/agency/{agency_id}/client/{client_id}/category/{category}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetSubscriptionsClientAgentStatusTest {

    private static final String CATEGORY = "money";
    private static final String EMAIL = getRandomEmail();
    private static final String AGENCY_ID = "19030";
    private static final String MANAGER_ID = "19565983";

    @Inject
    private ApiClient api;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Test
    public void shouldSeeStatusOkForManager() {
        String dealerId = "30082";
        String userId = "27187476";

        adaptor.clearSubscriptions(dealerId, userId);
        adaptor.addSubscription(dealerId, MANAGER_ID, CATEGORY, EMAIL);
        api.subscription().getByCustomer().clientIdPath(dealerId).categoryPath(CATEGORY).agencyIdPath(AGENCY_ID)
                .xAutoruOperatorUidHeader(MANAGER_ID).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldSeeStatusForbidden() {
        String dealerId = "36681";

        api.subscription().getByCustomer().clientIdPath(dealerId).categoryPath(CATEGORY).agencyIdPath("14336")
                .xAutoruOperatorUidHeader("721301").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}