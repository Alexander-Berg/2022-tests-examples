package ru.auto.tests.publicapi.search;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.utils.GrouppingIdParams;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.COMPLECTATION_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.TECH_PARAM_NOT_FOUND;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /search/cars/new-card")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class PostSearchCarsNewCard {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    private final String INVALID_ID = "1";

    @Test
    public void shouldSee403WhenNonAuth() {
        api.search().newCardGet().executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404ForInvalidTechParam() {
        GrouppingIdParams grouppingId = adaptor.getActualTechParamAndConfiguration();
        AutoApiErrorResponse errorResponse = getNewCardResponse(INVALID_ID, grouppingId.getComplectationId(), SC_NOT_FOUND, api)
                .as(AutoApiErrorResponse.class);

        assertThat(errorResponse).hasError(TECH_PARAM_NOT_FOUND);
    }

    @Test
    public void shouldSee404ForInvalidComplectation() {
        GrouppingIdParams grouppingId = adaptor.getActualTechParamAndConfiguration();
        AutoApiErrorResponse errorResponse = getNewCardResponse(grouppingId.getTechParamId(), INVALID_ID, SC_NOT_FOUND, api)
                .as(AutoApiErrorResponse.class);

        assertThat(errorResponse).hasError(COMPLECTATION_NOT_FOUND);
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        GrouppingIdParams grouppingId = adaptor.getActualTechParamAndConfiguration();

        JsonObject actualResponse = getNewCardResponse(grouppingId.getTechParamId(), grouppingId.getComplectationId(), SC_OK, api).as(JsonObject.class, GSON);
        JsonObject expectResponse = getNewCardResponse(grouppingId.getTechParamId(), grouppingId.getComplectationId(), SC_OK, prodApi).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(expectResponse, jsonEquals(actualResponse));
    }

    private Response getNewCardResponse(String techParamId, String complectationId, int expectedStatus, ApiClient api) {
        return api.search().newCardPost()
                .body(new AutoApiSearchSearchRequestParameters()
                        .groupingId(String.format("tech_param_id=%s,complectation_id=%s", techParamId, complectationId))
                )
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(expectedStatus)));
    }
}
