package ru.auto.tests.cabinet.listing;

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
import ru.auto.tests.desktop.mock.MockUserOffers;
import ru.auto.tests.desktop.mock.beans.offer.auction.Segment;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
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
import static ru.auto.tests.desktop.mock.beans.offer.auction.Auction.auction;
import static ru.auto.tests.desktop.mock.beans.offer.auction.CurrentState.currentState;
import static ru.auto.tests.desktop.mock.beans.offer.auction.Segment.segment;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(CABINET_DEALER)
@Feature(AUCTION_IN_USED)
@Story("Текст кнопки аукциона на снипете")
@DisplayName("Текст кнопки подключенного аукциона с разными текущими сегментами и ставкой")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AuctionSnippetButtonSegmentsTextTest {

    private final int basePrice = getRandomShortInt();
    private final int step = basePrice * 50;
    private final int minBid = step * 6;
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

    @Parameterized.Parameter
    public int segmentNumber;

    @Parameterized.Parameter(1)
    public int segmentValue;

    @Parameterized.Parameters(name = "{index}: Сегмент {1} процентов")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {0, 35},
                {1, 25},
                {2, 20},
                {3, 15},
                {4, 5}
        });
    }

    @Before
    public void before() {
        List<Segment> segments = asList(
                segment(35),
                segment(25),
                segment(20),
                segment(15),
                segment(5));

        segments.get(segmentNumber).setPercent(segmentValue).setCurrent(true);

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
                                        car().setServicePricesForAuction()
                                                .setAuction(
                                                        auction().setCurrentState(
                                                                        currentState()
                                                                                .setBasePrice(basePrice * 100)
                                                                                .setCurrentBid(currentBid * 100)
                                                                                .setMinBid(minBid * 100)
                                                                                .setMaxBid(maxBid * 100)
                                                                                .setOneStep(step * 100))
                                                                .setSegments(segments))).build())
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст кнопки подключенного аукциона с разными текущими сегментами и ставкой")
    public void shouldSeeEnabledAuctionButtonWithDifferentSegments() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().auction().should(hasText(
                format("В аукционе\n%s за звонок\n≈ %d%% интереса",
                        formatPrice(currentBid), segmentValue)));
    }

}
