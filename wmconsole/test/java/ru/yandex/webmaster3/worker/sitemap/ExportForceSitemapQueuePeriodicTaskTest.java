package ru.yandex.webmaster3.worker.sitemap;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import junit.framework.TestCase;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.host.CommonDataState;
import ru.yandex.webmaster3.storage.host.CommonDataType;
import ru.yandex.webmaster3.storage.settings.dao.CommonDataStateYDao;
import ru.yandex.webmaster3.storage.sitemap.dao.ForceSitemapExportQueueYDao;
import ru.yandex.webmaster3.storage.util.yt.TableWriter;
import ru.yandex.webmaster3.storage.util.yt.YtException;
import ru.yandex.webmaster3.storage.util.yt.YtPath;
import ru.yandex.webmaster3.storage.util.yt.YtService;
import ru.yandex.webmaster3.storage.util.yt.YtTableData;
import ru.yandex.webmaster3.storage.util.yt.YtTransactionService;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author akhazhoyan 02/2018
 */
public class ExportForceSitemapQueuePeriodicTaskTest extends TestCase {

    private static final YtPath workDir = YtPath.fromString("arnold://foo/bar");
    private static final CommonDataType OUR_DATA_TYPE = CommonDataType.LAST_SITEMAP_FORCE_QUEUE_RECORD_TS;
    private CommonDataStateYDao commonDataStateYDao;
    private ForceSitemapExportQueueYDao forceSitemapExportQueueYDao;
    private YtService ytService;
    private YtTransactionService.TransactionBuilder transactionBuilder;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.commonDataStateYDao = mock(CommonDataStateYDao.class);
        this.forceSitemapExportQueueYDao = mock(ForceSitemapExportQueueYDao.class);
        this.ytService = mock(YtService.class);
        this.transactionBuilder = mock(YtTransactionService.TransactionBuilder.class);
        when(ytService.prepareTableData(any(), any())).thenReturn(mock(YtTableData.class));
        when(ytService.inTransaction(any(YtPath.class))).thenReturn(transactionBuilder);
    }

    public void testLatestProcessedTsUpdates() throws Exception {
        DateTime started = DateTime.now();
        ExportForceSitemapQueuePeriodicTask task = new ExportForceSitemapQueuePeriodicTask(
                commonDataStateYDao, forceSitemapExportQueueYDao, workDir, ytService
        );
        when(commonDataStateYDao.getValue(OUR_DATA_TYPE))
                .thenReturn(new CommonDataState(OUR_DATA_TYPE, "", started));
        task.run(UUID.randomUUID());
        verify(commonDataStateYDao).update(argThat(arg -> Objects.equals(arg.getLastUpdate(), started)));
    }

    public void testLatestProcessedTsUpdatesEvenIfNull() throws Exception {
        DateTime started = DateTime.now();
        ExportForceSitemapQueuePeriodicTask task = new ExportForceSitemapQueuePeriodicTask(
                commonDataStateYDao, forceSitemapExportQueueYDao, workDir, ytService
        );
        when(commonDataStateYDao.getValue(OUR_DATA_TYPE)).thenReturn(null);
        task.run(UUID.randomUUID());
        // make sure null value is replaced with something meaningful
        verify(commonDataStateYDao).update(argThat(arg -> arg.getLastUpdate().compareTo(started) < 0));
    }

    public void testYtExceptionRaised() throws Exception {
        ExportForceSitemapQueuePeriodicTask task = new ExportForceSitemapQueuePeriodicTask(
                commonDataStateYDao, forceSitemapExportQueueYDao, workDir, ytService
        );
        doThrow(new YtException("transaction failed")).when(transactionBuilder).execute(any());
        try {
            task.run(UUID.randomUUID());
            fail("exception expected");
        } catch (YtException e) {
            // ok
        }
        verify(commonDataStateYDao, never()).update(any());
    }

    public void testSkipAlreadyProcessedRecords() throws Exception {
        ExportForceSitemapQueuePeriodicTask task = new ExportForceSitemapQueuePeriodicTask(
                commonDataStateYDao, forceSitemapExportQueueYDao, workDir, ytService
        );
        WebmasterHostId hostId = IdUtils.urlToHostId("www.example.org");
        DateTime lastProcessed = DateTime.now();
        setupCassandraState(Arrays.asList(
                Pair.of(hostId, lastProcessed.minus(3000)),  // processed
                Pair.of(hostId, lastProcessed.minus(2000)),  // processed
                Pair.of(hostId, lastProcessed.minus(1000)),  // processed
                Pair.of(hostId, lastProcessed),              // processed (!)
                Pair.of(hostId, lastProcessed.plus(1000)),   // new
                Pair.of(hostId, lastProcessed.plus(2000)),   // new
                Pair.of(hostId, lastProcessed.plus(3000))    // new
        ));
        TableWriter mockTableWriter = mock(TableWriter.class);

        task.populateTable(mockTableWriter, lastProcessed, new MutableObject<>(lastProcessed));

        final int wantedNumberOfRows = 3;
        verify(mockTableWriter, times(wantedNumberOfRows)).column(anyString(), anyString());
        verify(mockTableWriter, times(wantedNumberOfRows)).column(anyString(), anyLong());
        verify(mockTableWriter, times(wantedNumberOfRows)).rowEnd();
    }

    private void setupCassandraState(List<Pair<WebmasterHostId, DateTime>> content) throws Exception {
        doAnswer(invocation -> {
            Consumer<Pair<WebmasterHostId, DateTime>> arg = invocation.getArgument(0);
            content.forEach(p -> arg.accept(p));
            return null;
        }).when(forceSitemapExportQueueYDao).forEach(any());
    }
}
