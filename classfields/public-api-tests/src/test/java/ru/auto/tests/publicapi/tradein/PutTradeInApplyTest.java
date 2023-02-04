package ru.auto.tests.publicapi.tradein;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiGeoPoint;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiTradeInInfo;
import ru.auto.tests.publicapi.model.AutoApiTradeInInfoPriceRange;
import ru.auto.tests.publicapi.model.AutoTradeInNotifierApiTradeInApplyRequest;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.publicapi.model.AutoApiTradeInInfo.TradeInTypeEnum.FOR_USED;
import static ru.auto.tests.publicapi.model.AutoApiTradeInInfo.TradeInTypeEnum.FOR_MONEY;
import static ru.auto.tests.publicapi.model.AutoApiTradeInInfo.TradeInTypeEnum.FOR_NEW;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("PUT /trade-in/apply")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PutTradeInApplyTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Parameterized.Parameter
    public AutoApiTradeInInfo.TradeInTypeEnum tradeInType;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<AutoApiTradeInInfo.TradeInTypeEnum> getParameters() {
        return newArrayList(FOR_MONEY, FOR_NEW, FOR_USED);
    }

    @Test
    public void shouldApplyTradeIn() {
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiOffer offer = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOffer();

        api.tradeIn().applyTradeIn().body(
                new AutoTradeInNotifierApiTradeInApplyRequest().offer(getOfferWithTradeInAndCoords(offer))
        ).xSessionIdHeader(sessionId).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBe200OkJSON()));

    }

    private AutoApiOffer getOfferWithTradeInAndCoords(AutoApiOffer offer) {
        offer.getSeller().getLocation().setGeobaseId(11162L);
        offer.getSeller().getLocation().setCoord(new AutoApiGeoPoint().latitude(58.586755).longitude(61.530761));
        offer.tradeInInfo(new AutoApiTradeInInfo().tradeInType(tradeInType).tradeInPriceRange(
                new AutoApiTradeInInfoPriceRange().from(618000L).to(744000L).currency("RUR"))
        );
        return offer;
    }
}
