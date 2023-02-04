package ru.auto.tests.realtyapi.v1.user;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.restassured.http.ContentType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.VosUserModel;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonPartMatches;
import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.apiAwait;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.model.VosUserModel.PaymentTypeEnum.JURIDICAL_PERSON;
import static ru.auto.tests.realtyapi.v1.model.VosUserModel.PaymentTypeEnum.NATURAL_PERSON;


@Title("PATCH /user")
@GuiceModules(RealtyApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PatchUserTest {

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

    @Parameter
    @Parameterized.Parameter(0)
    public String parameter;

    @Parameter
    @Parameterized.Parameter(1)
    public Object value;

    @Parameterized.Parameters(name = "parameter={0} value={1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"photoUrl", "newPhotoUrl"},
                {"callCenter", true},
                {"redirectPhones", true},
                {"organization", "newOrganization"},
                {"ogrn", "newOgrn"},
                {"agencyId", "newAgencyId"},
                {"url", "newUrl"},
                {"paymentType", JURIDICAL_PERSON.getValue()}
        };
    }

    @Test
    public void shouldEditUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);

        api.user().updateUserRoute().reqSpec(authSpec())
                .uidPath(account.getId())
                .body(getUpdateBody(account))
                .execute(validatedWith(shouldBe200Ok()));

        api.user().patchUserRoute().reqSpec(authSpec())
                .xUidHeader(account.getId())
                .reqSpec(r -> r.setContentType(ContentType.JSON).setBody(getPatchBody(parameter, value)))
                .execute(validatedWith(shouldBe200Ok()));

        apiAwait().atMost(60, SECONDS)
                .until(() -> api.user().getDirectUserRoute().reqSpec(authSpec())
                                .uidPath(account.getId())
                                .execute(validatedWith(shouldBe200Ok()))
                                .as(JsonObject.class, GSON),
                        jsonPartMatches("response.user." + parameter, equalTo(value)));
    }

    static VosUserModel getUpdateBody(Account account) {
        return new VosUserModel()
                .login(account.getId())
                .type(0)
                .email("test@yandex.q")
                .name("nameTest")
                .ogrn("ogrnTest")
                .agencyId("agencyIdTest")
                .callCenter(false)
                .url("urlTest")
                .photoUrl("photoUrlTest")
                .allowedCommunicationChannels(newArrayList("COM_CALLS"))
                .telephones(new ArrayList<>(
                        Collections.singletonList(account.getPhone().get())))
                .redirectPhones(false)
                .organization("organizationTest")
                .paymentType(NATURAL_PERSON);
    }

    static JsonObject getPatchBody(String parameter, Object value) {
        JsonObject body = new JsonObject();

        JsonObject inner = new JsonObject();
        inner.add(parameter, new Gson().toJsonTree(value));

        body.add("user", inner);

        return body;
    }
}