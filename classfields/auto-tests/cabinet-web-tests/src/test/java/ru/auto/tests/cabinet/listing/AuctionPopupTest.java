package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.mock.beans.offer.auction.CurrentState;
import ru.auto.tests.desktop.mock.beans.offer.auction.Segment;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUCTION_IN_USED;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.cabinet.AuctionPopup.APPLY;
import static ru.auto.tests.desktop.element.cabinet.AuctionPopup.FOR_CALL;
import static ru.auto.tests.desktop.element.cabinet.AuctionPopup.INTEREST_TOOLTIP_TEXT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.car;
import static ru.auto.tests.desktop.mock.MockUserOffer.getQuery;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.beans.offer.auction.Auction.auction;
import static ru.auto.tests.desktop.mock.beans.offer.auction.CurrentState.currentState;
import static ru.auto.tests.desktop.mock.beans.offer.auction.Segment.segment;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Epic(CABINET_DEALER)
@Feature(AUCTION_IN_USED)
@Story("Проверки попапа")
@DisplayName("Проверки попапа")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AuctionPopupTest {

    private static final String ID = "1114782187-3302e085";
    private static final String AUCTION_POPUP_TEMPLATE = "Управление аукционом звонков\nИзменяя ставку, " +
            "вы получаете возможность повлиять на процент интереса пользователей к вашему объявлению. Что такое %% интереса?\n\nВаша " +
            "активная ставка %s. Максимум интереса можно получить при ставке %s. Шаг аукциона %s. " +
            "Минимальная ставка %s.\n≈\u20095%%\n≈\u200915%%\n≈\u200920%%\n≈\u200925%%\n≈\u200935%%\nСтоимость\n%s\n" +
            "%s\nЗа звонок\n%s\n%s\nПрименить%s";

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
                stub("cabinet/DesktopClientsGetAristos")
        );
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа не активного аукциона")
    public void shouldSeePopupNotActivatedAuction() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState())
                                                                .setSegments(
                                                                        getNotActiveSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().should(hasText(format(AUCTION_POPUP_TEMPLATE,
                formatPrice(basePrice), formatPrice(maxBid), formatPrice(step), formatPrice(minBid),
                formatPrice(maxBid), formatPrice(step), formatPrice(minBid), formatPrice(maxBid), "")));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа активного аукциона")
    public void shouldSeePopupActivatedAuction() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState()
                                                                                .setCurrentBid(currentBid * 100))
                                                                .setSegments(
                                                                        getSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().should(hasText(format(AUCTION_POPUP_TEMPLATE,
                formatPrice(currentBid), formatPrice(maxBid), formatPrice(step), formatPrice(minBid),
                formatPrice(maxBid), formatPrice(step), formatPrice(step), formatPrice(maxBid), "\nСнять с аукциона")));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Нет точки на столбиках графика при не активном аукционе")
    public void shouldSeePopupNotSelectedGraphNotActivatedAuction() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState())
                                                                .setSegments(
                                                                        getNotActiveSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().dots().should(hasSize(0));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Точка на последнем столбике графика при активном аукционе")
    public void shouldSeePopupSelectedGraphActivatedAuction() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState()
                                                                                .setCurrentBid(currentBid * 100))
                                                                .setSegments(
                                                                        getSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().dots().should(hasSize(1));
        steps.onCabinetOffersPage().auctionPopup().bars().get(4).dot().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст тултипа интереса")
    public void shouldSeeInterestTooltip() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState())
                                                                .setSegments(
                                                                        getNotActiveSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();
        steps.onCabinetOffersPage().auctionPopup().button("Что такое %\u00a0интереса?").hover();

        steps.onCabinetOffersPage().interestTooltip().should(hasText(INTEREST_TOOLTIP_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Жмем на максимальную ставку в тексте попапа -> протягивается в инпут")
    public void shouldClickMaxBidInPopupDescription() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState())
                                                                .setSegments(
                                                                        getNotActiveSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).waitUntil(hasValue(formatPrice(basePrice)));
        steps.onCabinetOffersPage().auctionPopup().description().button(formatPrice(maxBid, '\u00a0')).click();

        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL).should(hasValue(formatPrice(maxBid)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка уменьшения ставки задизейблена для базовой цены")
    public void shouldSeeDisabledDecreaseButtonForBasePrice() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState())
                                                                .setSegments(
                                                                        getNotActiveSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().decreaseBid().should(not(isEnabled()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка уменьшения ставки задизейблена для ставки равной минимальной")
    public void shouldSeeDisabledDecreaseButtonForCurrentPriceEqualMinPrice() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState()
                                                                                .setCurrentBid(minBid * 100))
                                                                .setSegments(
                                                                        getSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().decreaseBid().should(not(isEnabled()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка максимальной ставки задизейблена для ставки равной максимальной")
    public void shouldSeeDisabledMaxBidButtonForCurrentPriceEqualMaxPrice() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState()
                                                                                .setCurrentBid(maxBid * 100))
                                                                .setSegments(
                                                                        getSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().maxBid().should(not(isEnabled()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка уменьшения ставки раздизейбливается при увеличении ставки равной минимальной на шаг")
    public void shouldSeeEnabledDecreaseButtonForCurrentPriceEqualMinPricePlusStep() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState()
                                                                                .setCurrentBid(minBid * 100))
                                                                .setSegments(
                                                                        getSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();
        steps.onCabinetOffersPage().auctionPopup().increaseBid().waitUntil(hasText(formatPrice(step))).click();

        steps.onCabinetOffersPage().auctionPopup().decreaseBid().should(isEnabled()).should(hasText(formatPrice(step)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Применить» задизейблена без изменения ставки, для активного аукциона")
    public void shouldSeeApplyButtonDisabledWithoutChanges() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState()
                                                                                .setCurrentBid(currentBid * 100))
                                                                .setSegments(
                                                                        getSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).should(not(isEnabled()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Применить» задизейблена, для не активного аукциона с базовой ставкой")
    public void shouldSeeApplyButtonDisabledWithBasePrice() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState())
                                                                .setSegments(
                                                                        getNotActiveSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).should(not(isEnabled()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Применить» задизейблена, для введенной ставки ниже минимальной")
    public void shouldSeeApplyButtonDisabledWithBidLessThanMinBid() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState())
                                                                .setSegments(
                                                                        getNotActiveSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL, String.valueOf(minBid - step));

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).should(not(isEnabled()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Применить» активна, для введенной ставки равной минимальной")
    public void shouldSeeApplyButtonEnabledWithBidEqualMinBid() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        getBaseCurrentState())
                                                                .setSegments(
                                                                        getNotActiveSegments())
                                                )).build())
        ).create();

        urlSteps.open();
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().hover();
        steps.onCabinetOffersPage().auctionPopup().input(FOR_CALL, String.valueOf(minBid));

        steps.onCabinetOffersPage().auctionPopup().button(APPLY).should(isEnabled());
    }

    private List<Segment> getSegments() {
        return asList(
                segment(35).setMaxBid(maxBid * 100)
                        .setCurrent(true),
                segment(25),
                segment(20),
                segment(15),
                segment(5));
    }

    private List<Segment> getNotActiveSegments() {
        return asList(
                segment(35).setMaxBid(maxBid * 100),
                segment(25),
                segment(20),
                segment(15),
                segment(5));
    }

    private CurrentState getBaseCurrentState() {
        return currentState()
                .setBasePrice(basePrice * 100)
                .setMinBid(minBid * 100)
                .setMaxBid(maxBid * 100)
                .setOneStep(step * 100);
    }

}
