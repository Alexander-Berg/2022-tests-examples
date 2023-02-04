package ru.yandex.webmaster3.importer;

import java.util.Arrays;
import java.util.Collections;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.core.util.json.JsonMapping;
import ru.yandex.webmaster3.storage.importer.dao.ClickhouseTablesYDao;
import ru.yandex.webmaster3.storage.importer.model.ImportContext;
import ru.yandex.webmaster3.storage.importer.model.ImportDefinition;
import ru.yandex.webmaster3.storage.importer.model.ImportStage;
import ru.yandex.webmaster3.storage.importer.model.ImportTask;
import ru.yandex.webmaster3.storage.importer.model.MdbClickhouseTableInfo;
import ru.yandex.webmaster3.storage.importer.model.importing.ImportWithTransferManager;
import ru.yandex.webmaster3.storage.importer.model.init.ImportInitTailAndWeekTables;
import ru.yandex.webmaster3.storage.importer.model.switching.ImportSwitchReplaceTailAndWeek;
import ru.yandex.webmaster3.storage.util.clickhouse2.ClickhouseQueryContext;
import ru.yandex.webmaster3.storage.util.clickhouse2.ClickhouseServer;
import ru.yandex.webmaster3.storage.util.yt.YtCypressService;
import ru.yandex.webmaster3.storage.util.yt.YtNode;
import ru.yandex.webmaster3.storage.util.yt.YtPath;
import ru.yandex.webmaster3.storage.util.yt.transfer.YtTransferManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

/**
 * Created by Oleg Bazdyrev on 16/10/2020.
 */
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class TailAndWeekImportTest {

    private static final YtPath SOURCE_DIR = YtPath.fromString("hahn://some/path");
    private static final YtPath TAIL_TABLE = YtPath.fromString("hahn://some/path/tail");
    private static final YtPath WEEKLY_DIR = YtPath.fromString("hahn://some/path/weekly");
    private static final YtPath WEEK1 = YtPath.fromString("hahn://some/path/weekly/week_20201001_20201007");
    private static final YtPath WEEK2 = YtPath.fromString("hahn://some/path/weekly/week_20201008_20201014");
    private static final LocalDate TAIL_DATE = LocalDate.parse("1970-01-01");
    private static final LocalDate DATE1 = LocalDate.parse("2020-10-07");
    private static final LocalDate DATE2 = LocalDate.parse("2020-10-14");

    private ImportContext context;
    @Mock
    private YtCypressService cypressService;
    @Mock
    private ClickhouseServer clickhouseServer;
    @Mock
    private ClickhouseTablesYDao clickhouseTablesYDao;
    @Mock
    private YtTransferManager ytTransferManager;

    @Before
    public void init() throws Exception {
        ImportDefinition definition = ImportDefinition.builder()
                .id("test/id")
                .database("db")
                .tableNamePattern("new_test_#{task.data['INIT'].currentTable.date.asText().replace('-', '')}_#{task.data['INIT'].currentTable.updateDate}")
                .initPolicy(ImportInitTailAndWeekTables.builder().sourceDir(SOURCE_DIR).build())
                .switchPolicy(new ImportSwitchReplaceTailAndWeek())
                .importingPolicy(ImportWithTransferManager.builder().primaryKey("null").shardingKey("null").build())
                .build();

        context = ImportContext.builder()
                .definition(definition)
                .task(ImportTask.fromDefinition(definition))
                .cypressService(cypressService)
                //.clickhouseServer(clickhouseServer)
                //.clickhouseTablesYDao(clickhouseTablesYDao)
                //.ytTransferManager(ytTransferManager)
                .build();

        Mockito.when(clickhouseServer.getClusterId()).thenReturn("my-cluster");
    }

    @Test
    public void testFullCycle1() throws Exception {
        Mockito.when(clickhouseTablesYDao.listTables(anyString())).thenReturn(Collections.emptyList());

        Mockito.when(cypressService.exists(eq(TAIL_TABLE))).thenReturn(true);
        Mockito.when(cypressService.getNode(eq(TAIL_TABLE))).thenReturn(
                new YtNode(TAIL_TABLE, YtNode.NodeType.TABLE, "", "", null, null, JsonMapping.OM.createObjectNode().put("last_processed", 1600000000L)));
        Mockito.when(cypressService.list(eq(WEEKLY_DIR))).thenReturn(Arrays.asList(WEEK1, WEEK2));
        /*Mockito.when(cypressService.getNode(eq(WEEK1))).thenReturn(
                new YtNode(WEEK1, YtNode.NodeType.TABLE, "", "", null, null, JsonMapping.OM.createObjectNode().put("last_processed", 1600000000L)));
        Mockito.when(cypressService.getNode(eq(WEEK2))).thenReturn(
                new YtNode(WEEK2, YtNode.NodeType.TABLE, "", "", null, null, JsonMapping.OM.createObjectNode().put("last_processed", 1600000000L)));*/

        // init
        ImportTask task = context.getDefinition().getInitPolicy().apply(context);
        // start with tail processing
        Assert.assertEquals(ImportInitTailAndWeekTables.TableInfo.builder().tail(true)
                        .date(TAIL_DATE).replaceDates(Collections.singleton(TAIL_DATE)).path(TAIL_TABLE).updateDate(1600000000L).build(),
                task.getData(ImportStage.INIT, ImportInitTailAndWeekTables.Data.class).orElseThrow().getCurrentTable());
        // preprocess-import...
        context = context.toBuilder().task(task).build();
        task = context.getDefinition().getImportingPolicy().apply(context);
        // local table name
        Assert.assertEquals("new_test_19700101_1600000000", task.getDistributedTableName());
        task = task.toBuilder().localTableName(task.getDistributedTableName() + "_shard_xxx").data(task.clearData(ImportStage.IMPORTING)).build();

        // switch
        context = context.toBuilder().task(task).build();
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MdbClickhouseTableInfo> tableCaptor = ArgumentCaptor.forClass(MdbClickhouseTableInfo.class);
        task = context.getDefinition().getSwitchPolicy().apply(context);

        Mockito.verify(clickhouseServer, times(2)).execute(any(ClickhouseQueryContext.Builder.class), any(), queryCaptor.capture(), any(), any());
        Mockito.verify(clickhouseTablesYDao).update(tableCaptor.capture());
        Assert.assertEquals("DROP TABLE db.new_test_19700101_1600000000 ON CLUSTER my-cluster;", queryCaptor.getAllValues().get(0));
        Assert.assertEquals("RENAME TABLE db.new_test_19700101_1600000000_shard_xxx TO db.test_19700101_1600000000_shard_xxx ON CLUSTER my-cluster;", queryCaptor.getAllValues().get(1));

        task = task.toBuilder().stage(ImportStage.INIT).prevData(task.getData()).build();
        context = context.toBuilder().task(task).build();
    }

}
