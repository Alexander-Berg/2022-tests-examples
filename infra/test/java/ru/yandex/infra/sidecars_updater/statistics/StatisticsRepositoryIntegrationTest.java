package ru.yandex.infra.sidecars_updater.statistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.sidecars_updater.TestUtils;
import ru.yandex.infra.sidecars_updater.util.Utils;
import ru.yandex.qe.telemetry.metrics.yasm.YasmHistogram;
import ru.yandex.qe.telemetry.metrics.yasm.YasmSnapshot;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static ru.yandex.infra.controller.metrics.MetricUtils.buildMetricRegistry;
import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.CUSTOM_LOG_BROKER;
import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.HAS_DOCKER;
import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.HAS_PORTO_METRICS;
import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.HAS_TVM;
import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.MORE_THAN_2DU;
import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.STAGES_WITH_MANY_DU;
import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.TOTAL;
import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.USE_LOGS;

class StatisticsRepositoryIntegrationTest {
    private static MetricRegistry metricRegistry;
    private static final String DU_PREFIX = "du_";
    private static final String STATISTICS_NAME = "stat";

    private final Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> DEFAULT_STAGE_MAP =
            createStageMap(List.of(
                    createStage(Map.of(
                            DU_PREFIX + "00", createDeployUnit(false, true, false, false, true),
                            DU_PREFIX + "01", createDeployUnit(false, true, true, true, true),
                            DU_PREFIX + "02", createDeployUnit(false, true, false, true, true)
                    )),
                    createStage(Map.of(
                            DU_PREFIX + "10", createDeployUnit(true, true, false, false, true),
                            DU_PREFIX + "11", createDeployUnit(false, true, true, false, false)
                    )),
                    createStage(Map.of(
                            DU_PREFIX + "20", createDeployUnit(true, true, false, false, true),
                            DU_PREFIX + "21", createDeployUnit(true, false, false, true, true),
                            DU_PREFIX + "22", createDeployUnit(false, true, false, true, false),
                            DU_PREFIX + "23", createDeployUnit(false, true, false, false, false)
                    )),
                    createStage(Map.of())));


    private final List<? extends Statistics> basicStatistics = StatisticsRepository.getBasicStatistics();

    @BeforeEach
    public void setUp() {
        metricRegistry = buildMetricRegistry();
    }

    @Test
    public void totalStatWithNoStagesTest() {
        checkStatisticsResult(getStatisticsResultMap(new HashMap<>()), TOTAL, 0, 0);
    }

    @Test
    public void manyDuStatWithNoStagesTest() {
        checkStatisticsResult(getStatisticsResultMap(new HashMap<>()), STAGES_WITH_MANY_DU, 0, 0);
    }

