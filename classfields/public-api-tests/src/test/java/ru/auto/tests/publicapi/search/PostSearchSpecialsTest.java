package ru.auto.tests.publicapi.search;

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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("POST /search/specials")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class PostSearchSpecialsTest {

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
        api.search().specsPOST().categoryPath(CARS).body(new AutoApiSearchSearchRequestParameters())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404WithInvalidCategory() {
        String invalidCategory = Utils.getRandomString();
        api.search().specsPOST().categoryPath(invalidCategory).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldSee200WithoutBody() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.search().specsPOST().categoryPath(CARS).pageQuery(1).pageSizeQuery(2)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi))
                .whenIgnoringPaths(
                        "pagination.total_offers_count",
                        "pagination.total_page_count",
                        "offers[*].relevance"
                )
        );
    }
}
