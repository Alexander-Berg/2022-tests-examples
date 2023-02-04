package ru.auto.tests.realtyapi.v1.phone;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiPhoneBindRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyApiPhoneConfirmRequest;
import ru.yandex.qatools.allure.annotations.Title;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeOK;

@Title("POST /user/phone")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class PhoneTest {

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
    public void shouldSee403WithoutHeaders() {
        api.passport().phoneBind().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }



//    @Test
//    public void shouldChangePhone() {
//        Account account = am.create();
//        String token = oAuth.getToken(account);
//        String phone = "+" + Utils.getRandomPhone();
//        api.passport().phoneBind()
//                .body(getPhoneApiBindRequest(phone))
//                .reqSpec(authSpec())
//                .authorizationHeader(token)
//                .execute(validatedWith(shouldBeOK()));
//    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithExistingNumber() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String phone = adaptor.getFullPhoneNumber(account.getId());

        api.passport().phoneBind().reqSpec(authSpec())
                .body(getPhoneApiBindRequest(phone))
                .authorizationHeader(token)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidCode() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String phone = adaptor.getFullPhoneNumber(account.getId());

        api.passport().phoneConfirm().reqSpec(authSpec())
                .body(getConfirmRequest())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    private RealtyApiPhoneBindRequest getPhoneApiBindRequest(String phone) {
        RealtyApiPhoneBindRequest r = random(RealtyApiPhoneBindRequest.class);
        r.setPhone(phone);
        return r;
    }

    private RealtyApiPhoneConfirmRequest getConfirmRequest() {
        RealtyApiPhoneConfirmRequest r = random(RealtyApiPhoneConfirmRequest.class);
        r.setCode(String.valueOf(getRandomShortInt()));
        return r;
    }
}
