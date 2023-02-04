package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import ru.auto.tests.desktop.mock.MockUserOffers;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
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
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Хеддер групповых операций. Светофор аукциона")
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(CabinetTestsModule.class)
public class AuctionGroupOperationsCarsTest {

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

    @Parameterized.Parameter
    public int segmentNumber;

    @Parameterized.Parameter(1)
    public String segmentText;

    @Parameterized.Parameters(name = "Сегмент {index}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {0, "\n0\n0\n0\n0\n1"},
                {1, "\n0\n0\n0\n1\n0"},
                {2, "\n0\n0\n1\n0\n0"},
                {3, "\n0\n1\n0\n0\n0"},
                {4, "\n1\n0\n0\n0\n0"}
        });
    }

    @Before
    public void before() {

        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/ClientsGet"),
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                getQuery().setSection(MockUserOffers.USED))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID)
                                                .setServicePricesForAuction()
                                                .setAuction(
                                                        getAuctionWithRandomValues()
                                                                .setSegments(getSegmentsWithCurrent(segmentNumber))
                                                ),
                                        car().setId(ID_2)).build())
        ).create();
    }

    @Test
    @Category(Regression.class)
    @Owner(DENISKOROBOV)
    @DisplayName("Проверяем значения в  светофоре аукциона")
    public void shouldSeeAuctionSvetofor() {

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).addParam(STATUS, "active").open();
        steps.onCabinetOffersPage().salesFiltersBlock().groupOperationCheckbox().click();
        steps.onCabinetOffersPage().groupServiceButtons().auction()
                .should(hasText(format("Аукцион звонков%s", segmentText)));

    }
}
