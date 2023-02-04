package ru.auto.tests.publicapi.user.profile;


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
import ru.auto.tests.publicapi.ResponseSpecBuilders;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Created by dskuznetsov on 05.09.18.
 */

@DisplayName("GET /user/profile/userpic-upload-uri")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class UserPicUploadUriTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.userProfile().userpicUploadUri().executeAs(validatedWith(ResponseSpecBuilders.shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WhenNoSession() {
        api.userProfile().userpicUploadUri()
                .reqSpec(defaultSpec()).executeAs(validatedWith(ResponseSpecBuilders.shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldGetUserPicUploadUriHasNoDiffWithProduction() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.userProfile().userpicUploadUri()
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}