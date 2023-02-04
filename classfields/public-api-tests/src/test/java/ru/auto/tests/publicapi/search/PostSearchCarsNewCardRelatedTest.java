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
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.utils.GrouppingIdParams;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.*;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.TECH_PARAM_NOT_FOUND;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /search/cars/new-card/related")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class PostSearchCarsNewCardRelatedTest {
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

    @Test
    public void shouldSee403WhenNonAuth() {
        api.search().newCardRelatedPOST().executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404ForInvalidGrouppingId() {
        String invalid_id = "1";
        AutoApiSearchSearchRequestParameters body = new AutoApiSearchSearchRequestParameters().groupingId(
                String.format("tech_param_id=%s,complectation_id=%s", invalid_id, invalid_id)
        );
        AutoApiErrorResponse errorResp = api.search().newCardRelatedPOST()
                .body(body)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(AutoApiErrorResponse.class);

        assertThat(errorResp).hasError(TECH_PARAM_NOT_FOUND);
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        GrouppingIdParams groupingId = adaptor.getActualTechParamAndConfiguration();
        AutoApiSearchSearchRequestParameters body = new AutoApiSearchSearchRequestParameters().groupingId(
                String.format("tech_param_id=%s,complectation_id=%s", groupingId.getTechParamId(), groupingId.getComplectationId())
        );

        Function<ApiClient, JsonObject> apiReq = apiClient -> apiClient.search()
                .newCardRelatedPOST()
                .body(body)
                .pageQuery("1")
                .pageSizeQuery("2")
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(apiReq.apply(api), jsonEquals(apiReq.apply(prodApi)));
    }
}
