package ru.auto.tests.publicapi.tradein;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiGeoPoint;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoTradeInNotifierApiTradeInAvailableRequest;
import ru.auto.tests.publicapi.model.AutoTradeInNotifierApiTradeInAvailableResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /trade-in/is_available")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class PostTradeInIsAvailableTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee403WhenNoAuth() {
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiOffer offer = getOffer(adaptor.createOffer(account.getLogin(), sessionId, CARS).getOffer());

        api.tradeIn().isTradeInAvailable().body(new AutoTradeInNotifierApiTradeInAvailableRequest().offer(offer))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldTradeInBeAvailableFor11162Region() {
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiOffer offer = getOffer(adaptor.createOffer(account.getLogin(), sessionId, CARS).getOffer());

        AutoTradeInNotifierApiTradeInAvailableResponse result = api.tradeIn().isTradeInAvailable().body(
                new AutoTradeInNotifierApiTradeInAvailableRequest().offer(offer)
        ).xSessionIdHeader(sessionId).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(result.getIsAvailable()).isEqualTo(true);
    }

    @Test
    public void shouldTradeInBeAvailableFor11162RegionWithoutCoords() {
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiOffer offer = getOffer(adaptor.createOffer(account.getLogin(), sessionId, CARS).getOffer(), false);

        AutoTradeInNotifierApiTradeInAvailableResponse result = api.tradeIn().isTradeInAvailable().body(
                new AutoTradeInNotifierApiTradeInAvailableRequest().offer(offer)
        ).xSessionIdHeader(sessionId).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(result.getIsAvailable()).isEqualTo(true);
    }

    private AutoApiOffer getOffer(AutoApiOffer offer) {
        return getOffer(offer, true);
    }

    private AutoApiOffer getOffer(AutoApiOffer offer, boolean withCoords) {
        offer.getSeller().getLocation().setGeobaseId(11162L);
        if (withCoords) {
            offer.getSeller()
                .getLocation()
                .setCoord(new AutoApiGeoPoint().latitude(58.586755).longitude(61.530761));
        }
        return offer;
    }
}
