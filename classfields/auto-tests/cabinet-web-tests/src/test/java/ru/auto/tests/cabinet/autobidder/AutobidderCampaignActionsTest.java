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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOBIDDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.CAMPAIGN_ACTIVATED;
import static ru.auto.tests.desktop.consts.Notifications.CAMPAIGN_DELETED;
import static ru.auto.tests.desktop.consts.Notifications.CAMPAIGN_PAUSED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.AUCTION_USED_AUTOBIDDER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.autobidder.Row.ACTIVE_CAMPAIGN;
import static ru.auto.tests.desktop.element.cabinet.autobidder.Row.SUSPENDED;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.ACTIVE;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.PAUSED;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.getBaseCampaign;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.mockPromoCampaigns;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID_ACTIVATE;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID_PAUSE;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_AUTOBIDDER;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AUTOBIDDER)
@Story("Действия с кампанией")
@DisplayName("Действия с кампанией")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AutobidderCampaignActionsTest {

    private final Campaign campaign = getBaseCampaign();

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
        );

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(AUCTION_USED_AUTOBIDDER);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Останавливаем кампанию кнопкой «Pause»")
    public void shouldPauseCampaign() {
        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        campaign.setStatus(ACTIVE)).getBody())
        ).create();

        urlSteps.open();

        mockRule.setStubs(
                stub().withPutDeepEquals(format(DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID_PAUSE, campaign.getId()))
                        .withStatusSuccessResponse()
        ).update();

        steps.onAutobidderPage().rows().get(0).pause().click();

        steps.onAutobidderPage().notifier(CAMPAIGN_PAUSED).should(isDisplayed());
        steps.onAutobidderPage().rows().get(0).status().should(hasText(SUSPENDED));
        steps.onAutobidderPage().rows().get(0).play().should(isDisplayed());
        steps.onAutobidderPage().rows().get(0).pause().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Запускаем кампанию кнопкой «Play»")
    public void shouldPlayCampaign() {
        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        campaign.setStatus(PAUSED)).getBody())
        ).create();

        urlSteps.open();

        mockRule.setStubs(
                stub().withPutDeepEquals(format(DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID_ACTIVATE, campaign.getId()))
                        .withStatusSuccessResponse()
        ).update();

        steps.onAutobidderPage().rows().get(0).play().click();

        steps.onAutobidderPage().notifier(CAMPAIGN_ACTIVATED).should(isDisplayed());
        steps.onAutobidderPage().rows().get(0).status().should(hasText(ACTIVE_CAMPAIGN));
        steps.onAutobidderPage().rows().get(0).pause().should(isDisplayed());
        steps.onAutobidderPage().rows().get(0).play().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем единственную кампанию, оказываемся на промо странице")
    public void shouldDeleteLastCampaign() {
        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        campaign.setStatus(ACTIVE)).getBody())
        ).create();

        urlSteps.open();

        mockRule.setStubs(
                stub().withDeleteDeepEquals(format(DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID, campaign.getId()))
                        .withStatusSuccessResponse()
        ).update();
        mockRule.overwriteStub(5,
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns().getBody()));

        steps.onAutobidderPage().rows().get(0).delete().click();

        steps.onAutobidderPage().notifier(CAMPAIGN_DELETED).should(isDisplayed());
        steps.onAutobidderPage().promoDescription().should(isDisplayed());
        steps.onAutobidderPage().promoAdvantageSection().should(isDisplayed());
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем вторую кампанию, остается одна")
    public void shouldDeleteSecondCampaign() {
        Campaign secondCampaign = getBaseCampaign().setId(String.valueOf(getRandomShortInt())).setStatus(PAUSED);

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        campaign.setStatus(ACTIVE),
                                        secondCampaign
                                ).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().waitUntil(hasSize(2));

        mockRule.setStubs(
                stub().withDeleteDeepEquals(format(DEALER_AUCTION_CARS_USED_PROMO_CAMPAIGN_ID, campaign.getId()))
                        .withStatusSuccessResponse()
        ).update();
        mockRule.overwriteStub(5,
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(secondCampaign).getBody()));

        steps.onAutobidderPage().rows().get(0).delete().click();

        steps.onAutobidderPage().notifier(CAMPAIGN_DELETED).should(isDisplayed());
        steps.onAutobidderPage().rows().should(hasSize(1));
        steps.onAutobidderPage().rows().get(0).title().should(hasText(format("#%s:", secondCampaign.getId())));
    }

}
