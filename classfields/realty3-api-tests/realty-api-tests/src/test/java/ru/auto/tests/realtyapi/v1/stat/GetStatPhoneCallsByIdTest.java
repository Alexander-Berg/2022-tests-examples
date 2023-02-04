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
import static ru.auto.tests.realtyapi.v1.stat.GetStatShowsAggregatedUserTest.ALL;


@Title("GET /stat/phoneCalls/{offerId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetStatPhoneCallsByIdTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldSee403WithoutHeaders() {
        int span = getRandomShortInt();

        api.stat().thisUserOfferPhoneCalls()
                .offerIdPath(getRandomOfferId())
                .levelQuery(ALL)
                .spanQuery(span)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = getRandomOfferId();
        int span = getRandomShortInt();

        api.stat().thisUserOfferPhoneCalls()
                .reqSpec(authSpec()).xAuthorizationHeader(token)
                .offerIdPath(offerId)
                .levelQuery(ALL)
                .spanQuery(span)
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Test
    public void shouldStatByOfferIdWithStartAndEndTimeHasNoDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token).getResponse().getId();
        String startTime = getYesterdayDate();
        String endTime = Instant.now().toString();

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.stat().thisUserOfferPhoneCalls()
                .reqSpec(authSpec()).xAuthorizationHeader(token)
                .offerIdPath(offerId)
                .levelQuery(ALL)
                .startTimeQuery(startTime)
                .endTimeQuery(endTime)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi))
                .whenIgnoringPaths("response.values[*].date"));
    }
}
