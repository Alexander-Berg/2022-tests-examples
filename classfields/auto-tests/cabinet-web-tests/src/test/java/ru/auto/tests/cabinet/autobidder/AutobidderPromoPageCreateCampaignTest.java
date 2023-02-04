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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOBIDDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.AUCTION_USED_AUTOBIDDER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.PAUSED;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.getBaseCampaign;
import static ru.auto.tests.desktop.mock.MockSearchCars.DEALER_AUCTION_CARS_OFFERS_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockSearchCars.searchOffers;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_LISTING_OFFER;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_MARKET_INDICATOR;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.Campaign.campaign;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.MarketIndicator.marketIndicator;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.PromoCampaign.promoCampaign;
import static ru.auto.tests.desktop.page.cabinet.AutobidderPage.ADVANTAGE_PROMO_BLOCK;
import static ru.auto.tests.desktop.page.cabinet.AutobidderPage.CREATE_FIRST_CAMPAIGN;
import static ru.auto.tests.desktop.page.cabinet.AutobidderPage.DESCRIPTION_PROMO_BLOCK;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_AUTOBIDDER;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AUTOBIDDER)
@DisplayName("Промо страница")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AutobidderPromoPageCreateCampaignTest {

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
                stub("cabinet/DealerTariff/AllTariffs")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(AUCTION_USED_AUTOBIDDER).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @Story("Промо страница")
    @DisplayName("Текст блока описания на промо странице")
    public void shouldSeePromoPageDescriptionText() {
        steps.onAutobidderPage().promoDescription().should(hasText(DESCRIPTION_PROMO_BLOCK));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @Story("Промо страница")
    @DisplayName("Текст блока преимуществ на промо странице")
    public void shouldSeePromoPageAdvantageSectionText() {
        steps.onAutobidderPage().promoAdvantageSection().should(hasText(ADVANTAGE_PROMO_BLOCK));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @Story("Создание кампании")
    @DisplayName("Создаем кампанию из блока описания на промо странице")
    public void shouldCreateCampaignFromDescriptionBlockPromoPage() {
        updateMockWithCampaignCreation();

        steps.onAutobidderPage().promoDescription().button(CREATE_FIRST_CAMPAIGN).click();

        steps.onAutobidderPage().extendedRow().should(isDisplayed());
        steps.onAutobidderPage().promoAdvantageSection().should(not(isDisplayed()));
        steps.onAutobidderPage().promoDescription().should(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @Story("Создание кампании")
    @DisplayName("Создаем кампанию из блока преимуществ на промо странице")
    public void shouldCreateCampaignFromAdvantageBlockPromoPage() {
        updateMockWithCampaignCreation();

        steps.onAutobidderPage().promoAdvantageSection().button(CREATE_FIRST_CAMPAIGN).click();

        steps.onAutobidderPage().extendedRow().should(isDisplayed());
        steps.onAutobidderPage().promoAdvantageSection().should(not(isDisplayed()));
        steps.onAutobidderPage().promoDescription().should(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

    private void updateMockWithCampaignCreation() {
        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN)
                        .withResponseBody(getJsonObject(
                                promoCampaign().setCampaign(
                                        getBaseCampaign().setStatus(PAUSED)))),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_OFFER)
                        .withResponseBody(
                                searchOffers(DEALER_AUCTION_CARS_OFFERS_EXAMPLE).getBody()),

                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_MARKET_INDICATOR)
                        .withResponseBody(getJsonObject(
                                campaign().setMarketIndicator(marketIndicator())))
        ).update();
    }

}
