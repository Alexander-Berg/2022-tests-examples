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
import ru.yandex.general.beans.statistics.GraphItem;
import ru.yandex.general.mock.MockUserStatistics;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.beans.statistics.GraphItem.graphItem;
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
import static ru.yandex.general.utils.Utils.formatDate;
import static ru.yandex.general.utils.Utils.getCalendar;
import static ru.yandex.general.utils.Utils.getCurrentDate;
import static ru.yandex.general.utils.Utils.getDateEarlier;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(STATISTICS)
@Feature("Графики статистики")
@DisplayName("Графики статистики")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UserStatisticsBarTest {

    private static final int TODAY_COUNT = 500;
    private static final int TWO_DAYS_AGO_COUNT = 240;
    private static final int FOUR_DAYS_AGO_COUNT = 450;
    private static final int TOTAL_COUNT = 50;

    private static List<GraphItem> graph = asList(
            graphItem().setDate(formatDate(getDateEarlier(-6))).setCounterValue(0),
            graphItem().setDate(formatDate(getDateEarlier(-5))).setCounterValue(0),
            graphItem().setDate(formatDate(getDateEarlier(-4))).setCounterValue(FOUR_DAYS_AGO_COUNT),
            graphItem().setDate(formatDate(getDateEarlier(-3))).setCounterValue(0),
            graphItem().setDate(formatDate(getDateEarlier(-2))).setCounterValue(TWO_DAYS_AGO_COUNT),
            graphItem().setDate(formatDate(getDateEarlier(-1))).setCounterValue(0),
            graphItem().setDate(formatDate(getCurrentDate())).setCounterValue(TODAY_COUNT));

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
                {VIEWS, userStatistics().setViewsStatistics(statisticsBarChart().setTotalCount(TOTAL_COUNT).setGraph(graph))},
                {FAVORITES, userStatistics().setFavoritesStatistics(statisticsBarChart().setTotalCount(TOTAL_COUNT).setGraph(graph))},
                {PHONE_SHOWS, userStatistics().setPhoneCallsStatistics(statisticsBarChart().setTotalCount(TOTAL_COUNT).setGraph(graph))},
                {CHATS, userStatistics().setChatInitsStatistics(statisticsBarChart().setTotalCount(TOTAL_COUNT).setGraph(graph))},
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics.build())
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.testing().path(MY).path(STATS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение нужного кол-ва столбцов графика на каждом блоке статистики")
    public void shouldSeeBarsCountEveryBarChart() {
        basePageSteps.onStatisticsPage().barChart(barChartName).emptyBars().should(hasSize(4));
        basePageSteps.onStatisticsPage().barChart(barChartName).notEmptyBars().should(hasSize(3));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение даты в тултипе по ховеру на непустой столбик на каждом блоке статистики")
    public void shouldSeeNotEmptyBarTooltipDate() {
        basePageSteps.onStatisticsPage().barChart(barChartName).notEmptyBars().get(2).hover();

        basePageSteps.onStatisticsPage().barChart(barChartName).tooltipDate()
                .should(hasText(formatRu(getCalendar().getTime())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение каунтера в тултипе по ховеру на непустой столбик на каждом блоке статистики")
    public void shouldSeeNotEmptyBarTooltipCounter() {
        basePageSteps.onStatisticsPage().barChart(barChartName).notEmptyBars().get(2).hover();

        basePageSteps.onStatisticsPage().barChart(barChartName).tooltipValue()
                .should(hasText(String.valueOf(TODAY_COUNT)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение даты в тултипе по ховеру на пустой столбик на каждом блоке статистики")
    public void shouldSeeEmptyBarTooltipDate() {
        basePageSteps.onStatisticsPage().barChart(barChartName).emptyBars().get(0).hover();

        basePageSteps.onStatisticsPage().barChart(barChartName).tooltipDate()
                .should(hasText(formatRu(getDateEarlier(-6))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение каунтера в тултипе по ховеру на пустой столбик на каждом блоке статистики")
    public void shouldSeeEmptyBarTooltipCounter() {
        basePageSteps.onStatisticsPage().barChart(barChartName).emptyBars().get(0).hover();

        basePageSteps.onStatisticsPage().barChart(barChartName).tooltipValue()
                .should(hasText(String.valueOf(0)));
    }

    private static String formatRu(Date date) {
        Locale locale = new Locale("ru");
        SimpleDateFormat formatter = new SimpleDateFormat("d MMMM, E", locale);

        return formatter.format(date).toLowerCase();
    }

}
