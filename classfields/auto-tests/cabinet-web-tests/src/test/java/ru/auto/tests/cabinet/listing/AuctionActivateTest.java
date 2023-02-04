package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mock.MockUserOffers;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUCTION_IN_USED;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.BID_SUCCESSFULY_UPDATED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.cabinet.AuctionPopup.APPLY;
import static ru.auto.tests.desktop.element.cabinet.AuctionPopup.FOR_CALL;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.car;
import static ru.auto.tests.desktop.mock.MockUserOffer.getQuery;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_OFFER;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.beans.offer.auction.Auction.auction;
import static ru.auto.tests.desktop.mock.beans.offer.auction.CurrentState.currentState;
import static ru.auto.tests.desktop.mock.beans.offer.auction.Segment.segment;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AUCTION_IN_USED)
@Story("Подключаем аукцион на оффер который не в аукционе")
@DisplayName("Подключаем аукцион на оффер который не в аукционе")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AuctionActivateTest {

    private static final String ID = "1114782187-3302e085";
    private static final String IN_AUCTION_TEMPLATE = "В аукционе\n%s за звонок\n≈ 35%% интереса";

    private final int basePrice = getRandomShortInt();
    private final int step = basePrice * 50;
    private final int minBid = step * 4;
    private final int maxBid = minBid * 2;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/DealerCampaigns"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DesktopClientsGetAristos"),
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        currentState()
                                                                                .setBasePrice(basePrice * 100)
                                                                                .setMinBid(minBid * 100)
                                                                                .setMaxBid(maxBid * 100)
                                                                                .setOneStep(step * 100))
                                                                .setSegments(asList(
                                                                        segment(35).setMaxBid(maxBid * 100)
                                                                                .setCurrent(true),
                                                                        segment(25),
                                                                        segment(20),
                                                                        segment(15),
                                                                        segment(5)))
                                                )).build())
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Активируем аукцион с минимальной ставкой")
    public void shouldActivateAuctionWithMinimumBid() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(basePrice)));
        steps.onCabinetOffersPage().auctionPopup().increaseBid().waitUntil(hasText(formatPrice(minBid))).click();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(minBid)));

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/place-bid", DEALER_AUCTION_OFFER, ID))
                        .withRequestBody(getRequestBidBody(minBid))
                        .withStatusSuccessResponse()
        ).update();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).click();

        steps.onCabinetOffersPage().notifier(BID_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format(IN_AUCTION_TEMPLATE, formatPrice(minBid))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Активируем аукцион с минимальной ставкой + 1 шаг")
    public void shouldActivateAuctionWithMinimumBidPlusStep() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().increaseBid().click();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(minBid)));
        steps.onCabinetOffersPage().auctionPopup().increaseBid().waitUntil(hasText(formatPrice(step))).click();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(step + minBid)));

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/place-bid", DEALER_AUCTION_OFFER, ID))
                        .withRequestBody(getRequestBidBody(step + minBid))
                        .withStatusSuccessResponse()
        ).update();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).click();

        steps.onCabinetOffersPage().notifier(BID_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format(IN_AUCTION_TEMPLATE, formatPrice(step + minBid))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Активируем аукцион с максимальной ставкой")
    public void shouldActivateAuctionWithMaximumBid() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().maxBid().waitUntil(hasText(formatPrice(maxBid))).click();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(maxBid)));

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/place-bid", DEALER_AUCTION_OFFER, ID))
                        .withRequestBody(getRequestBidBody(maxBid))
                        .withStatusSuccessResponse()
        ).update();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).click();

        steps.onCabinetOffersPage().notifier(BID_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format(IN_AUCTION_TEMPLATE, formatPrice(maxBid))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Активируем аукцион с максимальной ставкой - 1 шаг")
    public void shouldActivateAuctionWithMaximumBidMinusStep() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().maxBid().click();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(maxBid)));
        steps.onCabinetOffersPage().auctionPopup().decreaseBid().waitUntil(hasText(formatPrice(step))).click();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(maxBid - step)));

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/place-bid", DEALER_AUCTION_OFFER, ID))
                        .withRequestBody(getRequestBidBody(maxBid - step))
                        .withStatusSuccessResponse()
        ).update();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).click();

        steps.onCabinetOffersPage().notifier(BID_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format(IN_AUCTION_TEMPLATE, formatPrice(maxBid - step))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Активируем аукцион с введённой ставкой")
    public void shouldActivateAuctionSetCustomBid() {
        int customBid = minBid + step * 2;

        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL, String.valueOf(customBid));

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/place-bid", DEALER_AUCTION_OFFER, ID))
                        .withRequestBody(getRequestBidBody(customBid))
                        .withStatusSuccessResponse()
        ).update();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).click();

        steps.onCabinetOffersPage().notifier(BID_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format(IN_AUCTION_TEMPLATE, formatPrice(customBid))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Активируем аукцион с введённой ставкой не кратной шагу -> ставка округляется вверх")
    public void shouldActivateAuctionSetCustomBidRounding() {
        int customBid = minBid + step / 2;

        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL, String.valueOf(customBid));

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/place-bid", DEALER_AUCTION_OFFER, ID))
                        .withRequestBody(getRequestBidBody(minBid + step))
                        .withStatusSuccessResponse()
        ).update();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).click();

        steps.onCabinetOffersPage().notifier(BID_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format(IN_AUCTION_TEMPLATE, formatPrice(minBid + step))));
    }

    private JsonObject getRequestBidBody(int bid) {
        JsonObject body = new JsonObject();
        body.addProperty("bid", bid * 100);
        return body;
    }

}
