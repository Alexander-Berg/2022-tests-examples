package ru.yandex.general.myOffers;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.STATISTICS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(MY_OFFERS_FEATURE)
@Feature(STATISTICS)
@DisplayName("Отображение столбцов статистики с разным кол-вом дней")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetStatisticsGraphBarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public int daysCount;

    @Parameterized.Parameter(2)
    public int dataBarsCount;

    @Parameterized.Parameter(3)
    public int todayDataBarsCount;

    @Parameterized.Parameter(4)
    public int noDataBarsCount;

    @Parameterized.Parameters(name = "{index}. «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Граф статистики пустой", 0, 0, 0, 7},
                {"Граф статистики 1 день", 1, 0, 1, 6},
                {"Граф статистики 6 дней", 6, 5, 1, 1},
                {"Граф статистики 7 дней", 7, 6, 1, 0},
                {"Граф статистики 8 дней", 8, 7, 1, 0},
                {"Граф статистики 14 дней", 14, 13, 1, 0},
                {"Граф статистики 30 дней", 30, 29, 1, 0}
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCabinetListing(cabinetListingResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).setStatisticsGraph(daysCount))).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение столбцов статистики с разным кол-вом дней")
    public void shouldSeeMyOffersSnippetStatisticsBars() {
        basePageSteps.onMyOffersPage().snippetFirst().statistics().dataBarsExceptToday().should(hasSize(dataBarsCount));
        basePageSteps.onMyOffersPage().snippetFirst().statistics().dataBarsToday().should(hasSize(todayDataBarsCount));
        basePageSteps.onMyOffersPage().snippetFirst().statistics().noDataBars().should(hasSize(noDataBarsCount));
    }

}
