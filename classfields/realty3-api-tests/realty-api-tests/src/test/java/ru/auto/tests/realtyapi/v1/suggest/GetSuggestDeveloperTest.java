package ru.auto.tests.realtyapi.v1.suggest;

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
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("GET /suggest/developer")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetSuggestDeveloperTest {

    private static final int INVALID_MAX_COUNT = 0;

    private static final int MAX_COUNT = 3;
    private static final int VALID_RGID = 741964;
    private static final String VALID_TEXT = "а";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;
    
    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.suggest().suggestDeveloperRoute()
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
    
    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithNoParameters() {
        api.suggest().suggestDeveloperRoute().reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
    
    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidMaxCount() {
        api.suggest().suggestDeveloperRoute().reqSpec(authSpec())
                .textQuery(getRandomString())
                .rgidQuery(getRandomShortInt())
                .maxCountQuery(INVALID_MAX_COUNT)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
    
    @Test
    @Owner(ARTEAMO)
    public void shouldSuggestHasRequestedListSize() {
        JsonObject response = api.suggest().suggestDeveloperRoute().reqSpec(authSpec())
                .textQuery(VALID_TEXT)
                .rgidQuery(VALID_RGID)
                .maxCountQuery(MAX_COUNT)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.getAsJsonArray("result"))
                .describedAs(format("Ответ должен содержать список заданного размера %s", MAX_COUNT))
                .hasSize(MAX_COUNT);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSuggestIsEmpty() {
        JsonObject response = api.suggest().suggestDeveloperRoute().reqSpec(authSpec())
                .textQuery(getRandomString())
                .rgidQuery(VALID_RGID)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.getAsJsonArray("result"))
                .describedAs("Ответ должен содержать пустой список")
                .isEmpty();
    }
}
