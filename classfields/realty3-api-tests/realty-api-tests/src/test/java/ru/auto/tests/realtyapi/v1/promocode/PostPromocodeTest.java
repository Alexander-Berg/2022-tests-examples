package ru.auto.tests.realtyapi.v1.promocode;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeAcceptableCodeForMissingPathElement;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("POST /user/{uid}/promocode/{promocodeId}/activate")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class PostPromocodeTest {

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
        api.promocode().activatePromocodeRoute()
                .uidPath(getRandomString())
                .promocodeIdPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee404WithInvalidPromocode() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.promocode().activatePromocodeRoute().reqSpec(authSpec())
                .uidPath(account.getId())
                .promocodeIdPath(getRandomString())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee4xxWithNoPromocode() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.promocode().activatePromocodeRoute()
                .reqSpec(authSpec())
                .uidPath(account.getId())
                .promocodeIdPath(EMPTY)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBeAcceptableCodeForMissingPathElement()));
    }
}
