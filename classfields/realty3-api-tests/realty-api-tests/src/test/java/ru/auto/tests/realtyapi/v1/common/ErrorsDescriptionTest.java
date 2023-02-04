package ru.auto.tests.realtyapi.v1.common;

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
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("GET /errorsDescription")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class ErrorsDescriptionTest {

    private static final String DESCRIPTION = "can't find error code %s in bunker";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.common().buildErrorsDescriptionRoute()
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
    
    @Test
    @Owner(ARTEAMO)
    public void shouldSee200AndEmptyDescriptionsWithNoCodes() {
        JsonObject response = api.common().buildErrorsDescriptionRoute().reqSpec(authSpec())
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.getAsJsonArray("descriptions"))
                .describedAs("Массив в ответе не должен содержать элементы")
                .isEmpty();
    }
    
    @Test
    @Owner(ARTEAMO)
    public void shouldSee404WithInvalidCode() {
        String invalidCode = getRandomString();

        JsonObject response = api.common().buildErrorsDescriptionRoute().reqSpec(authSpec())
                .codeQuery(invalidCode)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("error")
                .getAsJsonObject("data");

        Assertions.assertThat(response.get("description").getAsString())
                .isEqualTo(format(DESCRIPTION, invalidCode));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeDescriptionTwiceWithTwoEqualCodes() {
        int code = Utils.getRandomShortInt();
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.common().buildErrorsDescriptionRoute()
                .reqSpec(authSpec())
                .codeQuery(code, code)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        Assertions.assertThat(request.apply(api).getAsJsonObject("response")
                .getAsJsonArray("descriptions")).hasSize(2);
    }
}
