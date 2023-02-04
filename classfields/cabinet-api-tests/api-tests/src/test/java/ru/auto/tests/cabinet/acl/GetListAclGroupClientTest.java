package ru.auto.tests.cabinet.acl;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import net.javacrumbs.jsonunit.core.Option;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /acl/client/{client_id}/groups")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetListAclGroupClientTest {

    private static final String MANAGER_ID = "19565983";
    private static final String USER_ID = "11296277";
    private static final String DEALER_ID = "20101";


    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetListAclGroupClientHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> response = apiClient -> apiClient.acl().getClientAccessGroups()
                .clientIdPath(DEALER_ID).reqSpec(defaultSpec()).xAutoruOperatorUidHeader(USER_ID)
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
        MatcherAssert.assertThat(response.apply(api), jsonEquals(response.apply(prodApi))
                .when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    public void shouldGetStatusOkForManager() {
        api.acl().getClientAccessGroups()
                .clientIdPath(DEALER_ID).reqSpec(defaultSpec()).xAutoruOperatorUidHeader(MANAGER_ID)
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        api.acl().getClientAccessGroups()
                .clientIdPath(DEALER_ID).reqSpec(defaultSpec()).xAutoruOperatorUidHeader("1956598")
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}