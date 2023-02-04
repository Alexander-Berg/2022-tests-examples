package ru.auto.tests.publicapi.dealer;

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
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getMajorAccount;

@DisplayName("GET /dealer/phones/redirects")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class PhonesRedirectsTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    public PublicApiAdaptor adaptor;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee403WhenNoAuth() {
        api.dealer().getDealerPhonesRedirects().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee401WithoutSession() {
        api.dealer().getDealerPhonesRedirects().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldGetRedirectsHasNoDiffWithProduction() {
        String sessionId = adaptor.login(getMajorAccount()).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.dealer().getDealerPhonesRedirects().xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
