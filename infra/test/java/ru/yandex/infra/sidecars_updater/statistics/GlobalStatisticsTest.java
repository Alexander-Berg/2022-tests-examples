package ru.yandex.infra.sidecars_updater.statistics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.codahale.metrics.Gauge;
import org.junit.jupiter.api.Assertions;
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

class GlobalStatisticsTest extends StatisticsTest {
    private static final String STATISTICS_NAME = "stat";

    @Override
    protected StaticStatistics getStatInstance(String statisticsName, StaticStatistics.StatisticsMode statisticsMode) {
        return new GlobalStatistics(statisticsName, null, metrics -> -1, null, statisticsMode);
    }

    @Override
    protected void checkPrepare(String statName) {
        Assertions.assertTrue(metricRegistry.getMetrics().containsKey(statName));
    }

//    this test generates stages with DUs by map DUs ids to stage id
//    DU metric is DU's id, stage metric is function from set of DU's ids to stage id
//    global metric is function from stage(DU) ids to stage(DU) global result
//    these metric functions represent a map from argument to value, so this is a Dummy metric
//    this test check that all metric arguments was provided to corresponding metric and results are correct
    private static Stream<Arguments> updateSource() {
        return Stream.of(
                Arguments.of(
                        Map.of(
                                Set.of(), 10
                        ),
                        1, 2,
                        StaticStatistics.StatisticsMode.ALL
                ),
                Arguments.of(
                        Map.of(
                                Set.of(1), 11
                        ),
                        100, 200,
                        StaticStatistics.StatisticsMode.ONLY_STAGES
                ),
                Arguments.of(
                        Map.of(
                                Set.of(2, 3), 20,
                                Set.of(4), 21,
                                Set.of(5), 22
                        ),
                        3, -1,
                        StaticStatistics.StatisticsMode.ONLY_DU
                ),
                Arguments.of(
                        Map.of(
                                Set.of(2, 3, 4), 15,
                                Set.of(5, 6), 16
                        ),
                        5, 10,
                        StaticStatistics.StatisticsMode.ALL
                ),
                Arguments.of(
                        Map.of(
                                Set.of(), 23,
                                Set.of(3, 4), 24,
                                Set.of(5), 25
                        ),
                        9, 1,
                        StaticStatistics.StatisticsMode.ALL
                )
        );
    }

    @ParameterizedTest
    @MethodSource("updateSource")
    public void updateTest(Map<Collection<Integer>, Integer> duIdsToStageId, int duGlobalResult, int stageGlobalResult,
                           StaticStatistics.StatisticsMode statisticsMode) {
        Map<TStageAndDuId, Integer> duToId = new HashMap<>();
        Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> stages =
                TestUtils.generateStagesByIds(duIdsToStageId, duToId);
        Function<TStageAndDuId, Integer> deployUnitMetric = new TestUtils.DummyMetric<>(duToId);
        Function<Collection<Integer>, Integer> stageMetric = new TestUtils.SetDummyMetric<>(duIdsToStageId);
        Function<Collection<Integer>, Integer> globalMetric = new TestUtils.SetDummyMetric<>(Map.of(
                new HashSet<>(duToId.values()), duGlobalResult,
                new HashSet<>(duIdsToStageId.values()), stageGlobalResult
        ));

        GlobalStatistics statistics = new GlobalStatistics(STATISTICS_NAME, deployUnitMetric, stageMetric, globalMetric,
                statisticsMode);

        statistics.prepare(metricRegistry);
        statistics.update(metricRegistry, stages);
        ((TestUtils.DummyMetric) deployUnitMetric).checkAllKeysUsed();
        ((TestUtils.DummyMetric) stageMetric).checkAllKeysUsed();
        if (statisticsMode == StaticStatistics.StatisticsMode.ALL) {
            ((TestUtils.DummyMetric) globalMetric).checkAllKeysUsed();
        }

        if (statistics.isDUStatistics()) {
            Assertions.assertEquals(duGlobalResult,
                    ((Gauge) metricRegistry.getMetrics().get(statistics.getDUStatName())).getValue());
        }
        if (statistics.isStageStatistics()) {
            Assertions.assertEquals(stageGlobalResult,
                    ((Gauge) metricRegistry.getMetrics().get(statistics.getStageStatName())).getValue());
        }
    }
}
