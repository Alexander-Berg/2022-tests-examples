package ru.auto.tests.cabinet.autobidder;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mock.beans.promoCampaign.Campaign;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOBIDDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.AUCTION_USED_AUTOBIDDER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.ACTIVE;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.PAUSED;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.getBaseCampaign;
import static ru.auto.tests.desktop.mock.MockPromoCampaigns.mockPromoCampaigns;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.OrderedSegment.orderedSegment;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_AUTOBIDDER;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(CABINET_DEALER)
@Feature(AUTOBIDDER)
@Story("Отображение свернутой кампании")
@DisplayName("Отображение каунтеров в кружочках интереса")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AutobidderRowCirclesTest {

    private final String offersCount = String.valueOf(getRandomShortInt());
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

    @Parameterized.Parameter
    public int circleIndex;

    @Parameterized.Parameters(name = "name = Кружок {index}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {0},
                {1},
                {2},
                {3},
                {4},
        });
    }

    @Before
    public void before() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_AUTOBIDDER, DATE_IN_PAST);

        campaign.getMarketIndicator().getOrderedSegments()
                .add(circleIndex, orderedSegment().setOffersCount(offersCount));

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
    @DisplayName("Каунтеры в кружочках светофора активной кампании")
    public void shouldSeeActiveCampaignCircleCounters() {
        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        campaign.setStatus(ACTIVE)
                                ).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).circle(circleIndex + 1).should(hasText(offersCount));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Каунтеры в серых кружочках кампании на паузе")
    public void shouldSeePausedCampaignCircleCounters() {
        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_CARS_USED_LISTING_PROMO_CAMPAIGN)
                        .withResponseBody(
                                mockPromoCampaigns(
                                        campaign.setStatus(PAUSED)
                                ).getBody())
        ).create();

        urlSteps.open();

        steps.onAutobidderPage().rows().get(0).circle(circleIndex + 1).should(hasText(offersCount));
    }

}
