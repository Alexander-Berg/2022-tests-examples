package ru.auto.tests.realtyapi.v2.usersOffers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiVosOfferCardResponse;
import ru.yandex.qatools.allure.annotations.Title;

import static org.assertj.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.realtyapi.consts.Owners.KERFITD;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;

@Title("GET /user/{uid}/offers/{offer_id}/card")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetUserOfferCardFeaturesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private OAuth oAuth;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    private String token;
    private String uid;
    private String offerId;

    @Before
    public void createUser() {
        Account account = am.create();
        token = oAuth.getToken(account);
        uid = getUid(account);
        offerId = adaptor.createOffer(token).getResponse().getId();
    }


    @Test
    @Owner(KERFITD)
    public void shouldSeeNoFeaturedPartsWhenNotRequested() {
        RealtyApiVosOfferCardResponse response = api.usersOffers().getCardRoute()
                .reqSpec(authSpec())
                .uidPath(uid)
                .offerIdPath(offerId)
                .authorizationHeader(token)
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));

        assertThat(response.getResponse().getContent()).hasId(String.valueOf(offerId));
        assertThat(response.getResponse().getStatistics()).isNull();
    }

    @Test
    @Owner(KERFITD)
    @Issue("REALTYBACK-1105")
    public void shouldReceiveFeaturedPartsWhenRequested() {
        RealtyApiVosOfferCardResponse response = api.usersOffers().getCardRoute()
                .reqSpec(authSpec())
                .uidPath(uid)
                .offerIdPath(offerId)
                .includeFeatureQuery("STATS")
                .authorizationHeader(token)
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));

        assertThat(response.getResponse().getContent()).hasId(String.valueOf(offerId));
        assertThat(response.getResponse().getStatistics().getViews()).isNotNull();
    }

}
