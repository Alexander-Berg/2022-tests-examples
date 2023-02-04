package ru.auto.tests.publicapi.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.model.AutoApiUserResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 18.09.17.
 */

@DisplayName("GET /user")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class UserTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.user().getCurrentUser().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSeeSuccess() {
        api.user().getCurrentUser().reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldSeeUser() {
        Account account = am.create();
        AutoApiLoginResponse response = adaptor.login(account);
        AutoApiUserResponse userResponse = api.user().getCurrentUser().xSessionIdHeader(response.getSession().getId()).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBe200OkJSON()));
        assertThat(userResponse.getUser()).hasActive(true).hasId(account.getId());
        assertThat(userResponse.getUser().getPhones().get(0)).hasPhone(account.getLogin());
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Account account = am.create();
        AutoApiLoginResponse response = adaptor.login(account);
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.user().getCurrentUser()
                .xSessionIdHeader(response.getSession().getId())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