    @Test
    public void moreThan2DuStatWithNoStagesTest() {
        checkStatisticsResult(getStatisticsResultMap(new HashMap<>()), MORE_THAN_2DU, 0, 0);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 5, 10, 20, 50, 100})
    public void totalStatWithOneStageTest(int deployUnitAmount) {
        Map<String, StatisticsResult> statisticsResultMap =
                getStatisticsResultMap(createStagesWithDUs(List.of(deployUnitAmount)));
        checkStatisticsResult(statisticsResultMap, TOTAL, deployUnitAmount, 1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 5, 10, 20, 50, 100})
    public void manyDuStatWithOneStageTest(int deployUnitAmount) {
        Map<String, StatisticsResult> statisticsResultMap =
                getStatisticsResultMap(createStagesWithDUs(List.of(deployUnitAmount)));
        checkStatisticsResult(statisticsResultMap, STAGES_WITH_MANY_DU, 0, deployUnitAmount >= 2 ? 1 : 0);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 5, 10, 20, 50, 100})
    public void moreThan2DuStatWithOneStageTest(int deployUnitAmount) {
        Map<String, StatisticsResult> statisticsResultMap =
                getStatisticsResultMap(createStagesWithDUs(List.of(deployUnitAmount)));
        checkStatisticsResult(statisticsResultMap, MORE_THAN_2DU, 0, deployUnitAmount >= 3 ? 1 : 0);
    }

    public static Stream<Arguments> deployUnitAmountsForStages() {
        return Stream.of(
                Arguments.of(List.of(1, 2, 3)),
                Arguments.of(List.of(0, 0, 0)),
                Arguments.of(List.of(1, 1, 1)),
                Arguments.of(List.of(10, 20, 30)),
                Arguments.of(List.of(0, 20, 1))
        );
    }

    @ParameterizedTest
    @MethodSource("deployUnitAmountsForStages")
    public void totalStatWithManyStagesWithEmptyDUsTest(List<Integer> deployUnitAmounts) {
        Map<String, StatisticsResult> statisticsResultMap =
                getStatisticsResultMap(createStagesWithDUs(deployUnitAmounts));
        checkStatisticsResult(statisticsResultMap, TOTAL,
                deployUnitAmounts.stream().mapToInt(Integer::intValue).sum(),
                deployUnitAmounts.size());
    }

    @ParameterizedTest
    @MethodSource("deployUnitAmountsForStages")
    public void manyDuStatWithManyStagesWithEmptyDUsTest(List<Integer> deployUnitAmounts) {
        Map<String, StatisticsResult> statisticsResultMap =
                getStatisticsResultMap(createStagesWithDUs(deployUnitAmounts));
        checkStatisticsResult(statisticsResultMap, STAGES_WITH_MANY_DU,
                0,
                (int) deployUnitAmounts.stream().filter(duAmount -> duAmount > 1).count());
    }

    @ParameterizedTest
    @MethodSource("deployUnitAmountsForStages")
    public void moreThan2DuStatWithManyStagesWithEmptyDUsTest(List<Integer> deployUnitAmounts) {
        Map<String, StatisticsResult> statisticsResultMap =
                getStatisticsResultMap(createStagesWithDUs(deployUnitAmounts));
        checkStatisticsResult(statisticsResultMap, MORE_THAN_2DU,
                0,
                (int) deployUnitAmounts.stream().filter(duAmount -> duAmount > 2).count());
    }


    @Test
    public void hasTvmTest() {
        checkStatisticsResult(getStatisticsResultMap(DEFAULT_STAGE_MAP), HAS_TVM, 3, 2);
    }

    @Test
    public void useLogsTest() {
        checkStatisticsResult(getStatisticsResultMap(DEFAULT_STAGE_MAP), USE_LOGS, 8, 3);
    }

    @Test
    public void hasDockerTest() {
        checkStatisticsResult(getStatisticsResultMap(DEFAULT_STAGE_MAP), HAS_DOCKER, 2, 2);
    }

    @Test
    public void hasPortoMetricsTest() {
        checkStatisticsResult(getStatisticsResultMap(DEFAULT_STAGE_MAP), HAS_PORTO_METRICS, 4, 2);
    }

    @Test
    public void customLogBrokerTest() {
        checkStatisticsResult(getStatisticsResultMap(DEFAULT_STAGE_MAP), CUSTOM_LOG_BROKER, 6, 3);
    }

    private Map<String, StatisticsResult> getStatisticsResultMap(Map<String, YpObject<StageMeta, TStageSpec,
            TStageStatus>> stages) {
        Map<String, StatisticsResult> resultMap = new HashMap<>();
        basicStatistics.stream().filter(GlobalStatistics.class::isInstance).forEach(statistics -> {
            GlobalStatistics globalStatistics = (GlobalStatistics) statistics;
            statistics.prepare(metricRegistry);
            statistics.update(metricRegistry, stages);
            resultMap.put(globalStatistics.getName(), new StatisticsResult(
                    globalStatistics.isDUStatistics() ?
                    (int) ((Gauge) metricRegistry.getMetrics().get(globalStatistics.getDUStatName())).getValue() : 0,
                    globalStatistics.isStageStatistics() ?
                    (int) ((Gauge) metricRegistry.getMetrics().get(globalStatistics.getStageStatName())).getValue() : 0));
        });
        return resultMap;
    }

    private void checkStatisticsResult(Map<String, StatisticsResult> statisticsResultMap, String name,
                                       Integer expectedDUValue, Integer expectedStageValue) {
        Assertions.assertTrue(statisticsResultMap.containsKey(name));
        StatisticsResult statisticsResult = statisticsResultMap.get(name);
        Assertions.assertEquals(expectedDUValue, statisticsResult.deployUnitMetric);
        Assertions.assertEquals(expectedStageValue, statisticsResult.stageMetric);
    }

    private YpObject<StageMeta, TStageSpec, TStageStatus> createStage(Map<String, TDeployUnitSpec> deployUnitMap) {
        return TestUtils.createStage(deployUnitMap);
    }

    private Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> createStageMap(
            List<YpObject<StageMeta, TStageSpec, TStageStatus>> stages) {
        return TestUtils.createStageMap(stages);
    }

    private Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> createStagesWithDUs(List<Integer> deployUnitAmounts) {
        return TestUtils.createStagesWithDUs(deployUnitAmounts);
    }

    private TDeployUnitSpec createDeployUnit(boolean isHasTvm, boolean isUseLogs, boolean isHasDocker,
                                             boolean isHasProtoMetrics,
                                             boolean isHasCustomLogBroker) {
        return TestUtils.createDeployUnitBuilder(isHasTvm, isUseLogs, isHasDocker, isHasProtoMetrics,
                isHasCustomLogBroker).build();
    }


    private static Stream<Arguments> updateSource() {
        return Stream.of(
                Arguments.of(StaticStatistics.StatisticsMode.ALL, List.of(List.of(0)), List.of(1), List.of(1)),
                Arguments.of(StaticStatistics.StatisticsMode.ONLY_DU, List.of(List.of(0)), List.of(1), List.of(1)),
                Arguments.of(StaticStatistics.StatisticsMode.ONLY_STAGES, List.of(List.of(0)), List.of(1), List.of(1)),
                Arguments.of(
                        StaticStatistics.StatisticsMode.ALL,
                        List.of(
                                List.of(1)
                        ),
                        List.of(0, 1),
                        List.of(0, 1)
                ),
                Arguments.of(
                        StaticStatistics.StatisticsMode.ALL,
                        List.of(
                                List.of(0),
                                List.of(2, 5),
                                List.of(1, 1),
                                List.of(2)
                        ),
                        List.of(1, 2, 2, 0, 1),
                        List.of(1, 0, 2, 0, 0, 1)
                ),
                Arguments.of(
                        StaticStatistics.StatisticsMode.ALL,
                        List.of(
                                List.of(10, 1),
                                List.of(0, 0, 1),
                                List.of(1, 5),
                                List.of(3, 2),
                                List.of(7, 1, 1)
                        ),
                        List.of(2, 5, 1, 1, 1, 1, 1),
                        List.of(0, 1, 0, 0, 1, 1, 2)
                )
        );
    }

    // the distance of the histogram interval is growing with a coefficient of 1.5 [0, 1); [1, 1.5); [1.5, 1.5^2) ...

    @ParameterizedTest
    @MethodSource("updateSource")
    public void updateTest(StaticStatistics.StatisticsMode statisticsMode,
                           List<List<Integer>> stagesWithDUsWithBoxAmount,
                           List<Integer> duHistogram,
                           List<Integer> stageHistogram) {

        Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> stages =
                TestUtils.createStageMap(
                        stagesWithDUsWithBoxAmount.stream()
                                .map(boxAmounts ->
                                        TestUtils.createStage(
                                                boxAmounts.stream()
                                                        .map(boxAmount ->
                                                                TestUtils.addBoxesToDeployUnitBuilder(
                                                                                TDeployUnitSpec.newBuilder(),
                                                                                boxAmount)
                                                                        .build())
                                                        .collect(Collectors.toList()))
                                ).collect(Collectors.toList()));

        HistogramStatistics boxAmountHistogram = new HistogramStatistics(
                STATISTICS_NAME,
                Utils.onlyDuSpecMetrics(Utils::getBoxAmount),
                Utils.countMetrics(),
                StatisticsRepository.DEFAULT_YASM_HISTOGRAM_SUPPLIER,
                statisticsMode
        );
        boxAmountHistogram.update(metricRegistry, stages);
        if (boxAmountHistogram.isDUStatistics()) {
            testHistogram(boxAmountHistogram.getDUStatName(), duHistogram);
        } else {
            Assertions.assertFalse(metricRegistry.getMetrics().containsKey(boxAmountHistogram.getDUStatName()));
        }
        if (boxAmountHistogram.isStageStatistics()) {
            testHistogram(boxAmountHistogram.getStageStatName(), stageHistogram);
        } else {
            Assertions.assertFalse(metricRegistry.getMetrics().containsKey(boxAmountHistogram.getStageStatName()));
        }
    }

    private void testHistogram(String statName, List<Integer> histogram) {
        Assertions.assertTrue(metricRegistry.getMetrics().containsKey(statName));
        Metric metric = metricRegistry.getMetrics().get(statName);
        Assertions.assertTrue(metric instanceof YasmHistogram);
        YasmSnapshot yasmSnapshot = ((YasmHistogram) metric).getYasmSnapshot();
        List<YasmSnapshot.Entry> counters = yasmSnapshot.getCounters();
        Assertions.assertEquals(histogram.get(0), (int) yasmSnapshot.getZeroCount());
        for (int i = 0; i < Math.min(counters.size(), histogram.size() - 1); i++) {
            Assertions.assertEquals(histogram.get(i + 1), (int) counters.get(i).getCount());
        }
        for (int i = histogram.size() - 1; i < counters.size(); i++) {
            Assertions.assertEquals(0, (int) counters.get(i).getCount());
        }
    }

    static class StatisticsResult {
        final int deployUnitMetric;
        final int stageMetric;

        public StatisticsResult(int deployUnitMetric, int stageMetric) {
            this.deployUnitMetric = deployUnitMetric;
            this.stageMetric = stageMetric;
        }
    }
}
