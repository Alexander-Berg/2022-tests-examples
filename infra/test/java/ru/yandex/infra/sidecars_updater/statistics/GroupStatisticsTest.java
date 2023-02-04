package ru.yandex.infra.sidecars_updater.statistics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.bolts.function.Function;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.sidecars_updater.TestUtils;
import ru.yandex.infra.sidecars_updater.util.TStageAndDuId;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

class GroupStatisticsTest extends StatisticsTest {

    private static final String STATISTICS_NAME = "stat";
    private static final String DU_PREFIX = "du_";
    private static final String STAGE_PREFIX = "stage_";

    @Override
    protected StaticStatistics<Long> getStatInstance(String statisticsName, StaticStatistics.StatisticsMode statisticsMode) {
        return new GroupStatistics(statisticsName, null, metrics -> -1L, statisticsMode);
    }

    @Override
    protected void checkPrepare(String statName) {
        Assertions.assertTrue(
                StatisticsRepository.statisticsGroupResults.containsKey(statName) &&
                        StatisticsRepository.statisticsGroupResults.get(statName).isEmpty()
        );
    }

//    this test generates stages with DUs by map of stage names to map of DU names to DU metric value
//    stage metric is sum of DU values in the stage
//    this test check that all metric arguments was provided to corresponding metric and group results are correct
    private static Stream<Arguments> updateSource() {
        return Stream.of(
                Arguments.of(
                        Map.of(
                                STAGE_PREFIX + "0", Map.of(
                                        DU_PREFIX + "0", 0L
                                )
                        ),
                        Map.of(
                                0L, Set.of(getFullDUName(STAGE_PREFIX + "0", DU_PREFIX + "0"))
                        ),
                        Map.of(
                                0L, Set.of(STAGE_PREFIX + "0")
                        ),
                        StaticStatistics.StatisticsMode.ALL
                ),
                Arguments.of(
                        Map.of(
                                STAGE_PREFIX + "0", Map.of(
                                        DU_PREFIX + "0", 1L,
                                        DU_PREFIX + "1", 2L
                                ),
                                STAGE_PREFIX + "1", Map.of(
                                        DU_PREFIX + "2", 0L,
                                        DU_PREFIX + "3", 1L
                                )
                        ),
                        Map.of(
                                0L, Set.of(getFullDUName(STAGE_PREFIX + "1", DU_PREFIX + "2")),
                                1L, Set.of(
                                        getFullDUName(STAGE_PREFIX + "0", DU_PREFIX + "0"),
                                        getFullDUName(STAGE_PREFIX + "1", DU_PREFIX + "3")
                                ),
                                2L, Set.of(getFullDUName(STAGE_PREFIX + "0", DU_PREFIX + "1"))
                        ),
                        Map.of(
                                1L, Set.of(STAGE_PREFIX + "1"),
                                3L, Set.of(STAGE_PREFIX + "0")
                        ),
                        StaticStatistics.StatisticsMode.ONLY_DU
                ),
                Arguments.of(
                        Map.of(
                                STAGE_PREFIX + "0", Map.of(
                                        DU_PREFIX + "0", 0L,
                                        DU_PREFIX + "1", 1L,
                                        DU_PREFIX + "2", 3L
                                ),
                                STAGE_PREFIX + "1", Map.of(
                                        DU_PREFIX + "3", 1L,
                                        DU_PREFIX + "4", 0L
                                )
                        ),
                        Map.of(
                                0L, Set.of(
                                        getFullDUName(STAGE_PREFIX + "0", DU_PREFIX + "0"),
                                        getFullDUName(STAGE_PREFIX + "1", DU_PREFIX + "4")
                                ),
                                1L, Set.of(
                                        getFullDUName(STAGE_PREFIX + "0", DU_PREFIX + "1"),
                                        getFullDUName(STAGE_PREFIX + "1", DU_PREFIX + "3")
                                ),
                                3L, Set.of(getFullDUName(STAGE_PREFIX + "0", DU_PREFIX + "2"))
                        ),
                        Map.of(
                                1L, Set.of(STAGE_PREFIX + "1"),
                                4L, Set.of(STAGE_PREFIX + "0")
                        ),
                        StaticStatistics.StatisticsMode.ONLY_STAGES
                ),
                Arguments.of(
                        Map.of(
                                STAGE_PREFIX + "0", Map.of(
                                        DU_PREFIX + "0", 21L,
                                        DU_PREFIX + "1", 1L,
                                        DU_PREFIX + "2", 33L
                                ),
                                STAGE_PREFIX + "1", Map.of(
                                        DU_PREFIX + "3", 5L,
                                        DU_PREFIX + "4", 10L
                                ),
                                STAGE_PREFIX + "2", Map.of(
                                        DU_PREFIX + "5", 0L
                                ),
                                STAGE_PREFIX + "3", Map.of()
                        ),
                        Map.of(
                                0L, Set.of(getFullDUName(STAGE_PREFIX + "2", DU_PREFIX + "5")),
                                1L, Set.of(getFullDUName(STAGE_PREFIX + "0", DU_PREFIX + "1")),
                                5L, Set.of(getFullDUName(STAGE_PREFIX + "1", DU_PREFIX + "3")),
                                10L, Set.of(getFullDUName(STAGE_PREFIX + "1", DU_PREFIX + "4")),
                                21L, Set.of(getFullDUName(STAGE_PREFIX + "0", DU_PREFIX + "0")),
                                33L, Set.of(getFullDUName(STAGE_PREFIX + "0", DU_PREFIX + "2"))
                        ),
                        Map.of(
                                0L, Set.of(STAGE_PREFIX + "2", STAGE_PREFIX + "3"),
                                15L, Set.of(STAGE_PREFIX + "1"),
                                55L, Set.of(STAGE_PREFIX + "0")
                        ),
                        StaticStatistics.StatisticsMode.ALL
                )
        );
    }


