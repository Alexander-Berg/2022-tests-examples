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
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUCTION_IN_USED;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.BID_SUCCESSFULY_UPDATED;
import static ru.auto.tests.desktop.consts.Notifications.OFFER_REMOVED_FROM_AUCTION;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.cabinet.AuctionPopup.APPLY;
import static ru.auto.tests.desktop.element.cabinet.AuctionPopup.FOR_CALL;
import static ru.auto.tests.desktop.element.cabinet.AuctionPopup.REMOVE_FROM_AUCTION;
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
@Story("Редактируем аукцион")
@DisplayName("Редактируем аукцион")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AuctionEditTest {

    private static final String ID = "1114782187-3302e085";
    private static final String IN_AUCTION_TEMPLATE = "В аукционе\n%s за звонок\n≈ 35%% интереса";

    private final int basePrice = getRandomShortInt();
    private final int step = basePrice * 50;
    private final int minBid = step * 4;
    private final int currentBid = minBid * 2;
    private final int maxBid = currentBid * 2;

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
                                                                                .setCurrentBid(currentBid * 100)
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
    @DisplayName("Добавляем к текущей ставке 1 шаг, сохраняем аукцион")
    public void shouldEditAuctionIncreaseForOneStep() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(currentBid)));
        steps.onCabinetOffersPage().auctionPopup().increaseBid().waitUntil(hasText(formatPrice(step))).click();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(currentBid + step)));

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/place-bid", DEALER_AUCTION_OFFER, ID))
                        .withRequestBody(getRequestBidBody(currentBid + step))
                        .withStatusSuccessResponse()
        ).update();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).click();

        steps.onCabinetOffersPage().notifier(BID_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onCabinetOffersPage().auctionPopup().should(not(isDisplayed()));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format(IN_AUCTION_TEMPLATE, formatPrice(currentBid + step))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отнимаем от текущей ставки 1 шаг, сохраняем аукцион")
    public void shouldEditAuctionDecreaseForOneStep() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(currentBid)));
        steps.onCabinetOffersPage().auctionPopup().decreaseBid().waitUntil(hasText(formatPrice(step))).click();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(currentBid - step)));

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/place-bid", DEALER_AUCTION_OFFER, ID))
                        .withRequestBody(getRequestBidBody(currentBid - step))
                        .withStatusSuccessResponse()
        ).update();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).click();

        steps.onCabinetOffersPage().notifier(BID_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onCabinetOffersPage().auctionPopup().should(not(isDisplayed()));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format(IN_AUCTION_TEMPLATE, formatPrice(currentBid - step))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Меняем ставку на максимальную, сохраняем аукцион")
    public void shouldEditAuctionSetMaxBid() {
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
        steps.onCabinetOffersPage().auctionPopup().should(not(isDisplayed()));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format(IN_AUCTION_TEMPLATE, formatPrice(maxBid))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Меняем ставку, сохраняем аукцион")
    public void shouldEditAuctionSetCustomBid() {
        int customBid = currentBid + step * 2;

        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL, String.valueOf(customBid));

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/place-bid", DEALER_AUCTION_OFFER, ID))
                        .withRequestBody(getRequestBidBody(customBid))
                        .withStatusSuccessResponse()
        ).update();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).click();

        steps.onCabinetOffersPage().notifier(BID_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onCabinetOffersPage().auctionPopup().should(not(isDisplayed()));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format(IN_AUCTION_TEMPLATE, formatPrice(customBid))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Меняем ставку на не кратную шагу, сохраняем аукцион -> ставка округляется вверх")
    public void shouldEditAuctionSetCustomBidRounding() {
        int customBid = currentBid + step / 2;

        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL, String.valueOf(customBid));

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/place-bid", DEALER_AUCTION_OFFER, ID))
                        .withRequestBody(getRequestBidBody(currentBid + step))
                        .withStatusSuccessResponse()
        ).update();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).click();

        steps.onCabinetOffersPage().notifier(BID_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onCabinetOffersPage().auctionPopup().should(not(isDisplayed()));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format(IN_AUCTION_TEMPLATE, formatPrice(currentBid + step))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отключаем аукцион")
    public void shouldRemoveAuction() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        JsonObject body = new JsonObject();
        body.addProperty("previous_bid", currentBid * 100);

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/leave", DEALER_AUCTION_OFFER, ID))
                        .withRequestBody(body)
                        .withStatusSuccessResponse()
        ).update();

        steps.onCabinetOffersPage().auctionPopup().button(REMOVE_FROM_AUCTION).click();

        steps.onCabinetOffersPage().notifier(OFFER_REMOVED_FROM_AUCTION).should(isDisplayed());
        steps.onCabinetOffersPage().auctionPopup().should(not(isDisplayed()));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format("Не в аукционе\n%s за звонок\n≈ 35%% интереса", formatPrice(basePrice))));
    }

    private JsonObject getRequestBidBody(int bid) {
        JsonObject body = new JsonObject();
        body.addProperty("previous_bid", currentBid * 100);
        body.addProperty("bid", bid * 100);
        return body;
    }

}
