package ru.auto.tests.realtyapi.v1.phone;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiPhoneConfirmRequest;
import ru.yandex.qatools.allure.annotations.Title;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe400PhoneBadConfiramtionCode;


@Title("POST /user/check/phone")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class CheckPhoneTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WithoutHeaders() {
        api.passport().phoneConfirm().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithoutOAuth() {
        api.passport().phoneConfirm().reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee400WithInvalidCode() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        String code = String.valueOf(Utils.getRandomShortInt());
        api.passport().phoneConfirm().reqSpec(authSpec())
                .body(this.getPhoneConfirmRequest(code))
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe400PhoneBadConfiramtionCode()));
    }

    private RealtyApiPhoneConfirmRequest getPhoneConfirmRequest(String code) {
        RealtyApiPhoneConfirmRequest r = random(RealtyApiPhoneConfirmRequest.class);
        r.setCode(code);
        return r;
    }

}
