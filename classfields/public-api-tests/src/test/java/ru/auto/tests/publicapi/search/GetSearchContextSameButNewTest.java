package ru.auto.tests.publicapi.search;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiOfferListingResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.SectionEnum.NEW;
import static ru.auto.tests.publicapi.model.AutoApiOffer.SectionEnum.USED;

@DisplayName("GET /search/{category}/context/same-but-new/{offerId}")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetSearchContextSameButNewTest {
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

    private final String RID = "213";
    private final String RADIUS= "200";
    private final List<String> EXCLUDE_CATALOG_FILTER = Arrays.asList("mark=ZENVO", "mark=CHERRY");
    private final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);


    @Test
    public void ShouldSee403WhenNonAuth(){
        api.search().sameButNewGET().categoryPath(CARS.name().toLowerCase())
                .offerIdPath(adaptor.getRandomCarsOfferFromSearch().getId())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldHasNoDiffWithProductionForOfferWithSameNew() {
        AutoApiOffer usedOffer = getUsedOfferWithSameNewOffersInSearch();
        assertThat("Can`t find used offer with same new offers", usedOffer, not(nullValue()));

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.search().sameButNewGET()
                .reqSpec(defaultSpec())
                .categoryPath(CARS.name().toLowerCase())
                .offerIdPath(usedOffer.getId())
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi))
                .whenIgnoringPaths("offers[*].relevance", "pagination")
        );
    }

    @Test
    public void shouldSee200WithEmptyOffersForOfferWithoutSameNew() {
        AutoApiOffer oldUsedOffer = getOldUsedOffer();

        AutoApiOfferListingResponse resp = api.search().sameButNewGET()
                .reqSpec(defaultSpec())
                .categoryPath(CARS.name().toLowerCase())
                .offerIdPath(oldUsedOffer.getId())
                .executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(resp.getOffers()).isNullOrEmpty();
        assertThat(resp.getPagination().getTotalOffersCount()).isEqualTo(0);
    }

    @Step("Получаем БУ оффер старше 10 лет")
    private AutoApiOffer getOldUsedOffer() {
        AutoApiOfferListingResponse offers = api.search().searchCars()
                .reqSpec(defaultSpec())
                .pageSizeQuery("1")
                .pageQuery("1")
                .excludeCatalogFilterQuery(EXCLUDE_CATALOG_FILTER)
                .yearToQuery(String.valueOf(CURRENT_YEAR - 10))
                .stateQuery(USED)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        return offers.getOffers().get(0);
    }

    @Step("Получаем БУ оффер для которого есть похожие новые в выдаче")
    private AutoApiOffer getUsedOfferWithSameNewOffersInSearch() {
        AutoApiOfferListingResponse offers = api.search().searchCars()
                .reqSpec(defaultSpec())
                .ridQuery(RID)
                .pageSizeQuery("20")
                .pageQuery("1")
                .sortQuery("cr_date-asc")
                .excludeCatalogFilterQuery(EXCLUDE_CATALOG_FILTER)
                .yearFromQuery(String.valueOf(CURRENT_YEAR - 1))
                .stateQuery(USED)
                .geoRadiusQuery(RADIUS)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        for (AutoApiOffer offer: offers.getOffers()) {
            if (getNewCarsCountForOffer(offer) > 0) {
                return offer;
            }
        }

        return null;
    }

    @Step("Получаем кол-во новых офферов для {offer.carInfo.mark} {offer.carInfo.model} {offer.documents.year} года")
    private Integer getNewCarsCountForOffer(AutoApiOffer offer){
        AutoApiOfferListingResponse offers = api.search().searchCars()
                .reqSpec(defaultSpec())
                .ridQuery(RID)
                .geoRadiusQuery(RADIUS)
                .pageSizeQuery("1")
                .pageQuery("1")
                .catalogFilterQuery(
                        String.format("mark=%s,model=%s", offer.getCarInfo().getMark(), offer.getCarInfo().getModel())
                )
                .yearFromQuery(String.valueOf(offer.getDocuments().getYear()))
                .yearToQuery(String.valueOf(offer.getDocuments().getYear()))
                .stateQuery(NEW)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        return offers.getPagination() != null ? offers.getPagination().getTotalOffersCount() : 0;
    }
}
