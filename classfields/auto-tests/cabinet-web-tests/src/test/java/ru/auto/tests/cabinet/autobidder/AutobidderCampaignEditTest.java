package ru.auto.tests.cabinet.autobidder;

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
import ru.auto.tests.desktop.mock.beans.promoCampaign.Campaign;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOBIDDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.DATA_SAVED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.AUCTION_USED_AUTOBIDDER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.DAYS_FROM;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.INPUT_CAMPAIGN_NAME;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.MAX_CALL_PRICE;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.RUN_CAMPAIGN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.SAVE_CAMPAIGN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.TARGETED_CALLS_PER_DAY;
import static ru.auto.tests.desktop.element.cabinet.autobidder.ExtendedRow.TO;
import static ru.auto.tests.desktop.element.cabinet.autobidder.Filters.ON_THE_WAY;
import static ru.auto.tests.desktop.element.cabinet.autobidder.Filters.PRICE_FROM;
import static ru.auto.tests.desktop.element.cabinet.autobidder.Filters.VIN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.Filters.YEAR_FROM;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.getFilledCampaign;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.mockPromoCampaigns;
import static ru.auto.tests.desktop.mock.MockSearchCars.DEALER_AUCTION_CARS_OFFERS_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockSearchCars.searchOffers;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_LISTING_OFFER;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_MARKET_INDICATOR;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.BiddingAlgorithm.biddingAlgorithm;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.Campaign.campaign;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.Filters.filters;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.MarketIndicator.marketIndicator;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.MaxPositionForPrice.maxPositionForPrice;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.Period.period;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.PromoCampaign.promoCampaign;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.PromoCampaignParams.promoCampaignParams;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_AUTOBIDDER;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomString;
import static ru.auto.tests.desktop.utils.Utils.getRandomVin;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AUTOBIDDER)
@Story("Редактирование полей кампании")
@DisplayName("Редактирование полей кампании")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AutobidderCampaignEditTest {

    private final Campaign campaign = getFilledCampaign();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AgencyCabinetPagesSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_AUTOBIDDER, DATE_IN_PAST);

        mockRule.setStubs(
                stub("cabinet/SessionDirectDealerAristos"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGetClientAristos"),
                stub("cabinet/DesktopClientsGetAristos"),
                stub("cabinet/DealerTariff/AllTariffs"),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(campaign).getBody()),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE).getBody()),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_MARKET_INDICATOR)
                        .withResponseBody(getJsonObject(
                                campaign().setMarketIndicator(marketIndicator())))
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(AUCTION_USED_AUTOBIDDER).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактируем кампанию, меняем все фильтры")
    public void shouldEditCampaign() {
        String name = getRandomString();
        String noTargetedCallsFrom = String.valueOf(getRandomBetween(1, 50));
        String noTargetedCallsTo = String.valueOf(getRandomBetween(1, 50));
        String inWarehouseFrom = String.valueOf(getRandomBetween(1, 50));
        String inWarehouseTo = String.valueOf(getRandomBetween(1, 50));
        String priceFrom = String.valueOf(getRandomBetween(1, 1000));
        String priceTo = String.valueOf(getRandomBetween(5000, 500000));
        String vin = getRandomVin();
        String yearFrom = String.valueOf(getRandomBetween(2010, 2015));
        String yearTo = String.valueOf(getRandomBetween(2016, 2021));
        int maxCallPrice = getRandomBetween(1, 50);
        String targetedCallsPerDay = String.valueOf(getRandomBetween(1, 50));

        campaign.setName(name)
                .setDaysWithoutCalls(
                        period().setFrom(noTargetedCallsFrom)
                                .setTo(noTargetedCallsTo))
                .setDaysOnStock(
                        period().setFrom(inWarehouseFrom)
                                .setTo(inWarehouseTo))
                .setFilters(
                        filters().setPriceFrom(priceFrom)
                                .setPriceTo(priceTo)
                                .setYearFrom(yearFrom)
                                .setYearTo(yearTo)
                                .setVinCodes(asList(vin))
                                .setInStock("TO_ORDER"))
                .setMaxOfferDailyCalls(targetedCallsPerDay)
                .setBiddingAlgorithm(biddingAlgorithm().setMaxPositionForPrice(
                        maxPositionForPrice().setMaxBid(maxCallPrice * 100)))
                .setIsPristine(false);

        steps.onAutobidderPage().rows().get(0).click();

        steps.onAutobidderPage().extendedRow().input(INPUT_CAMPAIGN_NAME, name);

        steps.onAutobidderPage().extendedRow().noTargetedCallsBlock().input(DAYS_FROM, noTargetedCallsFrom);
        steps.onAutobidderPage().extendedRow().noTargetedCallsBlock().input(TO, noTargetedCallsTo);

        steps.onAutobidderPage().extendedRow().inWarehouseBlock().input(DAYS_FROM, inWarehouseFrom);
        steps.onAutobidderPage().extendedRow().inWarehouseBlock().input(TO, inWarehouseTo);

        steps.onAutobidderPage().extendedRow().filters().input(PRICE_FROM, priceFrom);
        steps.onAutobidderPage().extendedRow().filters().input(TO, priceTo);

        steps.onAutobidderPage().extendedRow().filters().selectItem(YEAR_FROM, yearFrom);
        steps.onAutobidderPage().extendedRow().filters().selectItem(TO, yearTo);

        steps.onAutobidderPage().extendedRow().filters().input(VIN, vin);

        steps.onAutobidderPage().extendedRow().filters().radioButton(ON_THE_WAY).click();

        steps.onAutobidderPage().extendedRow().budgetBlock().input(MAX_CALL_PRICE, String.valueOf(maxCallPrice));
        steps.onAutobidderPage().extendedRow().budgetBlock().input(TARGETED_CALLS_PER_DAY, targetedCallsPerDay);

        mockRule.setStubs(
                stub().withPutDeepEquals(format(DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID, campaign.getId()))
                        .withRequestBody(getJsonObject(
                                promoCampaignParams().setPromoCampaignParams(campaign)))
                        .withResponseBody(getJsonObject(
                                promoCampaign().setCampaign(campaign)))
        ).update();

        steps.onAutobidderPage().extendedRow().button(SAVE_CAMPAIGN).click();

        steps.onAutobidderPage().notifier(DATA_SAVED).should(isDisplayed());
        steps.onAutobidderPage().extendedRow().should(not(isDisplayed()));
        steps.onAutobidderPage().rows().get(0).title().should(hasText(format("#%s: %s", campaign.getId(), name)));
    }

}