    @ParameterizedTest
    @MethodSource("updateSource")
    public void updateSumTest(Map<String, Map<String, Long>> stageNameToDUNameToDUValue,
                              Map<Long, Set<String>> duGroups,
                              Map<Long, Set<String>> stageGroups,
                              StaticStatistics.StatisticsMode statisticsMode) {

        Map<TStageAndDuId, Long> duToValue = new HashMap<>();
        Map<Collection<Long>, Long> duValuesToStageValue = new HashMap<>();
        Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> stages =
                TestUtils.generateStagesByNamesAndDUValues(
                        stageNameToDUNameToDUValue,
                        duToValue,
                        duValuesToStageValue,
                        0L,
                        Long::sum);

        Function<TStageAndDuId, Long> deployUnitMetric = new TestUtils.DummyMetric<>(duToValue);
        Function<Collection<Long>, Long> stageMetric = new TestUtils.SetDummyMetric<>(duValuesToStageValue);

        GroupStatistics statistics = new GroupStatistics(STATISTICS_NAME, deployUnitMetric, stageMetric, statisticsMode);
        statistics.prepare(metricRegistry);
        statistics.update(metricRegistry, stages);

        ((TestUtils.DummyMetric) deployUnitMetric).checkAllKeysUsed();
        ((TestUtils.DummyMetric) stageMetric).checkAllKeysUsed();

        if (statistics.isDUStatistics()) {
            Assertions.assertEquals(
                    StatisticsRepository.statisticsGroupResults.get(statistics.getDUStatName()),
                    duGroups
            );
        }
        if (statistics.isStageStatistics()) {
            Assertions.assertEquals(
                    StatisticsRepository.statisticsGroupResults.get(statistics.getStageStatName()),
                    stageGroups
            );
        }
    }

    @Override
    @Test
    public void getDUStatNameTest() {
        Assertions.assertEquals(STATISTICS_NAME_PREFIX + GROUP_STATISTICS_NAME_PREFIX +
                        STATISTICS_NAME + DEPLOY_UNIT_METRIC_SUFFIX,
                getStatInstance(STATISTICS_NAME, StaticStatistics.StatisticsMode.ALL).getDUStatName());
    }

    @Override
    @Test
    public void getStageStatNameTest() {
        Assertions.assertEquals(STATISTICS_NAME_PREFIX + GROUP_STATISTICS_NAME_PREFIX +
                        STATISTICS_NAME + STAGE_METRIC_SUFFIX,
                getStatInstance(STATISTICS_NAME, StaticStatistics.StatisticsMode.ALL).getStageStatName());
    }

    private static String getFullDUName(String stageName, String duName) {
        return stageName + "." + duName;
    }
}
