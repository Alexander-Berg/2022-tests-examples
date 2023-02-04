package ru.yandex.infra.sidecars_updater.statistics;

import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static ru.yandex.infra.controller.metrics.MetricUtils.buildMetricRegistry;

public abstract class StatisticsTest {
    public static final String STATISTICS_NAME_PREFIX = "stat_";
    public static final String GROUP_STATISTICS_NAME_PREFIX = "group_";
    public static final String DEPLOY_UNIT_METRIC_SUFFIX = ".deployUnit";
    public static final String STAGE_METRIC_SUFFIX = ".stage";
    private final String STATISTICS_NAME = "stat";
    protected MetricRegistry metricRegistry;

    @BeforeEach
    public void setUp() {
        metricRegistry = buildMetricRegistry();
    }

    @ParameterizedTest
    @EnumSource(StaticStatistics.StatisticsMode.class)
    public void isDUStatisticsTest(GlobalStatistics.StatisticsMode statisticsMode) {
        Assertions.assertEquals(statisticsMode == GlobalStatistics.StatisticsMode.ALL ||
                        statisticsMode == GlobalStatistics.StatisticsMode.ONLY_DU,
                getStatInstance(STATISTICS_NAME, statisticsMode).isDUStatistics());
    }

    @ParameterizedTest
    @EnumSource(StaticStatistics.StatisticsMode.class)
    public void isStageStatisticsTest(GlobalStatistics.StatisticsMode statisticsMode) {
        Assertions.assertEquals(statisticsMode == GlobalStatistics.StatisticsMode.ALL ||
                        statisticsMode == GlobalStatistics.StatisticsMode.ONLY_STAGES,
                getStatInstance(STATISTICS_NAME, statisticsMode).isStageStatistics());
    }

    @Test
    public void getDUStatNameTest() {
        Assertions.assertEquals(STATISTICS_NAME_PREFIX + STATISTICS_NAME + DEPLOY_UNIT_METRIC_SUFFIX,
                getStatInstance(STATISTICS_NAME, StaticStatistics.StatisticsMode.ALL).getDUStatName());
    }

    @Test
    public void getStageStatNameTest() {
        Assertions.assertEquals(STATISTICS_NAME_PREFIX + STATISTICS_NAME + STAGE_METRIC_SUFFIX,
                getStatInstance(STATISTICS_NAME, StaticStatistics.StatisticsMode.ALL).getStageStatName());
    }

    @ParameterizedTest
    @EnumSource(StaticStatistics.StatisticsMode.class)
    public void prepareTest(StaticStatistics.StatisticsMode statisticsMode) {
        StaticStatistics statistics = getStatInstance(STATISTICS_NAME, statisticsMode);
        statistics.prepare(metricRegistry);
        if (statistics.isDUStatistics()) {
            checkPrepare(statistics.getDUStatName());
        }
        if (statistics.isStageStatistics()) {
            checkPrepare(statistics.getStageStatName());
        }
    }

    protected abstract StaticStatistics getStatInstance(String statisticsName, StaticStatistics.StatisticsMode statisticsMode);

    protected abstract void checkPrepare(String statName);
}
