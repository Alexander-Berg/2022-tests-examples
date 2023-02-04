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
import ru.yandex.general.mock.MockUserStatisticsBarChart;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.STATISTICS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockUserStatistics.userStatistics;
import static ru.yandex.general.mock.MockUserStatisticsBarChart.statisticsBarChart;
import static ru.yandex.general.page.StatisticsPage.CHATS;
import static ru.yandex.general.page.StatisticsPage.FAVORITES;
import static ru.yandex.general.page.StatisticsPage.PHONE_SHOWS;
import static ru.yandex.general.page.StatisticsPage.VIEWS;
import static ru.yandex.general.utils.Utils.getCurrentDate;
import static ru.yandex.general.utils.Utils.getDateEarlier;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(STATISTICS)
@Feature("Тултип разницы")
@DisplayName("Тултип разницы")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UserStatisticsStatsBarChartTooltipTest {

    private static final int DAYS_COUNT = 14;
    private static int totalCounter = getRandomIntInRange(10, 1000);
    private static int previousCounter = getRandomIntInRange(10, 1000);

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

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {VIEWS},
                {FAVORITES},
                {PHONE_SHOWS},
                {CHATS},
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(STATS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тултип разницы на каждом блоке статистики")
    public void shouldSeeTooltipAboutDifferenceEveryBarChart() {
        MockUserStatisticsBarChart barChart = statisticsBarChart().setGraphForLastDays(DAYS_COUNT)
                .setTotalCount(totalCounter).setPreviousPeriodDifference(previousCounter);

        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics()
                        .setViewsStatistics(barChart)
                        .setFavoritesStatistics(barChart)
                        .setPhoneCallsStatistics(barChart)
                        .setChatInitsStatistics(barChart)
                        .setStartDateOffset(DAYS_COUNT).build())
                .setCurrentUserExample()
                .build()).withDefaults().create();

        urlSteps.open();
        basePageSteps.onStatisticsPage().barChart(barChartName).totalCount().hover();

        basePageSteps.onStatisticsPage().popup().should(hasText(format("Разница с предыдущим\nпериодом: %s",
                formatTooltipDate(getDateEarlier(DAYS_COUNT), getCurrentDate()))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет тултипа разницы на каждом блоке статистики без разницы")
    public void shouldSeeNoTooltipAboutDifferenceEveryBarChart() {
        MockUserStatisticsBarChart barChart = statisticsBarChart().setGraphForLastDays(DAYS_COUNT)
                .setTotalCount(totalCounter).setPreviousPeriodDifference(0);

        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics()
                        .setViewsStatistics(barChart)
                        .setFavoritesStatistics(barChart)
                        .setPhoneCallsStatistics(barChart)
                        .setChatInitsStatistics(barChart)
                        .setStartDateOffset(DAYS_COUNT).build())
                .setCurrentUserExample()
                .build()).withDefaults().create();

        urlSteps.open();
        basePageSteps.onStatisticsPage().barChart(barChartName).totalCount().hover();

        basePageSteps.onStatisticsPage().popup().should(not(isDisplayed()));
    }

    private static String formatTooltipDate(Date startDate, Date endDate) {
        String tooltipDate;
        Locale locale = new Locale("ru");
        SimpleDateFormat formatterWithMonthYear = new SimpleDateFormat("d MMMM y", locale);
        SimpleDateFormat formatterWithMonth = new SimpleDateFormat("d MMMM", locale);
        SimpleDateFormat formatterWithoutMonth = new SimpleDateFormat("d", locale);
        if (startDate.getMonth() == endDate.getMonth())
            tooltipDate = format("%s – %s", formatterWithoutMonth.format(startDate), formatterWithMonthYear.format(endDate));
        else
            tooltipDate = format("%s – %s", formatterWithMonth.format(startDate), formatterWithMonthYear.format(endDate));

        return tooltipDate;
    }

}
