package ru.auto.tests.realtyapi.v2.service;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiServiceServiceInfoResponse;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.KERFITD;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("GET /service/info")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class ServiceInfoOffersTest {

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
    @Owner(KERFITD)
    public void shouldSeeOneOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);
        var offerId = adaptor.createOffer(token).getResponse().getId();

        RealtyApiServiceServiceInfoResponse response = api.service().getServiceInfo().reqSpec(authSpec())
                .authorizationHeader(token)
                .xUidHeader(getUid(account))
                .includeFeatureQuery("OFFERS")
                .executeAs(validatedWith(shouldBe200Ok()));

        assertThat(response.getResponse().getOffers().getOffers().size(), is(1));
        assertThat(response.getResponse().getOffers().getOffers().get(0).getVosUnifiedOffer().getContent().getId(), is(offerId));
        assertThat(response.getResponse().getOffers().getOffers().get(0).getViewCounts().size(), is(30));
    }
}
