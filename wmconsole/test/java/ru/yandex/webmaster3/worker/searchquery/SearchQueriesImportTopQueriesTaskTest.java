package ru.yandex.webmaster3.worker.searchquery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.webmaster3.storage.util.yt.YtCluster;
import ru.yandex.webmaster3.storage.util.yt.YtCypressService;
import ru.yandex.webmaster3.storage.util.yt.YtNode;
import ru.yandex.webmaster3.storage.util.yt.YtPath;
import ru.yandex.webmaster3.storage.util.yt.YtService;
import ru.yandex.webmaster3.storage.ytimport.YtClickhouseDataLoad;
import ru.yandex.webmaster3.storage.ytimport.YtClickhouseDataLoadState;
import ru.yandex.webmaster3.storage.ytimport.YtClickhouseDataLoadType;
import ru.yandex.webmaster3.worker.ytimport.AbstractYtClickhouseDataLoadTask;

import java.net.URI;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Created by Oleg Bazdyrev on 18/04/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class SearchQueriesImportTopQueriesTaskTest {

    private static final ObjectMapper OM = new ObjectMapper();

    @InjectMocks
    private SearchQueriesImportTopQueriesTask importTask;
    private YtPath tablePath = YtPath.fromString("hahn://home/webmaster/test/searchqueries/reports_v2/latest/top_3month.top");
    @Mock
    private YtCypressService cypressService;

    @Before
    public void init() throws Exception {
        YtService ytService = new YtService(cypressService);
        ytService.setClusters(newArrayList(new YtCluster("hahn", new URI("http://example.org"), null, null)));
        importTask.setYtService(ytService);
        importTask.setTablePath(tablePath);
    }

    @Test
    public void testInit_wrongSource() throws Exception {
        Mockito.when(cypressService.getNode(Mockito.any(YtPath.class))).thenReturn(
                new YtNode(tablePath, YtNode.NodeType.TABLE, "123", "unknown", DateTime.now(), DateTime.now(),
                        OM.readTree("{\"most_recent_source\": \"vasya\"}")));

        YtClickhouseDataLoad sqi = new YtClickhouseDataLoad(YtClickhouseDataLoadType.TOP,
                YtClickhouseDataLoadState.INITIALIZING, null, null, null, null, null, null, null);
        sqi = importTask.init(sqi);
        Assert.assertEquals("No data for import found", YtClickhouseDataLoadState.DONE, sqi.getState());
    }

    @Test
    public void testInit_correctSource() throws Exception {
        Mockito.when(cypressService.getNode(Mockito.any(YtPath.class))).thenReturn(
                new YtNode(tablePath, YtNode.NodeType.TABLE, "123", "unknown", DateTime.now(), DateTime.now(),
                        OM.readTree("{\"most_recent_source\": \"hahn://some/strange/path/clicks_shows_20170302_20170302_for_wmc_web\"," +
                                "\"least_recent_source\": \"hahn://some/strange/path/clicks_shows_20170301_20170301_for_wmc_web\"}")));

        YtClickhouseDataLoad sqi = new YtClickhouseDataLoad(YtClickhouseDataLoadType.TOP,
                YtClickhouseDataLoadState.INITIALIZING, null, null, null, null, null, null, null);
        sqi = importTask.init(sqi);
        Assert.assertEquals("Found data for import", YtClickhouseDataLoadState.PREPARING, sqi.getState());
        Assert.assertEquals("Date range from 2017-03-01 to 2017-03-02", LocalDate.parse("2017-03-01"), sqi.getDateFrom());
        Assert.assertEquals("Date range from 2017-03-01 to 2017-03-02", LocalDate.parse("2017-03-02"), sqi.getDateTo());
        Assert.assertEquals("Source table is table path", tablePath, sqi.getSourceTable());
    }

    @Test
    public void testInit_correctSource2() throws Exception {
        Mockito.when(cypressService.getNode(Mockito.any(YtPath.class))).thenReturn(
                new YtNode(tablePath, YtNode.NodeType.TABLE, "123", "unknown", DateTime.now(), DateTime.now(),
                        OM.readTree("{\"most_recent_source\": \"hahn://some/strange/path/clicks_shows_20170304_20170304_for_wmc_web\"," +
                                "\"least_recent_source\": \"hahn://some/strange/path/clicks_shows_20170101_20170101_for_wmc_web\"}")));

        YtClickhouseDataLoad sqi = new YtClickhouseDataLoad(YtClickhouseDataLoadType.TOP,
                YtClickhouseDataLoadState.INITIALIZING, LocalDate.parse("2017-02-01"), LocalDate.parse("2017-03-01"), null, null, null, null, null);
        sqi = importTask.init(sqi);
        Assert.assertEquals("Found data for import", YtClickhouseDataLoadState.PREPARING, sqi.getState());
        Assert.assertEquals("Date range from 2017-03-02 to 2017-03-04", LocalDate.parse("2017-03-02"), sqi.getDateFrom());
        Assert.assertEquals("Date range from 2017-03-02 to 2017-03-04", LocalDate.parse("2017-03-04"), sqi.getDateTo());
        Assert.assertEquals("Source table is table path", tablePath, sqi.getSourceTable());
    }


    @Test
    public void testInit_correctSourceNoChanges() throws Exception {
        Mockito.when(cypressService.getNode(Mockito.any(YtPath.class))).thenReturn(
                new YtNode(tablePath, YtNode.NodeType.TABLE, "123", "unknown", DateTime.now(), DateTime.now(),
                        OM.readTree("{\"most_recent_source\": \"hahn://some/strange/path/clicks_shows_20170302_20170302_for_wmc_web\"," +
                                "\"least_recent_source\": \"hahn://some/strange/path/clicks_shows_20170301_20170301_for_wmc_web\"}")));

        YtClickhouseDataLoad sqi = new YtClickhouseDataLoad(YtClickhouseDataLoadType.TOP,
                YtClickhouseDataLoadState.INITIALIZING, LocalDate.parse("2017-03-01"), LocalDate.parse("2017-03-02"), null, null, null, null, null);
        sqi = importTask.init(sqi);
        Assert.assertEquals("Data already imported", YtClickhouseDataLoadState.DONE, sqi.getState());
    }

    @Test
    public void testDateFromSource() throws Exception {
        YtNode node = new YtNode(null, null, null, null, null, null, OM.readTree(
                "{\"most_recent_source\": \"hahn://some/strange/path/clicks_shows_20170302_20170302_for_wmc_web\"," +
                        "\"least_recent_source\": \"hahn://some/strange/path/clicks_shows_20170301_20170301_for_wmc_web\"}"));

        LocalDate date = AbstractYtClickhouseDataLoadTask.dateFromSource(node, "least_recent_source");
        Assert.assertEquals(LocalDate.parse("2017-03-01"), date);
        date = AbstractYtClickhouseDataLoadTask.dateFromSource(node, "most_recent_source");
        Assert.assertEquals(LocalDate.parse("2017-03-02"), date);

        node = new YtNode(null, null, null, null, null, null, OM.readTree("{ }"));
        date = AbstractYtClickhouseDataLoadTask.dateFromSource(node, "least_recent_source");
        Assert.assertEquals(null, date);
        date = AbstractYtClickhouseDataLoadTask.dateFromSource(node, "most_recent_source");
        Assert.assertEquals(null, date);

        node = new YtNode(null, null, null, null, null, null, OM.readTree(
                "{\"most_recent_source\": \"hahn://some/strange/path/some_trash\"," +
                        "\"least_recent_source\": \"hahn://some/strange/path/some_trash\"}"));
        date = AbstractYtClickhouseDataLoadTask.dateFromSource(node, "least_recent_source");
        Assert.assertEquals(null, date);
        date = AbstractYtClickhouseDataLoadTask.dateFromSource(node, "most_recent_source");
        Assert.assertEquals(null, date);

        // new format
        node = new YtNode(null, null, null, null, null, null, OM.readTree(
                "{\"least_recent_source\": \"//home/webmaster/prod/searchqueries/user_sessions/daily/2019-08-18\"," +
                        "\"most_recent_source\": \"//home/webmaster/prod/searchqueries/user_sessions/daily/2019-09-14\"}"));
        date = AbstractYtClickhouseDataLoadTask.dateFromSource(node, "least_recent_source");
        Assert.assertEquals(LocalDate.parse("2019-08-18"), date);
        date = AbstractYtClickhouseDataLoadTask.dateFromSource(node, "most_recent_source");
        Assert.assertEquals(LocalDate.parse("2019-09-14"), date);
    }
}
