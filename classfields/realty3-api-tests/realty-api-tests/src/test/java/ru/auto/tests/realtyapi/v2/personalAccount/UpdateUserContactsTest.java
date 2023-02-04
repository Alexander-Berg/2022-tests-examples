package ru.auto.tests.realtyapi.v2.personalAccount;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v2.testdata.UserTestData.getUserInfo;

@Title("PUT /user/{uid}")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UpdateUserContactsTest {

    private static String randomValue = getRandomString();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Parameter
    @Parameterized.Parameter
    public String contact;

    @Parameter
    @Parameterized.Parameter(1)
    public String value;

    @Parameterized.Parameters(name = "contact={0} value={1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"name", getRandomString()},
                {"email", getRandomEmail()},
                {"organization", getRandomString()},
                {"ogrn", getRandomString()},
                {"agencyId", getRandomString()},
                {"url", getRandomString()},
                {"photoUrl", getRandomString()}
        };
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUpdateParameter() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String uid = getUid(account);

        api.personalAccountUsers().updateUserRoute().reqSpec(authSpec()
                .andThen(r -> r.setBody(getBody(contact, value))))
                .uidPath(uid)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()));

        String updatedValue = api.personalAccountUsers().getUserRoute().reqSpec(authSpec())
                .uidPath(uid)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("userContacts")
                .get(contact).getAsString();

        Assertions.assertThat(updatedValue).describedAs(format("У пользователя должно смениться: %s", contact))
                .isEqualTo(value);
    }

    private static JsonObject getBody(String key, String value) {
        Gson gson = new Gson();
        JsonObject body = new JsonObject();
        JsonObject contacts = new JsonObject();
        contacts.add(key, gson.toJsonTree(value));
        body.add("userInfo", gson.toJsonTree(getUserInfo()));
        body.add("userContacts", contacts);
        return body;
    }
}
