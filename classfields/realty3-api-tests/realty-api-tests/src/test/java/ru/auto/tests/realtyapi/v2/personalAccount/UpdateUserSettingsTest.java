package ru.auto.tests.realtyapi.v2.personalAccount;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserSettings;
import ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserUpdate;
import ru.auto.tests.realtyapi.v2.testdata.UserTestData;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("PUT /user/{uid}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class UpdateUserSettingsTest {

    private String token;
    private String uid;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Before
    public void createUser() {
        Account account = am.create();
        token = oAuth.getToken(account);
        uid = getUid(account);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldDisableRedirects() {
        api.personalAccountUsers().updateUserRoute().reqSpec(authSpec())
                .body(setRedirect(false))
                .uidPath(uid)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()));

        boolean redirect = api.personalAccountUsers().getUserRoute().reqSpec(authSpec())
                .uidPath(uid)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("userSettings")
                .get("redirectPhones").getAsBoolean();

        Assertions.assertThat(redirect).describedAs("У пользователя должны быть выключены подменники")
                .isFalse();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldEnableRedirects() {
        adaptor.vosUser(token);

        prodApi.phoneRedirect().turnOffRedirectRoute().reqSpec(authSpec())
                .uidPath(uid)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()));

        api.personalAccountUsers().updateUserRoute().reqSpec(authSpec())
                .body(setRedirect(true))
                .uidPath(uid)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        boolean redirect = api.personalAccountUsers().getUserRoute().reqSpec(authSpec())
                .uidPath(uid)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("userSettings")
                .get("redirectPhones").getAsBoolean();

        Assertions.assertThat(redirect).describedAs("У пользователя должны быть включены подменники")
                .isTrue();
    }

    private static RealtyApiUsersUserUpdate setRedirect(boolean redirect) {
        return UserTestData.getUpdate().userSettings(new RealtyApiUsersUserSettings().redirectPhones(redirect));
    }
}
