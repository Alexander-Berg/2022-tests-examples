package ru.yandex.disk.replication;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.test.AndroidTestCase2;

import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.provider.DiskContract.Settings.TABLE_NAME;

@Config(manifest = Config.NONE)
public class SettingsReplicatorTest extends AndroidTestCase2 {

    private SettingsReplicator settingsReplicator;
    private DiskContentProviderClient neighborsProviderClient;
    private DiskContentProviderClient selfProviderClient;
    private static final List<String> DEFAULT_SUPPORTED_KEYS =
            asList("supported_key", "supported_key2");
    private static final String SUPPORTED_KEY = DEFAULT_SUPPORTED_KEYS.get(0);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        selfProviderClient = mock(SelfContentProviderClient.class);
        neighborsProviderClient = mock(NeighborsContentProviderClient.class);

        settingsReplicator = new SettingsReplicator(neighborsProviderClient,
                selfProviderClient, DEFAULT_SUPPORTED_KEYS);
    }

    @Test
    public void mustQueryOnlySelf() {
        settingsReplicator.query("scope", "not_supported_key");

        verify(selfProviderClient, times(1))
                .query(eq(TABLE_NAME), any(), any(), any(), any());
        verifyNoMoreInteractions(selfProviderClient);
        verifyNoMoreInteractions(neighborsProviderClient);
    }

    @Test
    public void mustQueryAllOnlySelf() {
        settingsReplicator.queryAll();

        verify(selfProviderClient, times(1))
                .query(eq(TABLE_NAME), isNull(), isNull(),
                        isNull(), isNull());
        verifyNoMoreInteractions(selfProviderClient);
        verifyNoMoreInteractions(neighborsProviderClient);
    }

    @Test
    public void mustInsertOnlySelfOnUnsupportedKey() {
        settingsReplicator.insert("scope", "not_supported_key", "val");

        verify(selfProviderClient, times(1)).insert(eq(TABLE_NAME), any());
        verifyNoMoreInteractions(selfProviderClient);
        verifyNoMoreInteractions(neighborsProviderClient);
    }

    @Test
    public void mustUpdateOnlySelfOnUnsupportedKey() {
        settingsReplicator.insertOrUpdate("scope", "not_supported_key", "val");

        verify(selfProviderClient, times(1)).update(eq(TABLE_NAME), any(), any(), any());
        verifyNoMoreInteractions(selfProviderClient);
        verifyNoMoreInteractions(neighborsProviderClient);
    }

    @Test
    public void mustDeleteOnlySelfOnUnsupportedKey() {
        settingsReplicator.delete("scope", "not_supported_key");

        verify(selfProviderClient, times(1)).delete(eq(TABLE_NAME), any(), any());
        verifyNoMoreInteractions(selfProviderClient);
        verifyNoMoreInteractions(neighborsProviderClient);
    }

    @Test
    public void mustDeleteOnScopeOnlySelfOnUnsupportedKey() {
        settingsReplicator.delete("scope");

        verify(selfProviderClient, times(1)).delete(eq(TABLE_NAME), any(), any());
        verifyNoMoreInteractions(selfProviderClient);
        verifyNoMoreInteractions(neighborsProviderClient);
    }

    @Test
    public void mustInsertAllOnSupportedKey() {
        settingsReplicator.insert("scope", SUPPORTED_KEY, "val");

        verify(neighborsProviderClient, times(1)).insert(eq(TABLE_NAME), any());
        verify(selfProviderClient, times(1)).insert(eq(TABLE_NAME), any());
        verifyNoMoreInteractions(neighborsProviderClient);
        verifyNoMoreInteractions(selfProviderClient);
    }

    @Test
    public void mustUpdateAllOnSupportedKey() {
        settingsReplicator.insertOrUpdate("scope", SUPPORTED_KEY, "val");

        verify(neighborsProviderClient, times(1)).update(eq(TABLE_NAME), any(), any(), any());
        verify(selfProviderClient, times(1)).update(eq(TABLE_NAME), any(), any(), any());
        verifyNoMoreInteractions(neighborsProviderClient);
        verifyNoMoreInteractions(selfProviderClient);
    }

    @Test
    public void mustDeleteAllOnSupportedKey() {
        settingsReplicator.delete("scope", SUPPORTED_KEY);

        verify(neighborsProviderClient, times(1)).delete(eq(TABLE_NAME), any(), any());
        verify(selfProviderClient, times(1)).delete(eq(TABLE_NAME), any(), any());
        verifyNoMoreInteractions(neighborsProviderClient);
        verifyNoMoreInteractions(selfProviderClient);
    }
}
