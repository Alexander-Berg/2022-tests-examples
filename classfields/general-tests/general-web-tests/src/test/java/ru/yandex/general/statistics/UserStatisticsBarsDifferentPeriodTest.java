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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.STATISTICS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockUserStatistics.userStatistics;
import static ru.yandex.general.mock.MockUserStatisticsBarChart.statisticsBarChart;
import static ru.yandex.general.page.StatisticsPage.VIEWS;

@Epic(STATISTICS)
@Feature("Графики статистики")
@DisplayName("Графики статистики")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UserStatisticsBarsDifferentPeriodTest {

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
    public int daysCount;

    @Parameterized.Parameters(name = "Кол-во дней = «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {7},
                {14},
                {30}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics().setViewsStatistics(
                        statisticsBarChart().setTotalCount(50).setGraphForLastDays(daysCount)).build())
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.testing().path(MY).path(STATS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение нужного кол-ва столбцов графика с разным кол-вом дней")
    public void shouldSeeBarsCount() {
        basePageSteps.onStatisticsPage().barChart(VIEWS).emptyBars().should(hasSize(0));
        basePageSteps.onStatisticsPage().barChart(VIEWS).notEmptyBars().should(hasSize(daysCount));
    }

}
