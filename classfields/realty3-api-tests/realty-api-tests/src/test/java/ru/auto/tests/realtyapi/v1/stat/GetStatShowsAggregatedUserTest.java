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
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomOfferId;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomUID;
import static ru.auto.tests.realtyapi.v1.model.Payload.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.Payload.OfferTypeEnum.SELL;


@Title("GET /stat/shows/aggregated/user/{uid}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetStatShowsAggregatedUserTest {

    public static final String ALL = "all";


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WithoutHeaders() {
        int span = getRandomShortInt();

        api.stat().userAggregatedShows()
                .uidPath(getRandomUID())
                .offerTypeQuery(SELL)
                .categoryQuery(APARTMENT)
                .levelQuery(ALL)
                .spanQuery(span)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee200ForNotExistUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        int span = getRandomShortInt();

        api.stat().userAggregatedShows()
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .uidPath(getRandomOfferId())
                .offerTypeQuery(SELL)
                .categoryQuery(APARTMENT)
                .levelQuery(ALL)
                .spanQuery(span)
                .execute(validatedWith(shouldBe200OkJSON()));
    }
}
