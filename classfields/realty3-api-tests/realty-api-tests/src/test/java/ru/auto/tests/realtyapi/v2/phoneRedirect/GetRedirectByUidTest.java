package ru.auto.tests.realtyapi.v2.phoneRedirect;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.Assertions;
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
import ru.auto.tests.realtyapi.v2.model.RealtyApiPhoneRedirectStatusResponse;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("GET /phone/redirect/user/{uid}")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetRedirectByUidTest {

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
        api.phoneRedirect().getRedirectStatusRoute()
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404WithInvalidUid() {
        api.phoneRedirect().getRedirectStatusRoute().reqSpec(authSpec())
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldTurnOnRedirect() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);
        adaptor.createOffer(token);

        api.phoneRedirect().turnOnRedirectRoute()
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .uidPath(getUid(account))
                .execute(validatedWith(shouldBe200Ok()));

        RealtyApiPhoneRedirectStatusResponse response = api.phoneRedirect().getRedirectStatusRoute()
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .uidPath(getUid(account))
                .executeAs(validatedWith(shouldBe200Ok()));

        Assertions.assertThat(response.getResponse()).hasRedirectPhones(true);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldTurnOffRedirect() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);
        adaptor.createOffer(token);

        api.phoneRedirect().turnOnRedirectRoute()
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .uidPath(getUid(account))
                .execute(validatedWith(shouldBe200Ok()));

        api.phoneRedirect().turnOffRedirectRoute()
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .uidPath(getUid(account))
                .execute(validatedWith(shouldBe200Ok()));

        RealtyApiPhoneRedirectStatusResponse response = api.phoneRedirect().getRedirectStatusRoute()
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .uidPath(getUid(account))
                .executeAs(validatedWith(shouldBe200Ok()));

        Assertions.assertThat(response.getResponse()).hasRedirectPhones(false);
    }
}
