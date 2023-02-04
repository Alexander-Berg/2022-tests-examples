package ru.auto.tests.publicapi.auth;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
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
import ru.auto.tests.publicapi.model.AutoApiLoginByTokenResponse;
import ru.auto.tests.publicapi.model.VertisPassportLoginByTokenParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /auth/login-by-token")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class LoginByTokenTest {

    private static final String[] IGNORED_PATHS = new String[]{"session.creation_timestamp", "session.device_uid",
            "session.expire_timestamp", "session.id"};

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager accountManager;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    @Owner(TIMONDL)
    public void shouldSuccessGetSessionInfoAfterLoginByToken() {
        Account account = accountManager.create();
        String userLoginToken = adaptor.getUserLoginToken(account.getId());

        AutoApiLoginByTokenResponse response = api.auth().loginByToken().reqSpec(defaultSpec())
                .body(new VertisPassportLoginByTokenParameters().sessionTtlSec(3600).token(userLoginToken))
                .executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(response.getUser()).hasId(account.getId());

        api.session().getSession().reqSpec(defaultSpec())
                .xSessionIdHeader(response.getSession().getId())
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldLoginByTokenHasNoDiffWithProduction() {
        String userId = accountManager.create().getId();

        Function<ApiClient, JsonObject> req = apiClient -> {
            String userLoginToken = adaptor.getUserLoginToken(userId);
            return apiClient.auth().loginByToken().reqSpec(defaultSpec())
                    .body(new VertisPassportLoginByTokenParameters().sessionTtlSec(3600).token(userLoginToken))
                    .execute(validatedWith(shouldBe200OkJSON()))
                    .as(JsonObject.class);
        };

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)).whenIgnoringPaths(IGNORED_PATHS));
    }
}
