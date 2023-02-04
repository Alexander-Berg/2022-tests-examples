package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponseAssert;
import ru.auto.tests.publicapi.model.AutoApiPushDelivery;
import ru.auto.tests.publicapi.module.PublicApiSearchesModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.SUBSCRIPTION_NOT_FOUND;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("PUT /user/favorites/all/subscriptions/{id}/push")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiSearchesModule.class)
public class PushSubscriptionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.userFavorites().upsertPushDelivery().idPath(Utils.getRandomString()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void should400WithoutBody() {
        String id = Utils.getRandomString();
        api.userFavorites().upsertPushDelivery().idPath(id)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void should404ForNotExistId() {
        String id = Utils.getRandomString();
        AutoApiErrorResponse response = api.userFavorites().upsertPushDelivery().idPath(id)
                .reqSpec(defaultSpec()).body(new AutoApiPushDelivery().enabled(false)).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response).hasStatus(AutoApiErrorResponse.StatusEnum.ERROR)
                .hasError(SUBSCRIPTION_NOT_FOUND)
                .hasDetailedError(SUBSCRIPTION_NOT_FOUND.name());
    }
}
