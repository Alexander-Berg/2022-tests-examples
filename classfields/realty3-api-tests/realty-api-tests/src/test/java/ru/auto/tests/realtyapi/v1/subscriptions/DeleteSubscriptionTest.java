package ru.auto.tests.realtyapi.v1.subscriptions;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;
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
import ru.yandex.qatools.allure.annotations.Title;

import static java.lang.String.format;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404WithRequestedResourceCouldNotBeFound;


@Title("DELETE /user/subscriptions/{id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
@Log4j
@Ignore("HTTP status 409")
public class DeleteSubscriptionTest {

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
    public void shouldSuccessDeleteSubscription() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        subscriptionId = adaptor.createSubscriptionWithRandomBody().getResponse().getId();

        api.subscriptions().deleteSubscriptionRoute().reqSpec(authSpec()).authorizationHeader(token)
                .idPath(subscriptionId)
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Test
    public void shouldSuccessDeleteSubscriptionWithoutOAuth() {
        subscriptionId = adaptor.createSubscriptionWithRandomBody().getResponse().getId();

        api.subscriptions().deleteSubscriptionRoute().reqSpec(authSpec())
                .idPath(subscriptionId)
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Test
    public void shouldSee404WithTwiceDeleteSubscription() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        subscriptionId = adaptor.createSubscriptionWithRandomBody().getResponse().getId();

        api.subscriptions().deleteSubscriptionRoute().reqSpec(authSpec()).authorizationHeader(token)
                .idPath(subscriptionId)
                .execute(validatedWith(shouldBe200OkJSON()));

        api.subscriptions().deleteSubscriptionRoute().reqSpec(authSpec()).authorizationHeader(token)
                .idPath(subscriptionId)
                .execute(validatedWith(shouldBe404WithRequestedResourceCouldNotBeFound()));
    }

    @After
    public void deleteSubscriptions() {
        try {
            if (subscriptionId != null && adaptor.isSubscriptionExist(subscriptionId)) {
                adaptor.deleteSubscription(subscriptionId);
            }
        } catch (Exception e) {
            log.info(format("Can't delete subscription %s. Exception: %s", subscriptionId, e.getMessage()));
        }
    }
}
