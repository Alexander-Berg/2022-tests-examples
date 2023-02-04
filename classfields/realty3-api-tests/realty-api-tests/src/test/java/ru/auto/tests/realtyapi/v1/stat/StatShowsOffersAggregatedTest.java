package ru.auto.tests.realtyapi.v1.stat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.StatsOffersRequest;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.Instant;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomOfferId;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getYesterdayDate;
import static ru.auto.tests.realtyapi.v1.stat.GetStatShowsAggregatedUserTest.ALL;


@Title("POST /stat/shows/offers/aggregated")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class StatShowsOffersAggregatedTest {

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

    @Test
    public void shouldSee403WithoutHeaders() {
        api.stat().offersAggregatedShows()
                .levelQuery(ALL)
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

        api.stat().offersAggregatedShows()
                .reqSpec(authSpec()).xAuthorizationHeader(token)
                .levelQuery(ALL)
                .spanQuery(getRandomShortInt())
                .startTimeQuery(getYesterdayDate())
                .endTimeQuery(Instant.now().toString())
                .body(new StatsOffersRequest().addOfferIdsItem(offerId).addOfferIdsItem(getRandomOfferId()))
                .execute(validatedWith(shouldBe200OkJSON()));
    }
}
