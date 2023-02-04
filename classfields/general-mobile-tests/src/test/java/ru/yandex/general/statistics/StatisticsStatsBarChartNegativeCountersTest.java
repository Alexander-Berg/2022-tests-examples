package ru.yandex.general.statistics;

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
import ru.yandex.general.mock.MockUserStatistics;
import ru.yandex.general.mock.MockUserStatisticsBarChart;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.STATISTICS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockUserStatistics.userStatistics;
import static ru.yandex.general.mock.MockUserStatisticsBarChart.statisticsBarChart;
import static ru.yandex.general.mobile.page.StatisticsPage.CHATS;
import static ru.yandex.general.mobile.page.StatisticsPage.FAVORITES;
import static ru.yandex.general.mobile.page.StatisticsPage.PHONE_SHOWS;
import static ru.yandex.general.mobile.page.StatisticsPage.VIEWS;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(STATISTICS)
@Feature("Каунтеры в блоках графиков статистики")
@DisplayName("Отрицательные каунтеры в блоках графиков статистики")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class StatisticsStatsBarChartNegativeCountersTest {

    private static int totalCounter = getRandomIntInRange(10, 1000);
    private static int previousCounter = getRandomIntInRange(10, 1000);
    private static MockUserStatisticsBarChart barChart = statisticsBarChart().setGraphForLastDays(14)
            .setTotalCount(totalCounter).setPreviousPeriodDifference(-previousCounter);

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
    public String barChartName;

    @Parameterized.Parameter(1)
    public MockUserStatistics userStatistics;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {VIEWS, userStatistics().setViewsStatistics(barChart)},
                {FAVORITES, userStatistics().setFavoritesStatistics(barChart)},
                {PHONE_SHOWS, userStatistics().setPhoneCallsStatistics(barChart)},
                {CHATS, userStatistics().setChatInitsStatistics(barChart)},
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics.build())
                .setCurrentUserExample()
                .build()).withDefaults().create();
        basePageSteps.resize(375, 1250);
        urlSteps.testing().path(MY).path(STATS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Каунтер разницы с последним периодом на каждом блоке статистики, отрицательный")
    public void shouldSeeTotalCounterPreviousPeriodNegativeEveryBarChart() {
        basePageSteps.onStatisticsPage().barChart(barChartName).click();

        basePageSteps.onStatisticsPage().popup(barChartName).stats().previousPeriodDifference()
                .should(hasText(format("-%d", previousCounter)));
    }

}
