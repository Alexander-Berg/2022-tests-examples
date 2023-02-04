package ru.yandex.realty.adaptor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.hamcrest.Matcher;
import ru.auto.test.api.realty.ApiRealtyBack;
import ru.auto.test.api.realty.SubscriptionCurrency;
import ru.auto.test.api.realty.SubscriptionStatus;
import ru.auto.test.api.realty.service.user.user.subscriptions.EmailDelivery;
import ru.auto.test.api.realty.service.user.user.subscriptions.SubscriptionsReq;
import ru.auto.test.api.realty.service.user.user.subscriptions.responses.SubscriptionsResp;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.util.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.yandex.realty.adaptor.BackRtAdaptor.SubscriptionQualifier.SEARCH;

/**
 * Created by vicdev on 03.07.17.
 */
public class BackRtAdaptor extends AbstractModule {

    @Inject
    private ApiRealtyBack api;

    private static final long DEFAULT_PERIOD = 60;
    private static final String RU = "ru";

    public enum SubscriptionQualifier {
        SEARCH,
        PRICE,
        NEWBUILDING
    }

    public void waitSubscription(String prefix, String uid) {
        waitSubscription(prefix, uid, greaterThanOrEqualTo(1));
    }

    @Step("Ждем подписки у пользователя с {prefix}:{uid}")
    public void waitSubscription(String prefix, String uid, Matcher<Integer> matcher) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .until(() -> Arrays.asList(api.service().withDefaults().user()
                        .withUser(String.format("%s:%s", prefix, uid)).subscriptions()
                        .get(validatedWith(shouldBe200Ok())).as(SubscriptionsResp[].class)).size(), matcher);
    }

    @Step("Создаем подписку для пользователя с {prefix}:{uid}, в разделе {qualifier}, с описанием {body}, " +
            "с заголовком {title}, с почтой {email}")
    public void createSubscriptionWithReq(String prefix, String uid, SubscriptionQualifier qualifier,
                                          String body, String title, String email) {
        int statusCode = api.service().withDefaults().user().withUser(getUser(prefix, uid)).subscriptions()
                .withSubscriptionsReq(
                        getDefaultBody(qualifier).withBody(body).withTitle(title).withEmail(email))
                .post(Function.identity()).getStatusCode();
        assertThat(statusCode, anyOf(equalTo(SC_OK), equalTo(SC_CONFLICT)));
        waitSubscription(prefix, uid);
    }

    @Step("Создаем подписку для пользователя с {prefix}:{uid}, в разделе {qualifier}")
    public void createSubscription(String prefix, String uid, SubscriptionQualifier qualifier) {
        int statusCode = api.service().withDefaults().user().withUser(getUser(prefix, uid)).subscriptions()
                .withSubscriptionsReq(getDefaultBody(qualifier))
                .post(Function.identity()).getStatusCode();
        assertThat(statusCode, anyOf(equalTo(SC_OK), equalTo(SC_CONFLICT)));
        waitSubscription(prefix, uid);
    }

    @Step("Создаем выключенную подписку для пользователя с {prefix}:{uid}, в разделе {qualifier}")
    public void createDisabledSubscription(String prefix, String uid, SubscriptionQualifier qualifier) {
        int statusCode = api.service().withDefaults().user().withUser(getUser(prefix, uid)).subscriptions()
                .withSubscriptionsReq(getDefaultBody(qualifier).withDisabled(true))
                .post(Function.identity()).getStatusCode();
        assertThat(statusCode, anyOf(equalTo(SC_OK), equalTo(SC_CONFLICT)));
        waitSubscription(prefix, uid);
    }

    @Step("Создаём подписку для пользователя с {prefix}:{uid}, у которого email не подтвержён")
    public void createNotConfirmSubscription(String prefix, String uid, String email) {
        api.service().withDefaults().user().withUser(getUser(prefix, uid)).subscriptions()
                .withSubscriptionsReq(getDefaultBody(SEARCH)
                        .withEmail(email)
                        .withEmailDelivery(new EmailDelivery().withPeriod(DEFAULT_PERIOD).withAddress(email))
                        .withState(SubscriptionStatus.AWAIT_CONFIRMATION.value()))
                .post(validatedWith(shouldBe200Ok())).as(SubscriptionsResp.class);
        waitSubscription(prefix, uid);
    }

    @Step("Получаем подписки у пользователя с {prefix}:{uid}")
    public List<SubscriptionsResp> getSubscriptions(String prefix, String uid) {
        return Arrays.asList(api.service().withDefaults().user().withUser(getUser(prefix, uid)).subscriptions()
                .get(validatedWith(shouldBe200Ok())).as(SubscriptionsResp[].class));
    }

    @Step("Удаляем подписку {id} у пользователя с {prefix}:{uid}")
    public void deleteSubscriptions(String id, String prefix, String uid) {
        api.service().withDefaults().user().withUser(getUser(prefix, uid)).subscriptions()
                .id().withId(id).delete(validatedWith(shouldBe200Ok()));
    }

    private String getUser(String prefix, String uid) {
        return String.format("%s:%s", prefix, uid);
    }

    private static SubscriptionsReq getDefaultBody(SubscriptionQualifier qualifier) {
        String email = Utils.getRandomEmail();
        SubscriptionsReq defaultSubscriptionsReq = new SubscriptionsReq()
                .withEmail(email)
                .withPeriod(DEFAULT_PERIOD)
                .withEmailDelivery(new EmailDelivery().withPeriod(DEFAULT_PERIOD).withAddress(email))
                .withTitle(Utils.getRandomString())
                .withTopLevelDomain(RU)
                .withLanguage(RU)
                .withBody(Utils.getRandomString())
                .withCurrency(SubscriptionCurrency.RUR.value())
                .withSendEmailInTesting(false)
                .withSendPushInTesting(false)
                .withState(SubscriptionStatus.ACTIVE.value())
                .withDisabled(false);
        switch (qualifier) {
            case NEWBUILDING:
                return defaultSubscriptionsReq.withHttpQuery("siteId=189856")
                        .withQualifier("proposals")
                        .withFrontEndHttpQuery("/moskva/kupit/novostrojka/oktyabrskoe-pole-189856/?rgid=12439");
            case PRICE:
                return defaultSubscriptionsReq.withHttpQuery("ID=655482741710724833")
                        .withQualifier("price-update")
                        .withFrontEndHttpQuery("/offer/6554827417107248333/");
            case SEARCH:
            default:
                return defaultSubscriptionsReq.withHttpQuery("rgid=587795&type=SELL&category=APARTMENT")
                        .withFrontEndHttpQuery("rgid=587795&type=SELL&category=APARTMENT");
        }
    }

    @Override
    protected void configure() {
    }
}
