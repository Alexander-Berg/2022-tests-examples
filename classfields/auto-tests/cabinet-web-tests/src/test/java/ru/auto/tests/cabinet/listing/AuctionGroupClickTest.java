package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mock.MockUserOffer;
import ru.auto.tests.desktop.mock.MockUserOffers;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.car;
import static ru.auto.tests.desktop.mock.MockUserOffer.getAuctionWithRandomValues;
import static ru.auto.tests.desktop.mock.MockUserOffer.getQuery;
import static ru.auto.tests.desktop.mock.MockUserOffer.getSegmentsWithCurrent;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_AUCTION_LEAVE;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления. Групповые операции аукциона")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class AuctionGroupClickTest {

    private static final String ID = getRandomOfferId();
    private static final String ID_2 = getRandomOfferId();

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
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/ClientsGet"),

                stub().withPostDeepEquals(USER_OFFERS_CARS_AUCTION_LEAVE)
                        .withRequestQuery(
                                query().setUnderscoreOfferId(ID))
                        .withStatusSuccessResponse(),

                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        getAuctionWithRandomValues()
                                                                .setSegments(
                                                                        getSegmentsWithCurrent(0))),
                                        car().setId(ID_2)).build())
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).addParam(STATUS, "active").open();
        steps.onCabinetOffersPage().salesFiltersBlock().groupOperationCheckbox().click();
    }

    @Test
    @Category(Regression.class)
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по светофору аукциона")
    public void shouldClickAuctionSvetofor() {
        mockRule.overwriteStub(7,
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID),
                                        car().setId(ID_2)
                                ).build()

                        ));

        steps.onCabinetOffersPage().groupServiceButtons().auction().
                should(hasText("Аукцион звонков\n0\n0\n0\n0\n1")).click();
        steps.onCabinetOffersPage().popup().button("Снять с аукциона").click();

        steps.onCabinetOffersPage().auctionModal().should(hasText("Снятие с аукциона звонков\nЗвоки будут стоить по " +
                "1 ₽, интерес постепенно снизится до минимума. Вы действительно хотите снять 1 объявление с аукциона " +
                "звонков?\nДа\nНет"));
        steps.onCabinetOffersPage().auctionModal().button("Да").click();

        steps.onCabinetOffersPage().salesFiltersBlock().groupOperationCheckbox().click();

        steps.onCabinetOffersPage().groupServiceButtons().auction().should(not(isDisplayed()));
    }

    @Test
    @Category(Regression.class)
    @Owner(DENISKOROBOV)
    @DisplayName("Увелечение каунтеров в светофоре")
    public void shouldFillAuctionSvetofor() {
        mockRule.overwriteStub(7,
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        getCarWithAuctionSegmentCurrent(1),
                                        getCarWithAuctionSegmentCurrent(2),
                                        getCarWithAuctionSegmentCurrent(3),
                                        getCarWithAuctionSegmentCurrent(3),
                                        getCarWithAuctionSegmentCurrent(0),
                                        getCarWithAuctionSegmentCurrent(1),
                                        getCarWithAuctionSegmentCurrent(2)
                                ).build()
                        ));

        urlSteps.refresh();
        steps.onCabinetOffersPage().salesFiltersBlock().groupOperationCheckbox().click();

        steps.onCabinetOffersPage().groupServiceButtons().auction().should(hasText("Аукцион звонков\n0\n2\n2\n2\n1"));

    }

    @Step("Получаем мок оффера с активным аукционом в сегменте «{index}»")
    public static MockUserOffer getCarWithAuctionSegmentCurrent(int index) {

        return car().setId(getRandomOfferId())
                .setServicePricesForAuction()
                .setAuction(
                        getAuctionWithRandomValues().setSegments(
                                getSegmentsWithCurrent(index)
                        )
                );
    }
}
