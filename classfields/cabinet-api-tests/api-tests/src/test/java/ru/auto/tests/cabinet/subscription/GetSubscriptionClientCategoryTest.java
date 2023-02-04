package ru.auto.tests.cabinet.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /subscriptions/client/{client_id}/category/{category}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetApiModule.class)
public class GetSubscriptionClientCategoryTest {

    private static final String CATEGORY = "money";

    @Inject
    private ApiClient api;

    @Test
    public void shouldSeeStatusOkForManager() {
        String dealerId = "20101";
        String userId = "19565983";

        api.subscription().getByCategory().clientIdPath(dealerId).categoryPath(CATEGORY)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldSeeStatusForbidden() {
        String dealerId = "20101";
        String userId = "1956598";

        api.subscription().getByCategory().clientIdPath(dealerId).categoryPath(CATEGORY)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}