import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.sidecars_updater.LabelStatisticsUpdater;
import ru.yandex.infra.sidecars_updater.statistics.GlobalStatistics;
import ru.yandex.infra.sidecars_updater.statistics.GroupStatistics;
import ru.yandex.infra.sidecars_updater.statistics.Statistics;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LabelStatisticsUpdaterTest {
    private static final String LABEL_WITH_INNER_LABELS = "du_sidecar_target_revision";
    private static final String REVISION_LABEL = "du_patchers_target_revision";
    private static final String STAGE_LABEL = "sidecar_revision_update";

    private static final String INNER_LABEL = "inner_label";
    private static final String DU = "du";
    private static final String STAGE = "stage";

    private LabelStatisticsUpdater labelStatisticsUpdater;

    @BeforeEach
    public void setUp() {
        labelStatisticsUpdater = new LabelStatisticsUpdater();
    }

    @Test
    public void getNewLabelWithInnerLabelsStatisticsTest() {
        YpObject<StageMeta, TStageSpec, TStageStatus> stage0 = mock(YpObject.class);
        when(stage0.getLabels()).thenReturn(
                Map.of(LABEL_WITH_INNER_LABELS, getTreeMap(Map.of(
                        DU + "0", getTreeMapWithDuValues(List.of(0, 1), INNER_LABEL),
                        DU + "1", getTreeMapWithDuValues(List.of(1, 2), INNER_LABEL)
                )))
        );

        int groupLabelStatistics = 2;
        int globalLabelStatisticsWithDiffValues = 4;

        YpObject<StageMeta, TStageSpec, TStageStatus> stage1 = mock(YpObject.class);
        when(stage1.getLabels()).thenReturn(
                Map.of(LABEL_WITH_INNER_LABELS, getTreeMap(Map.of(
                        DU + "1", getTreeMapWithDuValues(List.of(3, 4), INNER_LABEL),
                        DU + "2", getTreeMapWithDuValues(List.of(5, 6), INNER_LABEL)
                )))
        );

        globalLabelStatisticsWithDiffValues += 4;

        Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> stages = Map.of(
                STAGE + "0", stage0,
                STAGE + "1", stage1
        );
        getNewLabelStatisticsTest(stages, groupLabelStatistics, globalLabelStatisticsWithDiffValues);
    }

    @Test
    public void getNewRevisionLabelStatisticsTest() {
        YpObject<StageMeta, TStageSpec, TStageStatus> stage0 = mock(YpObject.class);
        when(stage0.getLabels()).thenReturn(
                Map.of(REVISION_LABEL, getTreeMapWithDuValues(List.of(0, 1, 2), DU))
        );

        int groupLabelStatistics = 1;
        int globalLabelStatisticsWithDiffValues = 3;

        Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> stages = Map.of(
                STAGE + "0", stage0
        );
        getNewLabelStatisticsTest(stages, groupLabelStatistics, globalLabelStatisticsWithDiffValues);
    }

    @Test
    public void getNewStageLabelStatisticsTest() {
        YpObject<StageMeta, TStageSpec, TStageStatus> stage0 = mock(YpObject.class);
        when(stage0.getLabels()).thenReturn(
                Map.of(STAGE_LABEL, getTreeMap(Map.of(
                        INNER_LABEL + "0", getTreeInt(0),
                        INNER_LABEL + "1", getTreeInt(1)
                )))
        );

        int groupLabelStatistics = 2;
        int globalLabelStatisticsWithDiffValues = 2;

        Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> stages = Map.of(
                STAGE + "0", stage0
        );
        getNewLabelStatisticsTest(stages, groupLabelStatistics, globalLabelStatisticsWithDiffValues);
    }

    private void getNewLabelStatisticsTest(Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> stages,
                                           int groupLabelStatistics,
                                           int globalLabelStatisticsWithDiffValues) {
        List<Statistics> newStatistics = labelStatisticsUpdater.getNewLabelStatistics(stages);
        Assertions.assertEquals(
                groupLabelStatistics,
                newStatistics.stream().filter(stat -> stat instanceof GroupStatistics).count()
        );
        Assertions.assertEquals(
                globalLabelStatisticsWithDiffValues,
                newStatistics.stream().filter(stat -> stat instanceof GlobalStatistics).count()
        );
        newStatistics = labelStatisticsUpdater.getNewLabelStatistics(stages);
        Assertions.assertEquals(0, newStatistics.size());
    }

    private static YTreeIntegerNodeImpl getTreeInt(int val) {
        return new YTreeIntegerNodeImpl(true, val, null);
    }

    private static YTreeMapNodeImpl getTreeMap(Map<String, YTreeNode> map) {
        return new YTreeMapNodeImpl(map, null);
    }

    private static YTreeMapNodeImpl getTreeMapWithDuValues(List<Integer> values, String keyPrefix) {
        Map<String, YTreeNode> map = new HashMap<>();
        for (int i = 0; i < values.size(); i++) {
            map.put(keyPrefix + i, getTreeInt(values.get(i)));
        }
        return getTreeMap(map);
    }
}
