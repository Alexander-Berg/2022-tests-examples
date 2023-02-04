package ru.auto.tests.publicapi.catalog;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponseAssert;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.DICTIONARY_FORMAT_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.DICTIONARY_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withDefaultDictionaryPaths;


@DisplayName("GET /reference/catalog/{category}/dictionaries/{format}/{dictionary}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetDictionaryTest {

    private static final String DEFAULT_FORMAT = "v1";
    private static final String DEFAULT_DICTIONARY = "body_type";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.catalog().dictionary().reqSpec(withDefaultDictionaryPaths()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404WithInvalidFormat() {
        String invalidFormat = Utils.getRandomString();
        AutoApiErrorResponse response = api.catalog().dictionary().formatPath(invalidFormat)
                .categoryPath(CARS).dictionaryPath(DEFAULT_DICTIONARY).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND))).as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response).hasStatus(ERROR)
                .hasError(DICTIONARY_FORMAT_NOT_FOUND)
                .hasDetailedError(DICTIONARY_FORMAT_NOT_FOUND.name());
    }

    @Test
    public void shouldSee404WithInvalidCategory() {
        String invalidCategory = Utils.getRandomString();
        AutoApiErrorResponse response = api.catalog().dictionary().formatPath(DEFAULT_FORMAT)
                .categoryPath(invalidCategory).dictionaryPath(DEFAULT_DICTIONARY).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND))).as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response).hasStatus(ERROR)
                .hasError(BAD_REQUEST);
    }

    @Test
    public void shouldSee404WithInvalidDictionary() {
        String invalidDictionary = Utils.getRandomString();
        AutoApiErrorResponse response = api.catalog().dictionary().formatPath(DEFAULT_FORMAT)
                .categoryPath(CARS).dictionaryPath(invalidDictionary).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND))).as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response).hasStatus(ERROR)
                .hasError(DICTIONARY_NOT_FOUND)
                .hasDetailedError(DICTIONARY_NOT_FOUND.name());
    }
}
