package ru.yandex.realty.goals.phoneallshow.map;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.beans.Goal;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.GoalsConsts.Goal.PHONE_ALL_SHOW;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.MAP;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.MAPSERP_OFFERS_ITEM;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFERS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PER_DAY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.RENT_BY_DAY;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferPhonesResponse.TEST_PHONE;
import static ru.yandex.realty.mock.OfferPhonesResponse.offersPhonesTemplate;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.page.OffersSearchPage.SHOW_PHONE;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «phone.all.show». Карта")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class PhoneShowMapGoalsRentByDayTest {

    private static final String ANY_ACTIVE_POINT_COORDINATES = "60.0%2C30.0";

    private Goal.Params phoneParams;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private ProxySteps proxy;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Before
    public void before() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        MockOffer offer = mockOffer(RENT_BY_DAY);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate().addPhone(TEST_PHONE).build())
                .createWithDefaults();
        basePageSteps.setWindowSize(1400, 1800);
        urlSteps.testing().path(SANKT_PETERBURG).path(SNYAT).path(KVARTIRA).path(KARTA).queryParam("rentTime", "SHORT")
                .queryParam("activePointId", ANY_ACTIVE_POINT_COORDINATES).open();
        basePageSteps.onMapPage().wizardTip().closeButton().clickIf(isDisplayed());
        basePageSteps.clickMapOfferAndShowSnippetOffers();
        proxy.clearHarUntilThereAreNoHarEntries();

        phoneParams = params().offerType(RENT)
                .offerCategory(APARTMENT)
                .hasGoodPrice(FALSE)
                .hasPlan(FALSE)
                .placement(MAPSERP_OFFERS_ITEM)
                .pageType(MAP)
                .pricingPeriod(PER_DAY)
                .offerId(offer.getOfferId())
                .exactMatch(TRUE)
                .hasEgrnReport(FALSE)
                .hasVideoReview(FALSE)
                .hasVirtualTour(FALSE)
                .onlineShowAvailable(FALSE)
                .payed(FALSE)
                .tuz(FALSE)
                .page(OFFERS);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Снять квартиру посуточно. Клик «Показать телефон»")
    public void shouldSeeMapPhoneShowGoal() {

        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).buttonWithTitle(SHOW_PHONE).click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(phoneParams)).shouldExist();
    }
}