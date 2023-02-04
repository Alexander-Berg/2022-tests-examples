package ru.yandex.webmaster3.storage.clickhouse;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.core.WebmasterException;
import ru.yandex.webmaster3.storage.clickhouse.dao.LegacyClickhouseTablesYDao;

/**
 * Created by Oleg Bazdyrev on 12/04/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class TableStorageTest {

    @InjectMocks
    private LegacyMdbTableStorage tableStorage;
    @Mock
    private LegacyClickhouseTablesYDao clickhouseTablesCDao;

    @Test
    public void testTableCache_successful() throws Exception {
        List<ClickhouseTableInfo> tables = new ArrayList<>();
        tables.add(new ClickhouseTableInfo(TableType.GROUP_STATISTICS, null,
                TableState.ON_LINE, DateTime.now().minusSeconds(100), null, null, "table1", null, null, 1, 1));
        tables.add(new ClickhouseTableInfo(TableType.GROUP_STATISTICS, null,
                TableState.ON_LINE, DateTime.now().minusSeconds(58), null, null, "table2", null, null, 1, 1));
        mockTables(tables);
        var table = tableStorage.getTable(TableType.GROUP_STATISTICS);
        Assert.assertEquals("table2 is too young. table1 should be returned", "table1", table.getClickhouseFullName());
        Thread.sleep(2000);
        table = tableStorage.getTable(TableType.GROUP_STATISTICS);
        Assert.assertEquals("table2 now is ok", "table2", table.getClickhouseFullName());
    }

    @Test(expected = WebmasterException.class)
    public void testTableCache_notFound() throws Exception {
        List<ClickhouseTableInfo> tables = new ArrayList<>();
        tables.add(new ClickhouseTableInfo(TableType.GROUP_STATISTICS, null,
                TableState.ON_LINE, DateTime.now().minusSeconds(50), null, null, "table2", null, null, 1, 1));
        mockTables(tables);
        tableStorage.getTable(TableType.GROUP_STATISTICS);
    }

    private void mockTables(List<ClickhouseTableInfo> tables) throws Exception {
        Mockito.when(clickhouseTablesCDao.listTables()).thenReturn(tables);
    }
}
