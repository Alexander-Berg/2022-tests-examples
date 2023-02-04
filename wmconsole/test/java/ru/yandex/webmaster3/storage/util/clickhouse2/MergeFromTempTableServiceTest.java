package ru.yandex.webmaster3.storage.util.clickhouse2;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class MergeFromTempTableServiceTest {

    private static final Set<String> TABLE_NAMES = Set.of(
        "user_messages_updates_tmp_111",
        "user_messages_updates_tmp_222"
    );

    private static final List<ClickhouseHost> CLICKHOUSE_HOSTS = List.of(
        "wmc-chdb11-iva.search.yandex.net",
        "wmc-chdb12-iva.search.yandex.net",
        "wmc-chdb13-iva.search.yandex.net",
        "wmc-chdb14-iva.search.yandex.net",
        "wmc-chdb11-myt.search.yandex.net",
        "wmc-chdb12-myt.search.yandex.net",
        "wmc-chdb13-myt.search.yandex.net",
        "wmc-chdb11-sas.search.yandex.net",
        "wmc-chdb12-sas.search.yandex.net"
    ).stream()
        .map(x -> new ClickhouseHost(URI.create(x), x.substring(11, 14), Integer.parseInt(x.substring(8, 10)) - 11))
        .peek(ClickhouseHost::markAsUp)
        .collect(Collectors.toList());

    private ClickhouseServer mockCheckHostNameService;

    @Before
    public void before() {
        mockCheckHostNameService = EasyMock.createMock(ClickhouseServer.class);
    }

    @Test
    public void groupHostByTable() {
        final Map<Integer, Set<ClickhouseHost>> clickhouseHostsByShard = CLICKHOUSE_HOSTS.stream().collect(Collectors.groupingBy(ClickhouseHostLocation::getShard, Collectors.toSet()));
        final Integer shardCount = clickhouseHostsByShard.keySet().stream().max(Integer::compareTo).orElse(-1);
        if (shardCount == -1) {
            Assert.fail();
        }
        final Map<Pair<String, Integer>, List<ClickhouseHost>> tableNameAndShardWithHosts = defaultGroupHostByTable();

        Assert.assertEquals(8, tableNameAndShardWithHosts.size());

        for (var tableName : TABLE_NAMES) {
            for (int i = 0; i < shardCount; i++) {
                Assert.assertEquals(clickhouseHostsByShard.get(i), new HashSet<>(tableNameAndShardWithHosts.get(Pair.of(tableName, i))));
            }
        }
    }

    @Test
    public void getTempTablesByClickhouseHost() {
        final MergeFromTempTableService mergeFromTempTableService = new MergeFromTempTableService();
        mergeFromTempTableService.setClickhouseServer(mockCheckHostNameService);
        final AbstractSupportMergeDataFromTempTableCHDao concreteClickhouseDao
            = EasyMock.createMock(AbstractSupportMergeDataFromTempTableCHDao.class);

        EasyMock.expect(mockCheckHostNameService.getHosts()).andReturn(CLICKHOUSE_HOSTS);
        EasyMock.expect(mockCheckHostNameService.executeWithFixedHost(EasyMock.anyObject(), EasyMock.anyObject())).andReturn(List.copyOf(TABLE_NAMES)).times(CLICKHOUSE_HOSTS.size());

        EasyMock.expect(concreteClickhouseDao.getTempTablePrefix()).andReturn("user_messages_updates_tmp_");
        EasyMock.expect(concreteClickhouseDao.getMinutesIntervalSize()).andReturn(5);
        EasyMock.expect(concreteClickhouseDao.getDbName()).andReturn("webmaster3");

        EasyMock.replay(mockCheckHostNameService, concreteClickhouseDao);


        final Map<ClickhouseHost, Set<String>> tempTablesByClickhouseHost
            = mergeFromTempTableService.getTempTablesByClickhouseHost(concreteClickhouseDao);
        Assert.assertEquals(CLICKHOUSE_HOSTS.size(), tempTablesByClickhouseHost.size());
        for (var it : tempTablesByClickhouseHost.values()) {
            Assert.assertEquals(TABLE_NAMES, it);
        }
    }

    @Test
    public void mergeTempTableSuccess() {
        final MergeFromTempTableService mergeFromTempTableService = new MergeFromTempTableService();
        mergeFromTempTableService.setClickhouseServer(mockCheckHostNameService);
        final AbstractSupportMergeDataFromTempTableCHDao concreteClickhouseDao
            = EasyMock.createMock(AbstractSupportMergeDataFromTempTableCHDao.class);
        EasyMock.expect(mockCheckHostNameService.executeWithFixedHost(EasyMock.anyObject(), EasyMock.anyObject())).andReturn(true).times(TABLE_NAMES.size() * CLICKHOUSE_HOSTS.size());
        mockCheckHostNameService.execute(EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject());
        EasyMock.expectLastCall().anyTimes();

        EasyMock.expect(concreteClickhouseDao.getDbName()).andReturn("webmaster3");
        EasyMock.replay(mockCheckHostNameService, concreteClickhouseDao);

        final Map<Pair<String, Integer>, List<ClickhouseHost>> tableNameAndShardWithHosts = defaultGroupHostByTable();

        final List<String> brokenTables = mergeFromTempTableService.mergeTempTable(concreteClickhouseDao, tableNameAndShardWithHosts);

        Assert.assertEquals(0, brokenTables.size());
    }

    @Test
    public void mergeTempTableWithBrockenTables() {
        final MergeFromTempTableService mergeFromTempTableService = new MergeFromTempTableService();
        mergeFromTempTableService.setClickhouseServer(mockCheckHostNameService);
        final AbstractSupportMergeDataFromTempTableCHDao concreteClickhouseDao
            = EasyMock.createMock(AbstractSupportMergeDataFromTempTableCHDao.class);
        for (var it: CLICKHOUSE_HOSTS) {
            if (!it.toString().equals("wmc-chdb14-iva.search.yandex.net") && !it.toString().equals("wmc-chdb13-iva.search.yandex.net")) {
                EasyMock.expect(mockCheckHostNameService.executeWithFixedHost(EasyMock.eq(it), EasyMock.anyObject())).andReturn(true).times(0, 4);
            }
        }
        for (var it: CLICKHOUSE_HOSTS) {
            if (it.toString().equals("wmc-chdb14-iva.search.yandex.net") || it.toString().equals("wmc-chdb13-iva.search.yandex.net")) {
                EasyMock.expect(mockCheckHostNameService.executeWithFixedHost(EasyMock.eq(it), EasyMock.anyObject())).andReturn(false).times(2);
            }
        }
        mockCheckHostNameService.execute(EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject());
        EasyMock.expectLastCall().anyTimes();

        EasyMock.expect(concreteClickhouseDao.getDbName()).andReturn("webmaster3");
        EasyMock.replay(mockCheckHostNameService, concreteClickhouseDao);


        final Map<Pair<String, Integer>, List<ClickhouseHost>> tableNameAndShardWithHosts = defaultGroupHostByTable();
        System.out.println(tableNameAndShardWithHosts);
        final List<String> brokenTables = mergeFromTempTableService.mergeTempTable(concreteClickhouseDao, tableNameAndShardWithHosts);

        Assert.assertEquals(List.of("webmaster3.user_messages_updates_tmp_111", "webmaster3.user_messages_updates_tmp_222"), brokenTables);
    }

    @Test
    public void mergeTempTableFailWithCHException() {
        final MergeFromTempTableService mergeFromTempTableService = new MergeFromTempTableService();
        mergeFromTempTableService.setClickhouseServer(mockCheckHostNameService);
        final AbstractSupportMergeDataFromTempTableCHDao concreteClickhouseDao
            = EasyMock.createMock(AbstractSupportMergeDataFromTempTableCHDao.class);
        for (var it: CLICKHOUSE_HOSTS) {
            if (!it.toString().equals("wmc-chdb14-iva.search.yandex.net") && !it.toString().equals("wmc-chdb13-iva.search.yandex.net")) {
                EasyMock.expect(mockCheckHostNameService.executeWithFixedHost(EasyMock.eq(it), EasyMock.anyObject())).andReturn(true).times(0, 2);
            }
        }
        for (var it: CLICKHOUSE_HOSTS) {
            if (it.toString().equals("wmc-chdb14-iva.search.yandex.net") || it.toString().equals("wmc-chdb13-iva.search.yandex.net")) {
                EasyMock.expect(mockCheckHostNameService.executeWithFixedHost(EasyMock.eq(it), EasyMock.anyObject())).andReturn(true).once();
                EasyMock.expect(mockCheckHostNameService.executeWithFixedHost(EasyMock.eq(it), EasyMock.anyObject())).andThrow(
                    new ClickhouseException("corrupted table", " select", "corrupted table")).once();
            }
        }
        mockCheckHostNameService.execute(EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject());
        EasyMock.expectLastCall().anyTimes();

        EasyMock.expect(concreteClickhouseDao.getDbName()).andReturn("webmaster3");
        EasyMock.replay(mockCheckHostNameService, concreteClickhouseDao);


        final Map<Pair<String, Integer>, List<ClickhouseHost>> tableNameAndShardWithHosts = defaultGroupHostByTable(CLICKHOUSE_HOSTS, Set.of("table_name_temp"));
        System.out.println(tableNameAndShardWithHosts);
        final List<String> brokenTables = mergeFromTempTableService.mergeTempTable(concreteClickhouseDao, tableNameAndShardWithHosts);

        Assert.assertEquals(List.of("webmaster3.table_name_temp"), brokenTables);
    }

    @NotNull
    private Map<Pair<String, Integer>, List<ClickhouseHost>> defaultGroupHostByTable() {
        return defaultGroupHostByTable(CLICKHOUSE_HOSTS, TABLE_NAMES);
    }

    @NotNull
    private Map<Pair<String, Integer>, List<ClickhouseHost>> defaultGroupHostByTable(List<ClickhouseHost> clickhouseHosts, Set<String> tableNames) {
        final Map<ClickhouseHost, Set<String>> hostsWithTables = clickhouseHosts.stream().collect(Collectors.toMap(x -> x, ign -> tableNames));

        return MergeFromTempTableService.groupHostByTable(hostsWithTables);
    }

}
