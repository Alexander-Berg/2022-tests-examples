package ru.yandex.realty.back;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.test.api.realty.ApiRealtyBack;
import ru.auto.test.api.realty.Domain;
import ru.auto.test.api.realty.SubscriptionCurrency;
import ru.auto.test.api.realty.SubscriptionStatus;
import ru.auto.test.api.realty.service.user.user.subscriptions.EmailDelivery;
import ru.auto.test.api.realty.service.user.user.subscriptions.SubscriptionsReq;
import ru.auto.test.api.realty.service.user.user.subscriptions.responses.SubscriptionsResp;
import ru.auto.test.api.realty.service.user.user.subscriptions.responses.SubscriptionsRespAssert;
import ru.auto.test.api.realty.service.user.user.subscriptions.responses.ViewAssert;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.module.RealtyApiModule;

import java.net.URISyntaxException;
import java.util.Arrays;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;

/**
 * Created by vicdev on 07.07.17.
 */
@DisplayName("Создание подписки - проверка полей")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class CreateSubscriptionsTest {

    @Inject
    public ApiRealtyBack api;

    @Inject
    public Account account;

    @Test
    @DisplayName("Создание подписки")
    public void shouldCreateSubscription() throws URISyntaxException {
        String user = getUser("uid", account.getId());
        String httpQuery = getHttpQuery(account.getId());
        String frontEndHttpQuery = Utils.getRandomString();
        String body = Utils.getRandomString();
        String currency = SubscriptionCurrency.RUR.value();
        String state = SubscriptionStatus.ACTIVE.value();
        String title = Utils.getRandomString();
        String domain = Domain.RU.value();
        long period = getRandomShortInt();
        String email = Utils.getRandomEmail();
        SubscriptionsReq subscriptionsReq = new SubscriptionsReq()
                .withHttpQuery(httpQuery)
                .withEmail(email)
                .withPeriod(period)
                .withEmailDelivery(new EmailDelivery().withPeriod(period).withAddress(email))
                .withTitle(title)
                .withTopLevelDomain(domain)
                .withLanguage(domain)
                .withBody(body)
                .withCurrency(currency)
                .withFrontEndHttpQuery(frontEndHttpQuery)
                .withSendEmailInTesting(false)
                .withSendPushInTesting(false)
                .withState(state)
                .withDisabled(false);

        api.service().withDefaults().user().withUser(user).subscriptions().withSubscriptionsReq(subscriptionsReq)
                .post(validatedWith(shouldBe200Ok())).getBody().prettyPeek();

        SubscriptionsResp resp = Arrays.asList(api.service().withDefaults().user().withUser(user).subscriptions()
                .get(validatedWith(shouldBe200Ok())).as(SubscriptionsResp[].class)).get(0);

        SubscriptionsRespAssert.assertThat(resp).hasState(SubscriptionStatus.ACTIVE.value()).hasDisabled(false);

        ViewAssert.assertThat(resp.getView()).hasCurrency(currency)
                .hasBody(body).hasFrontEndHttpQuery(frontEndHttpQuery).hasLanguage(domain)
                .hasTitle(title).hasTopLevelDomain(domain);
    }

    private static String getUser(String prefix, String uid) {
        return String.format("%s:%s", prefix, uid);
    }

    private static String getHttpQuery(String uid) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder("")
                .addParameter("rgid", valueOf(getRandomShortInt()))
                .addParameter("type", ru.auto.test.api.realty.VosOfferType.SELL.value())
                .addParameter("category", ru.auto.test.api.realty.VosOfferCategory.APARTMENT.value())
                .addParameter("showUniquePoints", "NO")
                .addParameter("pageSize", "20")
                .addParameter("minTrust", "NORMAL")
                .addParameter("searchType", "search")
                .addParameter("login", uid);
        return uriBuilder.build().getQuery();
    }
}
