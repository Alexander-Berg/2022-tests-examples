package ru.auto.tests.realtyapi.v1.subscriptions;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.SubscriptionRequest;
import ru.yandex.qatools.allure.annotations.Title;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe400WithInvalidParams;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404WithRequestedResourceCouldNotBeFound;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getEmptyBody;


@Title("PUT /user/subscriptions/{id}")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
@Log4j
@Ignore("HTTP status 409")
public class UpdateUserSubscriptionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    private String subscriptionId;

    @Test
    public void shouldSee400WithoutHeaders() {
        api.subscriptions().updateSubscriptionRoute().idPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee400WithoutOAuth() {
        api.subscriptions().updateSubscriptionRoute().reqSpec(authSpec()).idPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee400WithoutBodyWithoutOAuth() {
        subscriptionId = adaptor.createSubscriptionWithRandomBody().getResponse().getId();
        api.subscriptions().updateSubscriptionRoute().reqSpec(authSpec())
                .idPath(subscriptionId)
                .reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        subscriptionId = adaptor.createSubscriptionWithRandomBody().getResponse().getId();
        api.subscriptions().updateSubscriptionRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .idPath(subscriptionId)
                .reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee400WithEmptyBodyWithoutOAuth() {
        subscriptionId = adaptor.createSubscriptionWithRandomBody().getResponse().getId();
        api.subscriptions().updateSubscriptionRoute().reqSpec(authSpec())
                .idPath(subscriptionId)
                .reqSpec(jsonBody(getEmptyBody()))
                .execute(validatedWith(shouldBe400WithInvalidParams()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        subscriptionId = adaptor.createSubscriptionWithRandomBody().getResponse().getId();
        api.subscriptions().updateSubscriptionRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .idPath(subscriptionId)
                .reqSpec(jsonBody(getEmptyBody()))
                .execute(validatedWith(shouldBe400WithInvalidParams()));
    }

    @Test
    public void shouldSee404ForNotExistSubscriptionWithoutOAuth() {
        api.subscriptions().updateSubscriptionRoute().reqSpec(authSpec())
                .idPath(getRandomString())
                .body(random(SubscriptionRequest.class))
                .execute(validatedWith(shouldBe404WithRequestedResourceCouldNotBeFound()));
    }

    @Test
    public void shouldSee404ForNotExistSubscription() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.subscriptions().updateSubscriptionRoute().reqSpec(authSpec()).authorizationHeader(token)
                .idPath(getRandomString())
                .body(random(SubscriptionRequest.class))
                .execute(validatedWith(shouldBe404WithRequestedResourceCouldNotBeFound()));
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
