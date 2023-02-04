package ru.auto.tests.realtyapi.v1.user;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.FtlProcessor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.ResponseSpecBuilders;
import ru.auto.tests.realtyapi.v1.common.ErrorsDescriptionCompareTest;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.*;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("PATCH /user")
@Description("Инициализация пользователя в VOS")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class EditUserInvalidPatchBodyTest {
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

    @Parameter
    @Parameterized.Parameter(0)
    public String body;

    @Parameterized.Parameters(name = "body={0}")
    public static Object[] getParameters() throws IOException {
        InputStream dataStream = ErrorsDescriptionCompareTest.class.getClassLoader()
                .getResourceAsStream("testdata/patch_user.txt");

        return IOUtils.readLines(dataStream, UTF_8).toArray();
    }

    @Test
    public void shouldSee200AndHasNoDiffWithProd() {
        Account account = am.create();
        String token = oAuth.getToken(account);


        JsonObject user = prodApi.user().getUserRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .execute(ResponseSpecBuilders.validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        String phoneId = user
                .getAsJsonObject("response")
                .getAsJsonObject("user")
                .getAsJsonArray("phones")
                .get(0).getAsJsonObject().get("id").getAsString();

        String fullBody = getFullBody(account, phoneId);
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.user().patchUserRoute().reqSpec(authSpec())
                .reqSpec(req -> req.setBody(fullBody))
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    private String getFullBody(Account account, String phoneId) {
        if (body.contains("%s")) {
            return String.format(body,  phoneId, account.getPhone().orElse(""));
        } else {
            return body;
        }
    }
}
