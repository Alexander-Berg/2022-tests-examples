package ru.auto.tests.realtyapi.v1.stat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.StatsOffersRequest;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.Instant;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomOfferId;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getYesterdayDate;


@Title("POST /stat/shows/offers/total")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class StatShowsOffersTotalTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    public void shouldSee403WithoutHeaders() {
        api.stat().offersTotalShows()
                .spanQuery(getRandomShortInt())
                .startTimeQuery(getYesterdayDate())
                .endTimeQuery(Instant.now().toString())
                .body(new StatsOffersRequest().addOfferIdsItem(getRandomOfferId()).addOfferIdsItem(getRandomOfferId()))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee200ForNotExistOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token).getResponse().getId();

        api.stat().offersTotalShows()
                .reqSpec(authSpec()).xAuthorizationHeader(token)
                .spanQuery(getRandomShortInt())
                .startTimeQuery(getYesterdayDate())
                .endTimeQuery(Instant.now().toString())
                .body(new StatsOffersRequest().addOfferIdsItem(offerId).addOfferIdsItem(getRandomOfferId()))
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String firstOfferId = adaptor.createOffer(token).getResponse().getId();
        String secondOfferId = adaptor.createOffer(token).getResponse().getId();

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.stat().offersTotalShows()
                .reqSpec(authSpec()).xAuthorizationHeader(token)
                .spanQuery(getRandomShortInt())
                .startTimeQuery(getYesterdayDate())
                .endTimeQuery(Instant.now().toString())
                .body(new StatsOffersRequest().addOfferIdsItem(firstOfferId).addOfferIdsItem(secondOfferId))
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
