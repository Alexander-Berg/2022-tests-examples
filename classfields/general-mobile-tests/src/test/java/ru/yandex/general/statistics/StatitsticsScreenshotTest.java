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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockUserStatisticsBarChart;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.GeneralFeatures.STATISTICS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.mobile.page.StatisticsPage.VIEWS;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockUserStatistics.userStatistics;
import static ru.yandex.general.mock.MockUserStatisticsBarChart.statisticsBarChart;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;

@Epic(STATISTICS)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншотные тесты")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class StatitsticsScreenshotTest {

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

    @Inject
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public String theme;

    @Parameterized.Parameters(name = "Скриншот статистики. тема «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {LIGHT_THEME},
                {DARK_THEME}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        urlSteps.testing().path(MY).path(STATS);
        compareSteps.resize(375, 1250);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот страницы статистики, графики с данными, светлая/темная темы")
    public void shouldSeeStatisticsWithDataScreenshot() {
        MockUserStatisticsBarChart barChart = statisticsBarChart().setGraphForLastDays(14);

        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics()
                        .setTotalActive(525).setTotalBanned(12).setTotalEarned(21345).setTotalExpired(33).setTotalSold(5)
                        .setViewsStatistics(barChart.setTotalCount(1215).setPreviousPeriodDifference(250))
                        .setChatInitsStatistics(barChart.setTotalCount(55).setPreviousPeriodDifference(-11))
                        .setPhoneCallsStatistics(barChart.setTotalCount(128).setPreviousPeriodDifference(0))
                        .setFavoritesStatistics(barChart.setTotalCount(9).setPreviousPeriodDifference(-169)).build())
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.open();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onStatisticsPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onStatisticsPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот страницы статистики, часть графиков с данными, часть без, светлая/темная темы")
    public void shouldSeeStatisticsWithPartialDataScreenshot() {
        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics()
                        .setTotalActive(1599).setTotalBanned(0).setTotalEarned(1694855).setTotalExpired(33).setTotalSold(5)
                        .setViewsStatistics(statisticsBarChart().setGraphForLastDays(30)
                                .setTotalCount(50).setPreviousPeriodDifference(250))
                        .setChatInitsStatistics(statisticsBarChart().setZeroStatGraph()
                                .setTotalCount(0))
                        .setPhoneCallsStatistics(statisticsBarChart().setZeroStatGraph()
                                .setTotalCount(0))
                        .setFavoritesStatistics(statisticsBarChart().setGraphForLastDays(30)
                                .setTotalCount(9).setPreviousPeriodDifference(-169)).build())
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.open();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onStatisticsPage().pageRoot());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onStatisticsPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот попапа просмотров, светлая/темная темы")
    public void shouldSeeStatisticsViewsPopupScreenshot() {
        MockUserStatisticsBarChart barChart = statisticsBarChart().setGraphForLastDays(14);

        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics()
                        .setTotalActive(525).setTotalBanned(12).setTotalEarned(21345).setTotalExpired(33).setTotalSold(5)
                        .setViewsStatistics(barChart.setTotalCount(1215).setPreviousPeriodDifference(250)).build())
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onStatisticsPage().barChart(VIEWS).click();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onStatisticsPage().popup(VIEWS));

        urlSteps.setProductionHost().open();
        basePageSteps.onStatisticsPage().barChart(VIEWS).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onStatisticsPage().popup(VIEWS));

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
