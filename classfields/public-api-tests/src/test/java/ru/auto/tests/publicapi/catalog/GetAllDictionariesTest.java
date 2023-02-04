package ru.auto.tests.publicapi.catalog;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponseAssert;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.DICTIONARY_FORMAT_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withDefaultDictionaryFormatPath;


@DisplayName("GET /reference/catalog/all/dictionaries/{format}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetAllDictionariesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.catalog().dictionariesList().reqSpec(withDefaultDictionaryFormatPath()).execute(ResponseSpecBuilders.validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404WithInvalidFormat() {
        String invalidFormat = Utils.getRandomString();
        AutoApiErrorResponse response = api.catalog().dictionariesList().formatPath(invalidFormat).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND))).as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response).hasStatus(ERROR)
                .hasError(DICTIONARY_FORMAT_NOT_FOUND)
                .hasDetailedError(DICTIONARY_FORMAT_NOT_FOUND.name());
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.catalog().dictionariesList()
                .reqSpec(withDefaultDictionaryFormatPath())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
