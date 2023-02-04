package ru.auto.tests.realtyapi.v2.personalAccount;

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
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserContacts;
import ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserUpdate;
import ru.auto.tests.realtyapi.v2.model.RealtyPhone;
import ru.auto.tests.realtyapi.v2.testdata.UserTestData;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("PUT /user/{uid}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class UpdateUserPhoneTest {

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
    @Owner(ARTEAMO)
    public void shouldUpdateUserPhone() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String uid = getUid(account);
        String randomPhone = getRandomString();

        api.personalAccountUsers().updateUserRoute().reqSpec(authSpec())
                .body(getBody(randomPhone))
                .uidPath(uid)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()));

        String phone = api.personalAccountUsers().getUserRoute().reqSpec(authSpec())
                .uidPath(uid)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("userContacts")
                .getAsJsonArray("phones").get(0).getAsJsonObject()
                .get("wholePhoneNumber").getAsString();

        Assertions.assertThat(phone).describedAs("У пользователя должен измениться подменник")
                .isEqualTo(randomPhone);
    }

    private static RealtyApiUsersUserUpdate getBody(String randomPhone) {
        return UserTestData.getUpdate().userContacts(
                new RealtyApiUsersUserContacts().addPhonesItem(new RealtyPhone().wholePhoneNumber(randomPhone)));
    }
}
