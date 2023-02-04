package ru.yandex.infra.sidecars_updater.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.codahale.metrics.UniformReservoir;
import org.junit.jupiter.api.Assertions;

import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.sidecars_updater.TestUtils;
import ru.yandex.qe.telemetry.metrics.yasm.YasmHistogram;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.HISTOGRAM_STAT_SUFFIX;

public class InnerHistogramStatisticsTest extends HistogramStatisticsTest {

    @Override
    protected StaticStatistics getStatInstance(String statisticsName, StaticStatistics.StatisticsMode statisticsMode) {
        return new InnerHistogramStatistics(statisticsName, null,
                StatisticsRepository.DEFAULT_YASM_HISTOGRAM_SUPPLIER);
    }

    @Override
    public void isDUStatisticsTest(GlobalStatistics.StatisticsMode statisticsMode) {
        if (statisticsMode == StaticStatistics.StatisticsMode.ONLY_DU) {
            super.isDUStatisticsTest(statisticsMode);
        }
    }

    @Override
    public void isStageStatisticsTest(GlobalStatistics.StatisticsMode statisticsMode) {
        if (statisticsMode == StaticStatistics.StatisticsMode.ONLY_DU) {
            super.isStageStatisticsTest(statisticsMode);
        }
    }

//    this test generates one stage with DUs by map duMetrics ids to du id, DU metric is some collection of ints
//    this test check that all metric arguments was provided to corresponding metric and results was added to histogram
    @Override
    public void updateTest(Map<Collection<Integer>, Integer> duMetricsToDuId, StaticStatistics.StatisticsMode statisticsMode) {
        Map<TDeployUnitSpec, Collection<Integer>> deployUnitMetricMap = duMetricsToDuId.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> TDeployUnitSpec.newBuilder().setRevision(entry.getValue()).build(),
                        Map.Entry::getKey
                ));

        Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> stages =
                TestUtils.createStageMap(
                        List.of(
                                TestUtils.createStage(new ArrayList<>(deployUnitMetricMap.keySet()))
                        )
                );

        TestUtils.DummyMetric<TDeployUnitSpec, Collection<Integer>> deployUnitMetric =
                new TestUtils.DummyMetric<>(deployUnitMetricMap);

        Set<Integer> updatedValues = new HashSet<>();
        YasmHistogram yasmHistogram = new YasmHistogram(new UniformReservoir(), Set.of(HISTOGRAM_STAT_SUFFIX)) {
            @Override
            public void update(long value) {
                updatedValues.add((int) value);
                super.update(value);
            }
        };

        InnerHistogramStatistics statistics = new InnerHistogramStatistics(
                STATISTICS_NAME,
                deployUnitMetric,
                () -> yasmHistogram
        );
        statistics.prepare(metricRegistry);
        statistics.update(metricRegistry, stages);

        deployUnitMetric.checkAllKeysUsed();

        Set<Integer> expectedUpdatedValues =
                deployUnitMetricMap.entrySet().stream()
                        .flatMap(entry -> entry.getValue().stream())
                        .collect(Collectors.toSet());

        Assertions.assertEquals(expectedUpdatedValues, updatedValues);
    }
}
