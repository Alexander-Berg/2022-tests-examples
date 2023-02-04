package ru.auto.tests.publicapi.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.model.AutoApiUserResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


/**
 * Created by vicdev on 18.09.17.
 */

@DisplayName("GET /user/{userId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class UserIdTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.user().getUser().userIDPath(account.getId()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Issue("AUTORUOFFICE-4649")
    public void shouldSeeUser() {
        AutoApiUserResponse response = api.user().getUser().userIDPath(account.getId()).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBe200OkJSON()));
        AutoruApiModelsAssertions.assertThat(response.getUser()).hasId(account.getId());
    }

    @Test
    @Issue("AUTORUOFFICE-4649")
    public void shouldUserHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.user().getUser().userIDPath(account.getId()).reqSpec(defaultSpec()).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }

    @Test
    public void shouldFullUserInfoHasNoDiffWithProduction() {
        AutoApiLoginResponse response = adaptor.login(account);

        api.user().getUser().userIDPath(account.getId())
                .xSessionIdHeader(response.getSession().getId()).reqSpec(defaultSpec()).execute(validatedWith(shouldBe200OkJSON()));

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.user().getUser().userIDPath(account.getId())
                .xSessionIdHeader(response.getSession().getId()).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
