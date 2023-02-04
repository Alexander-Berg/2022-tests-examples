package ru.auto.tests.publicapi.compare;

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
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiCatalogCardListingResponse;
import ru.auto.tests.publicapi.model.AutoApiModelCompareData;
import ru.auto.tests.publicapi.model.AutoApiModelsCompareRequest;
import ru.auto.tests.publicapi.model.AutoApiModelsCompareResponse;
import ru.auto.tests.publicapi.model.AutoApiSearchCatalogFilter;
import ru.auto.tests.publicapi.model.AutoApiOfferCompareData;
import ru.auto.tests.publicapi.model.AutoApiOffersCompareResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 22.09.17.
 */

@DisplayName("GET /user/compare/{category}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class CompareListTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private Account account;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee200WithIncorrectCatalogId() {
        String sessionId = adaptor.login(account).getSession().getId();
        api.userCompare().cards().categoryPath(CARS.name()).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldSee400WithIncorrectCategory() {
        String incorrectCategory = Utils.getRandomString();
        String sessionId = adaptor.login(account).getSession().getId();
        api.userCompare().cards()
                .categoryPath(incorrectCategory)
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    public void shouldSeeEmptyList() {
        String sessionId = adaptor.login(account).getSession().getId();
        api.userCompare().cards().categoryPath(CARS.name()).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldSeeCompareList() {
        String sessionId = adaptor.login(account).getSession().getId();
        String catalogCardId = adaptor.addToCompare(account.getLogin(), sessionId);
        String offerId = adaptor.addOfferToCompare(account.getLogin(), sessionId);
        String favoriteId = "favorite-" + offerId;

        AutoApiCatalogCardListingResponse response = api.userCompare().cards().categoryPath(CARS.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(response).hasOnlyCatalogCardIds(catalogCardId, favoriteId);
    }

    @Test
    public void shouldCompareOffersForUser() {
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.addToCompare(account.getLogin(), sessionId);
        String offerId = adaptor.addOfferToCompare(account.getLogin(), sessionId);

        AutoApiOffersCompareResponse response = api.userCompare().getCompareOffers().categoryPath(CARS.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBe200OkJSON()));

        MatcherAssert.assertThat(response.getOffers().size(), equalTo(1));
        AutoApiOfferCompareData model = response.getOffers().get(0);
        MatcherAssert.assertThat(model.getSummary().getId(), equalTo(offerId));
    }

    @Test
    public void shouldCompareModelsForUser() {
        String sessionId = adaptor.login(account).getSession().getId();
        String catalogCardId = adaptor.addToCompare(account.getLogin(), sessionId);
        adaptor.addOfferToCompare(account.getLogin(), sessionId);

        AutoApiModelsCompareResponse response = api.userCompare().getCompareModels().categoryPath(CARS.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBe200OkJSON()));

        MatcherAssert.assertThat(response.getModels().size(), equalTo(1));
        AutoApiModelCompareData model = response.getModels().get(0);
        MatcherAssert.assertThat(model.getSummary().getId(), equalTo(catalogCardId));
    }

    @Test
    public void shouldUpdateCompareModels() {
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.addToCompare(account.getLogin(), sessionId);

        AutoApiModelsCompareResponse response = api.userCompare().updateCompareModels().body(new AutoApiModelsCompareRequest().addDataItem(new AutoApiSearchCatalogFilter().mark("BMW").model("5ER"))).categoryPath(CARS.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBe200OkJSON()));

        MatcherAssert.assertThat(response.getModels().size(), equalTo(1));
        AutoApiModelCompareData model = response.getModels().get(0);
        MatcherAssert.assertThat(model.getSummary().getMark().getId(), equalTo("BMW"));

        String configurationId = model.getSummary().getConfiguration().getId();
        String techParamId = model.getSummary().getTechparam().getId();
        String complectationId = model.getSummary().getComplectation().getId();
        String catalogCardId = configurationId + "_" + complectationId + "_" + techParamId;
        MatcherAssert.assertThat(model.getSummary().getId(), equalTo(catalogCardId));

        AutoApiCatalogCardListingResponse userCompareList = adaptor.getCompareList(sessionId);
        assertThat(userCompareList).hasOnlyCatalogCardIds(catalogCardId);
    }

    @Test
    public void shouldCompareModelsList() {
        String sessionId = adaptor.login(account).getSession().getId();
        String catalogCardId = adaptor.addToCompare(account.getLogin(), sessionId);

        AutoApiModelsCompareResponse response = api.userCompare().compareModelsList().body(new AutoApiModelsCompareRequest().addDataItem(new AutoApiSearchCatalogFilter().mark("BMW").model("5ER"))).categoryPath(CARS.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBe200OkJSON()));

        MatcherAssert.assertThat(response.getModels().size(), equalTo(1));
        AutoApiModelCompareData model = response.getModels().get(0);
        MatcherAssert.assertThat(model.getSummary().getMark().getId(), equalTo("BMW"));
        MatcherAssert.assertThat(model.getSummary().getModel().getId(), equalTo("5ER"));

        AutoApiCatalogCardListingResponse userCompareList = adaptor.getCompareList(sessionId);
        assertThat(userCompareList).hasOnlyCatalogCardIds(catalogCardId);
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.addToCompare(account.getLogin(), sessionId);

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.userCompare().cards().categoryPath(CARS.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
