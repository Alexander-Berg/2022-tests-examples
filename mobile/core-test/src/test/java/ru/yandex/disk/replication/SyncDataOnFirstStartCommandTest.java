package ru.yandex.disk.replication;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.app.DiskServicesScanner;
import ru.yandex.disk.provider.DiskContract;
import ru.yandex.disk.provider.IncidentContentResolver;
import ru.yandex.disk.storage.MockSharedPreferences;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.upload.AccessMediaLocationCoordinator;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.*;

@Config(manifest = Config.NONE)
public class SyncDataOnFirstStartCommandTest extends AndroidTestCase2 {

    private static final String IS_SETTINGS_FETCHED_ON_FIRST_START = "is_settings_fetched_on_first_start";

    private SyncDataOnFirstStartCommand syncDataOnFirstStartCommand;
    private SharedPreferences prefs;
    private SyncDataOnFirstStartCommandRequest request;
    private Context context;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        context = spy(getMockContext());
        //spying of SharedPreferences calls deadlocks
        prefs = spy(new MockSharedPreferences());
        final IncidentContentResolver cr = new IncidentContentResolver(context.getContentResolver());
        final DiskServicesScanner scanner = new DiskServicesScanner(cr, context.getPackageManager(),
                context, mock(CredentialsManager.class));
        final NeighborsDataAcceptor neighborsDataAcceptor = mock(NeighborsDataAcceptor.class);
        syncDataOnFirstStartCommand = new SyncDataOnFirstStartCommand(prefs, cr, scanner,
                neighborsDataAcceptor, Collections.emptyList(), AccessMediaLocationCoordinator.Stub.INSTANCE,
                context);
        request = new SyncDataOnFirstStartCommandRequest();
    }

    @Test
    public void mustSaveStateToSharedPrefs() throws Exception {
        syncDataOnFirstStartCommand.execute(request);

        assertThat(prefs.getBoolean(IS_SETTINGS_FETCHED_ON_FIRST_START, false), equalTo(true));
    }

    @Test
    public void mustDoNothingIfIsNotFirstRun() throws Exception {
        prefs.edit()
                .putBoolean(IS_SETTINGS_FETCHED_ON_FIRST_START, true)
                .commit();

        syncDataOnFirstStartCommand.execute(request);

        verify(prefs).edit();
        verify(prefs).getBoolean(IS_SETTINGS_FETCHED_ON_FIRST_START, false);
        verifyNoMoreInteractions(prefs);
    }

    @Test
    public void mustSendProperBroadcastOnFirstStart() throws Exception {
        syncDataOnFirstStartCommand.execute(request);

        verify(context).sendBroadcast(argThat(intent ->
                DiskContract.DISK_PACKAGE_INSTALLED_ACTION.equals(intent.getAction())
                        && Uri.parse("package:").equals(intent.getData())));
    }
}
