package ru.yandex.infra.sidecars_updater;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class YtTest {
    private final String UNISTAT = "unistat";
    private final String STATISTICS_NAME = "Statistics_name";
    private final String STATISTICS_VALUE = "Statistics_value";
    private final String DU_STAGE_STATISTICS = "du_stage_statistics";
    private final String DU_OR_STAGE_NAME = "Du_or_stage_name";
    private final static String TABLES_FOLDER_PATH = "//tableFolder";
    private final static String STAT = "stat_";
    private final static String STAGE = "stage_";
    private final static String DU = "du_";
    private static StatisticsService statisticsService;
    private static YtTables ytTables;

    @BeforeEach
    void setUp() {
        StatisticsRepository.statisticsResults = new HashMap<>();
        StatisticsRepository.statisticsGroupResults = new HashMap<>();
        ytTables = spy(mock(YtTables.class));
        statisticsService = new StatisticsService(null, ytTables, TABLES_FOLDER_PATH, null);    }

    @Test
    public void emptyStatUpdateYtTest() {
        statisticsService.updateYt();
        verify(ytTables, times(1))
                .write(
                        YPath.simple(TABLES_FOLDER_PATH + "/" + UNISTAT).append(true),
                        YTableEntryTypes.YSON,
                        List.of()
                );

        verify(ytTables, times(1))
                .write(
                        YPath.simple(TABLES_FOLDER_PATH + "/" + DU_STAGE_STATISTICS).append(false),
                        YTableEntryTypes.YSON,
                        List.of()
                );
    }

    @Test
    public void ytUpdateTest() {
        StatisticsRepository.statisticsResults = new HashMap<>(Map.of(
                STAT + "0", 1,
                STAT + "1", 2,
                STAT + "2", 3
        ));

        StatisticsRepository.statisticsGroupResults = new HashMap<>(Map.of(
                STAT + "0", Map.of(
                        1L, Set.of(STAGE + "0")
                ),
                STAT + "1", Map.of(
                        2L, Set.of(DU + "0", DU + "1"),
                        0L, Set.of(DU + "2")
                ),
                STAT + "2", Map.of(
                        3L, Set.of(STAGE + "1", STAGE + "2")
                )
        ));

        Set<DuStageStatRes> expectedDuStageStatRows = Set.of(
                new DuStageStatRes(STAT + "0", 1, STAGE + "0"),
                new DuStageStatRes(STAT + "1", 2, DU + "0"),
                new DuStageStatRes(STAT + "1", 2, DU + "1"),
                new DuStageStatRes(STAT + "1", 0, DU + "2"),
                new DuStageStatRes(STAT + "2", 3, STAGE + "1"),
                new DuStageStatRes(STAT + "2", 3, STAGE + "2")
        );

        statisticsService.updateYt();

        ArgumentCaptor<YPath> yPath = ArgumentCaptor.forClass(YPath.class);
        ArgumentCaptor<YTableEntryType<YTreeMapNode>> yTableEntryType = ArgumentCaptor.forClass(YTableEntryType.class);
        ArgumentCaptor<List<YTreeMapNode>> tableRows = ArgumentCaptor.forClass(List.class);

        verify(ytTables, atLeastOnce()).write(yPath.capture(), yTableEntryType.capture(), tableRows.capture());

        Assertions.assertEquals(yPath.getAllValues().get(0),
                YPath.simple(TABLES_FOLDER_PATH + "/" + UNISTAT).append(true));

        Assertions.assertEquals(yTableEntryType.getAllValues().get(0), YTableEntryTypes.YSON);

        Assertions.assertEquals(
                StatisticsRepository.statisticsResults,
                tableRows.getAllValues().get(0).stream().collect(Collectors.toMap(
                        ytTreeMapNode -> ytTreeMapNode.getString(STATISTICS_NAME),
                        ytTreeMapNode ->    ytTreeMapNode.getInt(STATISTICS_VALUE)
                )));


        Assertions.assertEquals(yPath.getAllValues().get(1),
                YPath.simple(TABLES_FOLDER_PATH + "/" + DU_STAGE_STATISTICS).append(false));

        Assertions.assertEquals(yTableEntryType.getAllValues().get(1), YTableEntryTypes.YSON);

        List<DuStageStatRes> actualDuStageStatRows = tableRows.getAllValues().get(1).stream().map(ytTreeMapNode ->
                new DuStageStatRes(
                        ytTreeMapNode.getString(STATISTICS_NAME),
                        ytTreeMapNode.getInt(STATISTICS_VALUE),
                        ytTreeMapNode.getString(DU_OR_STAGE_NAME))).collect(Collectors.toList());

        Assertions.assertEquals(
                expectedDuStageStatRows.size(),
                actualDuStageStatRows.size()
        );

        Assertions.assertEquals(
                expectedDuStageStatRows,
                new HashSet<>(actualDuStageStatRows)
        );
    }

    static class DuStageStatRes {
        final String statName;
        final int statValue;
        final String duOrStageName;

        public DuStageStatRes(String statName, int statValue, String duOrStageName) {
            this.statName = statName;
            this.statValue = statValue;
            this.duOrStageName = duOrStageName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DuStageStatRes that = (DuStageStatRes) o;
            return statValue == that.statValue &&
                    Objects.equals(statName, that.statName) &&
                    Objects.equals(duOrStageName, that.duOrStageName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statName, statValue, duOrStageName);
        }
    }

}
