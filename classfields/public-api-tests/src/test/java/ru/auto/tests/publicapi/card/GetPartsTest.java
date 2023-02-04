package ru.auto.tests.publicapi.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import net.javacrumbs.jsonunit.core.internal.Options;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_VALUES;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withDefaultSearchQuery;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /offer/{category}/{offerID}/parts")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetPartsTest {

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
        api.offerCard().relatedAutoParts().categoryPath(CARS)
                .offerIDPath(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldCarPartsHasNoDiffWithProduction() {
        String offerId = prodApi.search().searchCars().pageQuery(1).pageSizeQuery(1).reqSpec(defaultSpec())
                .reqSpec(withDefaultSearchQuery()).executeAs(validatedWith(shouldBeSuccess())).getOffers()
                .get(0).getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.offerCard().relatedAutoParts()
                .categoryPath(CARS).offerIDPath(offerId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)).withOptions(Options.empty()
                .with(IGNORING_VALUES)));
    }
}
