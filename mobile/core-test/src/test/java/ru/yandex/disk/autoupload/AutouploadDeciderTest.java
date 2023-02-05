package ru.yandex.disk.autoupload;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import com.yandex.disk.BuildConfig;
import ru.yandex.disk.app.DiskServiceInfo;
import ru.yandex.disk.app.DiskServicesAnalyzer;
import ru.yandex.disk.test.AndroidTestCase2;

import static org.mockito.Mockito.*;

@Config(manifest = Config.NONE)
public class AutouploadDeciderTest extends AndroidTestCase2 {
    private static final String DEFAULT_USER_ID = "1";
    private static final String SELF_APP_ID = "ru.yandex.client.disk";
    private AutouploadDecider autouploadDecider;
    private DiskServicesAnalyzer diskServicesAnalyzer;

    @Before
    public void setUp() {
        final Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn(SELF_APP_ID);

        diskServicesAnalyzer = mock(DiskServicesAnalyzer.class);

        autouploadDecider = new AutouploadDecider(context, diskServicesAnalyzer);
    }

    @Test
    public void mustBeEnabledIfNoOldDiskFoundAndIsMaster() {
        when(diskServicesAnalyzer.isOldDiskInstalled()).thenReturn(false);
        when(diskServicesAnalyzer.getMaster())
                .thenReturn(new DiskServiceInfo(1, DEFAULT_USER_ID, SELF_APP_ID));

        assertTrue(autouploadDecider.shouldAutoupload());
    }

    @Test
    public void mustBeDisabledIfOldDiskFoundAndIsMaster() {
        if (BuildConfig.DISK_SERVICE_ENABLED) {
            when(diskServicesAnalyzer.isOldDiskInstalled()).thenReturn(true);
            when(diskServicesAnalyzer.getMaster())
                    .thenReturn(new DiskServiceInfo(1, DEFAULT_USER_ID, SELF_APP_ID));

            assertFalse(autouploadDecider.shouldAutoupload());
        } else {
            assertTrue(autouploadDecider.shouldAutoupload());
        }
    }

    @Test
    public void mustBeDisabledIfOldDiskFoundAndIsNotMaster() {
        if (BuildConfig.DISK_SERVICE_ENABLED) {
            when(diskServicesAnalyzer.isOldDiskInstalled()).thenReturn(true);
            when(diskServicesAnalyzer.getMaster())
                    .thenReturn(new DiskServiceInfo(1, DEFAULT_USER_ID, "another.app.id"));

            assertFalse(autouploadDecider.shouldAutoupload());
        } else {
            assertTrue(autouploadDecider.shouldAutoupload());
        }
    }
}
