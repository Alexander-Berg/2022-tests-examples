package ru.auto.tests.realtyapi.v1.personalization;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
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

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeAcceptableCodeForMissingPathElement;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("PUT /user/{user}/personalization/moveHiddenOffers")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class PutMoveHiddenOffersTest {

    private static final String ME = "me";

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

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuthTest() {
        api.personalization().moveHiddenClustersRoute()
                .userPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee4xxWithNoUserTest() {
        api.personalization().moveHiddenClustersRoute()
                .reqSpec(authSpec())
                .userPath(EMPTY)
                .execute(validatedWith(shouldBeAcceptableCodeForMissingPathElement()));
    }

    @Test
    @Owner(ARTEAMO)
    public void should400WithNoUserIdTest() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.personalization().moveHiddenClustersRoute().reqSpec(authSpec())
                .userPath(ME)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldMoveHiddenOffersToAnotherUserTest() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        Account toAccount = am.create();
        String toAccountToken = oAuth.getToken(toAccount);

        String offerId = adaptor.getOfferIdFromSearcher();

        api.personalization().hideOffersRoute().reqSpec(authSpec())
                .userPath(ME)
                .offerIdQuery(offerId)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()));

        api.personalization().moveHiddenClustersRoute().reqSpec(authSpec())
                .userPath(ME)
                .toQuery(toAccount.getId())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()));

        JsonObject response = api.personalization().getHiddenCount().reqSpec(authSpec())
                .userPath(ME)
                .authorizationHeader(toAccountToken)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.get("count").getAsInt())
                .describedAs("У пользователя должен быть один спрятанный оффер")
                .isEqualTo(1);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldDeleteMovedOffersTest() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        Account toAccount = am.create();

        String offerId = adaptor.getOfferIdFromSearcher();

        api.personalization().hideOffersRoute().reqSpec(authSpec())
                .userPath(ME)
                .offerIdQuery(offerId)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()));

        api.personalization().moveHiddenClustersRoute().reqSpec(authSpec())
                .userPath(ME)
                .toQuery(toAccount.getId())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()));

        JsonObject response = api.personalization().getHiddenCount().reqSpec(authSpec())
                .userPath(ME)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.get("count").getAsInt())
                .describedAs("У пользователя не должно быть спрятанных офферов")
                .isEqualTo(0);
    }
}
