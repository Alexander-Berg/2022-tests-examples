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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.AUCTION_IN_USED;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.car;
import static ru.auto.tests.desktop.mock.MockUserOffer.getQuery;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(CABINET_DEALER)
@Feature(AUCTION_IN_USED)
@Story("Тултип позиции")
@DisplayName("Текст тултипа позиции")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AuctionSnippetPositionTooltipTest {

    private static final int TOTAL_COUNT = 1000;

    private static final String HIGH_POSITION_POPUP_TEXT = "Похожими считаются объявления в вашем регионе, " +
            "у которых совпадают марка и модель. Так ищут объявления большинство пользователей Авто.ру.\n\n" +
            "С данным объявлением всё хорошо, оно заметно большинству потенциальных покупателей. " +
            "Когда позиция начнёт снижаться, рекомендуем применить услугу «поднятие».";

    private static final String LOW_POSITION_POPUP_TEXT = "Похожими считаются объявления в вашем регионе, у которых " +
            "совпадают марка и модель. Так ищут объявления большинство пользователей Авто.ру.";

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
    @DisplayName("Текст тултипа позиции для оффера на 1 месте")
    public void shouldSeeFirstPositionPopupText() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setServicePricesForAuction()
                                                .setRelevanceSearchPosition(1, TOTAL_COUNT)).build())
        ).create();
        urlSteps.open();

        steps.onCabinetOffersPage().snippet(0).position().waitUntil(hasText("На 1-м месте")).hover();
        steps.onCabinetOffersPage().positionTooltip().should(hasText(HIGH_POSITION_POPUP_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст тултипа позиции для оффера на 2 месте")
    public void shouldSeeSecondPositionPopupText() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setServicePricesForAuction()
                                                .setRelevanceSearchPosition(2, TOTAL_COUNT)).build())
        ).create();
        urlSteps.open();

        steps.onCabinetOffersPage().snippet(0).position().waitUntil(hasText("Выше 99% похожих")).hover();
        steps.onCabinetOffersPage().positionTooltip().should(hasText(HIGH_POSITION_POPUP_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст тултипа позиции для оффера «Выше 79% похожих», граничное значение")
    public void shouldSeeHighPositionPopupText() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setServicePricesForAuction()
                                                .setRelevanceSearchPosition(209, TOTAL_COUNT)).build())
        ).create();
        urlSteps.open();

        steps.onCabinetOffersPage().snippet(0).position().waitUntil(hasText("Выше 79% похожих")).hover();
        steps.onCabinetOffersPage().positionTooltip().should(hasText(HIGH_POSITION_POPUP_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст тултипа позиции для оффера «Ниже 20% похожих», граничное значение")
    public void shouldSeeLowPositionPopupText() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setServicePricesForAuction()
                                                .setRelevanceSearchPosition(210, TOTAL_COUNT)).build())
        ).create();
        urlSteps.open();

        steps.onCabinetOffersPage().snippet(0).position().waitUntil(hasText("Ниже 20% похожих")).hover();
        steps.onCabinetOffersPage().positionTooltip().should(hasText(LOW_POSITION_POPUP_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст тултипа позиции для оффера «Ниже 99% похожих», последняя позиция")
    public void shouldSeeLastPositionPopupText() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setServicePricesForAuction()
                                                .setRelevanceSearchPosition(TOTAL_COUNT, TOTAL_COUNT)).build())
        ).create();
        urlSteps.open();

        steps.onCabinetOffersPage().snippet(0).position().waitUntil(hasText("Ниже 99% похожих")).hover();
        steps.onCabinetOffersPage().positionTooltip().should(hasText(LOW_POSITION_POPUP_TEXT));
    }

}
