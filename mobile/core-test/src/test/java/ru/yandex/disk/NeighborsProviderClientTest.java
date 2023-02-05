package ru.yandex.disk;

import android.content.ContentValues;
import android.net.Uri;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.app.DiskServiceInfo;
import ru.yandex.disk.app.DiskServicesAnalyzer;
import ru.yandex.disk.provider.DiskContentProvider;
import ru.yandex.disk.provider.DiskContract;
import ru.yandex.disk.provider.IncidentContentResolver;
import ru.yandex.disk.replication.NeighborsContentProviderClient;
import ru.yandex.disk.replication.BaseContentProviderClient;
import ru.yandex.disk.stats.EventLog;
import ru.yandex.disk.test.AndroidTestCase2;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.sql.SQLVocabulary.CONTENT;
import static ru.yandex.disk.util.Arrays2.asStringArray;

@Config(manifest = Config.NONE)
public class NeighborsProviderClientTest extends AndroidTestCase2 {

    private DiskServicesAnalyzer analyzer;
    private IncidentContentResolver cr;
    private NeighborsContentProviderClient neighborsProviderClient;
    private static final String DEFAULT_PATH = "disk_queue?user=test";
    private static final List<DiskServiceInfo> MOCKED_INFOS =
            asList(new DiskServiceInfo(1, "1", "mock.app.name"),
                    new DiskServiceInfo(1, "1", "mock.app.name2"));
    private String packageName;
    private static final ContentValues CV = new ContentValues();
    private static final String[] DEFAULT_SELECTION_ARGS = asStringArray("1");
    private static final String DEFAULT_SELECTION = "SOME = ?";

    @BeforeClass
    public static void setUpDefaultValues() {
        CV.put("test", "value");
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        analyzer = mock(DiskServicesAnalyzer.class);
        when(analyzer.getAllServices()).thenReturn(MOCKED_INFOS);

        cr = mock(IncidentContentResolver.class);
        neighborsProviderClient = new NeighborsContentProviderClient(
                DiskContentProvider.getAuthority(getMockContext()), cr, analyzer);
        packageName = getMockContext().getApplicationInfo().packageName;
    }

    @Test
    public void mustInsertToNeighborsServices() throws Exception {
        neighborsProviderClient.insert(DEFAULT_PATH, CV);

        verify(cr, times(2)).insert(any(), eq(CV));
        verify(cr).insert(buildWithPath(MOCKED_INFOS.get(0).getAppName(), DEFAULT_PATH), CV);
        verify(cr).insert(buildWithPath(MOCKED_INFOS.get(1).getAppName(), DEFAULT_PATH), CV);
    }

    @Test
    public void mustDeleteInNeighborsServices() throws Exception {
        neighborsProviderClient.delete(DEFAULT_PATH, DEFAULT_SELECTION, DEFAULT_SELECTION_ARGS);

        verify(cr, times(2)).delete(any(), eq(DEFAULT_SELECTION), eq(DEFAULT_SELECTION_ARGS));
        verify(cr).delete(buildWithPath(MOCKED_INFOS.get(0).getAppName(), DEFAULT_PATH),
                DEFAULT_SELECTION, DEFAULT_SELECTION_ARGS);
        verify(cr).delete(buildWithPath(MOCKED_INFOS.get(1).getAppName(), DEFAULT_PATH),
                DEFAULT_SELECTION, DEFAULT_SELECTION_ARGS);
    }

    @Test
    public void mustUpdateInNeighborsServices() throws Exception {
        neighborsProviderClient.update(DEFAULT_PATH, CV, DEFAULT_SELECTION, DEFAULT_SELECTION_ARGS);

        verify(cr, times(2)).update(any(), eq(CV), eq(DEFAULT_SELECTION), eq(DEFAULT_SELECTION_ARGS));
        verify(cr).update(buildWithPath(MOCKED_INFOS.get(0).getAppName(), DEFAULT_PATH),
                CV, DEFAULT_SELECTION, DEFAULT_SELECTION_ARGS);
        verify(cr).update(buildWithPath(MOCKED_INFOS.get(1).getAppName(), DEFAULT_PATH),
                CV, DEFAULT_SELECTION, DEFAULT_SELECTION_ARGS);
    }

    @Test
    public void mustBulkInsertToNeighborsServices() throws Exception {
        final ContentValues[] values = new ContentValues[]{CV};
        neighborsProviderClient.bulkInsert(DEFAULT_PATH, values);

        verify(cr, times(2)).bulkInsert(any(), eq(values));
        verify(cr).bulkInsert(buildWithPath(MOCKED_INFOS.get(0).getAppName(), DEFAULT_PATH), values);
        verify(cr).bulkInsert(buildWithPath(MOCKED_INFOS.get(1).getAppName(), DEFAULT_PATH), values);
    }

    @Test
    public void mustInsertOnlySelfIfNoOneScanned() {
        when(analyzer.getAllServices()).thenReturn(emptyList());

        neighborsProviderClient.insert(DEFAULT_PATH, CV);
    }

    private Uri buildWithPath(final String auth, final String path) {
        return Uri.parse(CONTENT + auth + DiskContract.DISK_CONTENT_PROVIDER_SUFFIX + "/" + path);
    }

}
