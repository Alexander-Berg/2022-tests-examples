package ru.yandex.infra.sidecars_updater.statistics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.codahale.metrics.UniformReservoir;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.bolts.function.Function;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.sidecars_updater.TestUtils;
import ru.yandex.infra.sidecars_updater.util.TStageAndDuId;
import ru.yandex.qe.telemetry.metrics.yasm.YasmHistogram;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.HISTOGRAM_STAT_SUFFIX;

public class HistogramStatisticsTest extends StatisticsTest {
    protected static final String STATISTICS_NAME = "stat";

    @Override
    protected StaticStatistics getStatInstance(String statisticsName, StaticStatistics.StatisticsMode statisticsMode) {
        return new HistogramStatistics(statisticsName, null, metrics -> -1,
                StatisticsRepository.DEFAULT_YASM_HISTOGRAM_SUPPLIER, statisticsMode);
    }

    @Override
    protected void checkPrepare(String statName) {
        Assertions.assertFalse(metricRegistry.getMetrics().containsKey(statName));
    }

//    this test generates stages with DUs by map DUs ids to stage id
//    DU metric is DU's id, stage metric is function from set of DU's ids to stage id
//    these metric functions represent a map from argument to value, so this is a Dummy metric
//    this test check that all metric arguments was provided to corresponding metric and results was added to histogram
    private static Stream<Arguments> updateSource() {
        return Stream.of(
                Arguments.of(
                        Map.of(
                                Set.of(), 10
                        ),
                        StaticStatistics.StatisticsMode.ALL
                ),
                Arguments.of(
                        Map.of(
                                Set.of(1), 11
                        ),
                        StaticStatistics.StatisticsMode.ONLY_DU
                ),
                Arguments.of(
                        Map.of(
                                Set.of(2, 3), 20,
                                Set.of(4), 21,
                                Set.of(5), 22
                        ),
                        StaticStatistics.StatisticsMode.ONLY_DU
                ),
                Arguments.of(
                        Map.of(
                                Set.of(2, 3, 4), 15,
                                Set.of(5, 6), 16
                        ),
                        StaticStatistics.StatisticsMode.ALL
                ),
                Arguments.of(
                        Map.of(
                                Set.of(), 23,
                                Set.of(3, 4), 24,
                                Set.of(5), 25
                        ),
                        StaticStatistics.StatisticsMode.ALL
                )
        );
    }


    @ParameterizedTest
    @MethodSource("updateSource")
    public void updateTest(Map<Collection<Integer>, Integer> duIdsToStageId, StaticStatistics.StatisticsMode statisticsMode) {
        Map<TStageAndDuId, Integer> duToId = new HashMap<>();
        Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> stages =
                TestUtils.generateStagesByIds(duIdsToStageId, duToId);
        Function<TStageAndDuId, Integer> deployUnitMetric = new TestUtils.DummyMetric<>(duToId);
        Function<Collection<Integer>, Integer> stageMetric = new TestUtils.SetDummyMetric<>(duIdsToStageId);

        Set<Integer> updatedValues = new HashSet<>();
        YasmHistogram yasmHistogram = new YasmHistogram(new UniformReservoir(), Set.of(HISTOGRAM_STAT_SUFFIX)) {
            @Override
            public void update(long value) {
                updatedValues.add((int) value);
                super.update(value);
            }
        };

        HistogramStatistics statistics = new HistogramStatistics(
                STATISTICS_NAME,
                deployUnitMetric,
                stageMetric,
                () -> yasmHistogram,
                statisticsMode
        );
        statistics.prepare(metricRegistry);
        statistics.update(metricRegistry, stages);

        ((TestUtils.DummyMetric) deployUnitMetric).checkAllKeysUsed();
        ((TestUtils.DummyMetric) stageMetric).checkAllKeysUsed();

        Set<Integer> expectedUpdatedValues = new HashSet<>();
        if (statistics.isDUStatistics()) {
            duIdsToStageId.keySet().forEach(expectedUpdatedValues::addAll);
        }
        if (statistics.isStageStatistics()) {
            expectedUpdatedValues.addAll(duIdsToStageId.values());
        }
        Assertions.assertEquals(expectedUpdatedValues, updatedValues);
    }
}
