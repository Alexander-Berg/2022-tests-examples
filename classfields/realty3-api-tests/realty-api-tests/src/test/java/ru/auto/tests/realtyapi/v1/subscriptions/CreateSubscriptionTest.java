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
import ru.auto.tests.realtyapi.responses.SubscriptionResponse;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.SubscriptionRequest;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe500WithUnrecognizedToken;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomIntForSubscription;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getValidSubscriptionRequest;


@Title("POST /user/subscriptions")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
@Log4j
@Ignore("HTTP status 409")
public class CreateSubscriptionTest {

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

    private SubscriptionResponse resp;

    @Test
    public void shouldSee409WithTwiceCreateSubscription() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        SubscriptionRequest reqBody = getValidSubscriptionRequest();

        resp = api.subscriptions().createSubscriptionRoute().reqSpec(authSpec()).authorizationHeader(token)
                .body(reqBody)
                .execute(validatedWith(shouldBe200OkJSON())).as(SubscriptionResponse.class, GSON);

        adaptor.getSubscriptionById(resp.getResponse().getId());

        api.subscriptions().createSubscriptionRoute().reqSpec(authSpec()).authorizationHeader(token)
                .body(reqBody)
                .execute(validatedWith(shouldBeCode(SC_CONFLICT)
                        .expectBody("error.codename", equalTo("SUBSCRIPTION_EXISTS"))));
    }

    @Test
    public void shouldSee200WithInvalidPeriodWithoutOAuth() {
        resp = api.subscriptions().createSubscriptionRoute().reqSpec(authSpec())
                .body(getValidSubscriptionRequest().period(-1 * getRandomIntForSubscription()))
                .execute(validatedWith(shouldBe200OkJSON())).as(SubscriptionResponse.class, GSON);
    }

    @Test
    public void shouldSee500WithInvalidOfferTypeWithoutOAuth() {
        SubscriptionRequest reqBody = getValidSubscriptionRequest();
        reqBody.getQuery().setType(getRandomString());

        api.subscriptions().createSubscriptionRoute().reqSpec(authSpec())
                .body(reqBody)
                .execute(validatedWith(shouldBe500WithUnrecognizedToken()));
    }

    @Test
    public void shouldSee500WithInvalidCategoryWithoutOAuth() {
        SubscriptionRequest reqBody = getValidSubscriptionRequest();
        reqBody.getQuery().setCategory(getRandomString());

        api.subscriptions().createSubscriptionRoute().reqSpec(authSpec())
                .body(reqBody)
                .execute(validatedWith(shouldBe500WithUnrecognizedToken()));
    }

    @Test
    public void shouldSee200WithInvalidRgidWithoutOAuth() {
        SubscriptionRequest reqBody = getValidSubscriptionRequest();
        reqBody.getQuery().setRgid((long) (-1 * getRandomIntForSubscription()));

        resp = api.subscriptions().createSubscriptionRoute().reqSpec(authSpec())
                .body(reqBody)
                .execute(validatedWith(shouldBe200OkJSON())).as(SubscriptionResponse.class, GSON);
    }

    @Test
    public void shouldSee500WithInvalidOfferType() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        SubscriptionRequest reqBody = getValidSubscriptionRequest();
        reqBody.getQuery().setType(getRandomString());

        api.subscriptions().createSubscriptionRoute().reqSpec(authSpec()).authorizationHeader(token)
                .body(reqBody)
                .execute(validatedWith(shouldBe500WithUnrecognizedToken()));
    }

    @Test
    public void shouldSee500WithInvalidCategory() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        SubscriptionRequest reqBody = getValidSubscriptionRequest();
        reqBody.getQuery().setCategory(getRandomString());

        api.subscriptions().createSubscriptionRoute().reqSpec(authSpec()).authorizationHeader(token)
                .body(reqBody)
                .execute(validatedWith(shouldBe500WithUnrecognizedToken()));
    }

    @Test
    public void shouldSee200WithInvalidRgid() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        SubscriptionRequest reqBody = getValidSubscriptionRequest();
        reqBody.getQuery().setRgid((long) (-1 * getRandomIntForSubscription()));

        resp = api.subscriptions().createSubscriptionRoute().reqSpec(authSpec()).authorizationHeader(token)
                .body(reqBody)
                .execute(validatedWith(shouldBe200OkJSON())).as(SubscriptionResponse.class, GSON);
    }

    @Test
    public void shouldSee200WithInvalidPeriod() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        resp = api.subscriptions().createSubscriptionRoute().reqSpec(authSpec()).authorizationHeader(token)
                .body(getValidSubscriptionRequest().period(-1 * getRandomIntForSubscription()))
                .execute(validatedWith(shouldBe200OkJSON())).as(SubscriptionResponse.class, GSON);
    }

    @After
    public void deleteSubscriptions() {
        try {
            if (resp != null) {
                adaptor.deleteSubscription(resp.getResponse().getId());
            }
        } catch (Exception e) {
            log.info(format("Can't delete subscription %s. Exception: %s", resp.getResponse().getId(), e.getMessage()));
        }
    }
}
