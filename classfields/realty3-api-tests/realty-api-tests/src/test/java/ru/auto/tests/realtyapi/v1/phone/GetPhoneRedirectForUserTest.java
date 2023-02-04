package ru.auto.tests.realtyapi.v1.phone;

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
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("GET /phone/redirect/{uid}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetPhoneRedirectForUserTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private OAuth oAuth;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.phone().getRedirectStatusRoute()
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee404WithInvalidUid() {
        api.phone().getRedirectStatusRoute().reqSpec(authSpec())
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee404ForNotVosUser() {
        Account account = am.create();

        api.phone().getRedirectStatusRoute().reqSpec(authSpec())
                .uidPath(account.getId())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldNewUserHasNoRedirectPhones() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);

        JsonObject response = api.phone().getRedirectStatusRoute().reqSpec(authSpec())
                .uidPath(account.getId())
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        Assertions.assertThat(response.get("redirectPhones").getAsBoolean())
                .describedAs("У нового пользователя не должно быть подменных номеров")
                .isFalse();
    }
}
