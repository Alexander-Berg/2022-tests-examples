package ru.yandex.infra.sidecars_updater;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.sidecars_updater.statistics.StaticStatistics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.controller.metrics.MetricUtils.buildMetricRegistry;

class StatisticsServiceTest {
    private static StatisticsService statisticsService;
    private static MetricRegistry metricRegistry;
    private static LabelStatisticsUpdater labelStatisticsUpdater;

    @BeforeEach
    void setUp() {
        metricRegistry = buildMetricRegistry();
        labelStatisticsUpdater = mock(LabelStatisticsUpdater.class);
        when(labelStatisticsUpdater.getNewLabelStatistics(anyMap())).thenReturn(List.of());
        statisticsService = new StatisticsService(metricRegistry, null, null, labelStatisticsUpdater);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10, 50, 100})
    public void registerTest(int statisticsAmount) {
        List<StaticStatistics> statistics = getMockStatistics(statisticsAmount);
        statisticsService.registerStatistic(statistics);
        statistics.forEach(statistic -> {
            verify(statistic, times(1)).prepare(metricRegistry);
            verify(statistic, times(0)).update(any(), anyMap());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10, 50, 100})
    public void updateTest(int statisticsAmount) {
        List<StaticStatistics> statistics = getMockStatistics(statisticsAmount);
        statisticsService.registerStatistic(statistics);
        statisticsService.updateStatistics(Map.of());
        statistics.forEach(statistic -> {
            verify(statistic, times(1)).prepare(metricRegistry);
            verify(statistic, times(1)).update(any(), anyMap());
        });
    }

    private List<StaticStatistics> getMockStatistics(int n) {
        List<StaticStatistics> statistics = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            statistics.add(spy(mock(StaticStatistics.class)));
        }
        return statistics;
    }
}
