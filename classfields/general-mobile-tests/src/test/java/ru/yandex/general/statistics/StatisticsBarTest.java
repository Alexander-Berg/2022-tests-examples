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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockUserStatistics;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(STATISTICS)
@Feature("Графики статистики")
@DisplayName("Графики статистики")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class StatisticsBarTest {

    private static final int TODAY_COUNT = 500;
    private static final int TWO_DAYS_AGO_COUNT = 240;
    private static final int FOUR_DAYS_AGO_COUNT = 450;
    private static final int TOTAL_COUNT = 50;

    private static List<GraphItem> graph = asList(
            graphItem().setDate(formatDate(getDateEarlier(6))).setCounterValue(0),
            graphItem().setDate(formatDate(getDateEarlier(5))).setCounterValue(0),
            graphItem().setDate(formatDate(getDateEarlier(4))).setCounterValue(FOUR_DAYS_AGO_COUNT),
            graphItem().setDate(formatDate(getDateEarlier(3))).setCounterValue(0),
            graphItem().setDate(formatDate(getDateEarlier(2))).setCounterValue(TWO_DAYS_AGO_COUNT),
            graphItem().setDate(formatDate(getDateEarlier(1))).setCounterValue(0),
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
        basePageSteps.resize(375, 1250);
        urlSteps.testing().path(MY).path(STATS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение нужного кол-ва столбцов графика на каждом блоке статистики")
    public void shouldSeeBarsCountEveryBarChart() {
        basePageSteps.onStatisticsPage().barChart(barChartName).click();
        basePageSteps.onStatisticsPage().popup(barChartName).stats().emptyBars().should(hasSize(4));
        basePageSteps.onStatisticsPage().popup(barChartName).stats().notEmptyBars().should(hasSize(3));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение даты под последним столбиком на каждом блоке статистики")
    public void shouldSeeNotEmptyBarDate() {
        basePageSteps.onStatisticsPage().barChart(barChartName).click();
        basePageSteps.onStatisticsPage().popup(barChartName).stats().notEmptyBars().get(1).waitUntil(isDisplayed());

        basePageSteps.onStatisticsPage().popup(barChartName).stats().datesAxisList().get(6)
                .should(hasText(formatAxisDate(getCalendar().getTime())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение каунтера над последним столбиком на каждом блоке статистики")
    public void shouldSeeNotEmptyBarCounter() {
        basePageSteps.onStatisticsPage().barChart(barChartName).click();
        basePageSteps.onStatisticsPage().popup(barChartName).stats().notEmptyBars().get(1).waitUntil(isDisplayed());

        basePageSteps.onStatisticsPage().popup(barChartName).stats().countersBarList().get(6)
                .should(hasText(String.valueOf(TODAY_COUNT)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение каунтера = «0» над пустым столбиком на каждом блоке статистики")
    public void shouldSeeEmptyBarCounter() {
        basePageSteps.onStatisticsPage().barChart(barChartName).click();
        basePageSteps.onStatisticsPage().popup(barChartName).stats().emptyBars().get(0).waitUntil(isDisplayed());

        basePageSteps.onStatisticsPage().popup(barChartName).stats().countersBarList().get(0)
                .should(hasText(String.valueOf(0)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение даты в блоке инфо по клику на последний столбик на каждом блоке статистики")
    public void shouldSeeNotEmptyBarInfoDate() {
        basePageSteps.onStatisticsPage().barChart(barChartName).click();
        basePageSteps.onStatisticsPage().popup(barChartName).stats().notEmptyBars().get(2).waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();

        basePageSteps.onStatisticsPage().popup(barChartName).stats().date()
                .should(hasText(formatRu(getCalendar().getTime())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение каунтера в блоке инфо по клику на последний столбик на каждом блоке статистики")
    public void shouldSeeNotEmptyBarInfoCounter() {
        basePageSteps.onStatisticsPage().barChart(barChartName).click();
        basePageSteps.onStatisticsPage().popup(barChartName).stats().notEmptyBars().get(2).waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();

        basePageSteps.onStatisticsPage().popup(barChartName).stats().totalCount()
                .should(hasText(String.valueOf(TODAY_COUNT)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение каунтера = «0» в блоке инфо по клику на пустой столбик на каждом блоке статистики")
    public void shouldSeeEmptyBarInfoCounter() {
        basePageSteps.onStatisticsPage().barChart(barChartName).click();
        basePageSteps.onStatisticsPage().popup(barChartName).stats().emptyBars().get(0).waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();

        basePageSteps.onStatisticsPage().popup(barChartName).stats().totalCount()
                .should(hasText(String.valueOf(0)));
    }

    private static String formatAxisDate(Date date) {
        Locale locale = new Locale("ru");
        SimpleDateFormat formatter = new SimpleDateFormat("d, E", locale);

        return formatter.format(date).toLowerCase();
    }

    private static String formatRu(Date date) {
        Locale locale = new Locale("ru");
        SimpleDateFormat formatter = new SimpleDateFormat("d MMMM y", locale);

        return formatter.format(date).toLowerCase();
    }

}
