package ru.auto.tests.realtyapi.v2.usersOffers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.Assertions;
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
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiVosOffersResponse;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.consts.Owners.KERFITD;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("GET /user/{uid}/offers")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetAllOfferSnippetsForUserTest {

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
    public void shouldSee403WithNoAuth() {
        api.usersOffers().getSnippetsRoute()
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404WithInvalidUid() {
        api.usersOffers().getSnippetsRoute().reqSpec(authSpec())
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeEmptyOfferList() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);

        RealtyApiVosOffersResponse offers = api.usersOffers().getSnippetsRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .uidPath(getUid(account))
                .executeAs(validatedWith(shouldBe200Ok()));

        Assertions.assertThat(offers.getResponse().getSlicing()).hasTotal(0);
    }

    @Ignore("REALTYBACK-1418")
    @Test
    @Owner(ARTEAMO)
    public void shouldSeeOneOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);
        adaptor.createOffer(token);

        RealtyApiVosOffersResponse response = api.usersOffers().getSnippetsRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .uidPath(getUid(account))
                .executeAs(validatedWith(shouldBe200Ok()));

        Assertions.assertThat(response.getResponse().getSlicing()).hasTotal(1);
    }

    @Test
    @Owner(KERFITD)
    @Issue("REALTYBACK-1558")
    public void shouldWorkWithPhoneUserRef() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);

        // manually created account
        String userRef = "phone:71110000002";

        // setting price limit to filter out all offers so that REALTYBACK-1418 wouldn't trigger
        // todo: remove price limit after REALTYBACK-1418 is done
        RealtyApiVosOffersResponse offers = api.usersOffers().getSnippetsRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .uidPath(userRef)
                .priceToQuery(1)
                .executeAs(validatedWith(shouldBe200Ok()));

        Assertions.assertThat(offers.getResponse().getSlicing()).hasTotal(0);
    }
}
