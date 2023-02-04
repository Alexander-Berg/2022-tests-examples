package ru.auto.tests.realtyapi.v1.subscriptions;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("GET /user/subscriptions")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
@Log4j
@Ignore("HTTP status 409")
public class GetUserSubscriptionsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    private String subscriptionId;

    @Test
    public void shouldSee200WithoutOAuth() {
        api.subscriptions().getUserSubscriptionsRoute().reqSpec(authSpec()).execute(validatedWith(shouldBe200OkJSON()));
    }

    @Test
    public void shouldOfferHasNoDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        subscriptionId = adaptor.createSubscriptionWithRandomBody().getResponse().getId();

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.subscriptions().getUserSubscriptionsRoute()
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @After
    public void deleteSubscriptions() {
        try {
            if (subscriptionId != null) {
                adaptor.deleteSubscription(subscriptionId);
            }
        } catch (Exception e) {
            log.info(format("Can't delete subscription %s. Exception: %s", subscriptionId, e.getMessage()));
        }
    }
}
