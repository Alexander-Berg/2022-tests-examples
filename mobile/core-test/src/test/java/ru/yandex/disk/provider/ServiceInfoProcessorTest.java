package ru.yandex.disk.provider;

import android.database.Cursor;
import android.net.Uri;
import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.app.DiskServiceInfo;
import ru.yandex.disk.app.DiskServicesAnalyzer;
import ru.yandex.disk.autoupload.AutouploadDecider;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.util.Booleans;

import static org.mockito.Mockito.*;
import static ru.yandex.disk.util.Arrays2.asStringArray;

@Config(manifest = Config.NONE)
public class ServiceInfoProcessorTest extends AndroidTestCase2 {

    private ServiceInfoProcessor serviceInfoProcessor;
    private AutouploadDecider autouploadDeciderMock;
    private DiskServicesAnalyzer mockAnalyzer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        autouploadDeciderMock = mock(AutouploadDecider.class);
        mockAnalyzer = mock(DiskServicesAnalyzer.class);
        serviceInfoProcessor = new ServiceInfoProcessor(getMockContext(), autouploadDeciderMock,
                mockAnalyzer, false);
    }

    @Test
    public void shouldReturnEmptyCursorOnUnknownRequest() throws Exception {
        try (final Cursor cursor = serviceInfoProcessor.query(Uri.EMPTY, null, null, null, null)) {
            assertFalse(cursor.moveToFirst());
        }
    }

    @Test
    public void shouldReturnAutouploadDeciderState() throws Exception {
        when(autouploadDeciderMock.shouldAutoupload()).thenReturn(true);
        try (final Cursor cursor = serviceInfoProcessor.query(Uri.EMPTY,
                asStringArray(DiskContract.ServiceInfo.SHOULD_AUTOUPLOAD), null, null, null)) {
            if (cursor.moveToFirst()) {
                final boolean b = Booleans.intToBool(cursor.getInt(0));
                assertTrue(b);
            } else {
                fail("Empty cursor!");
            }
        }
    }

    @Test
    public void shouldReturnMasterPackageName() throws Exception {
        final String expectedPackageName = "ru.yandex.testpackage";
        when(mockAnalyzer.getMaster()).thenReturn(new DiskServiceInfo(1, "123", expectedPackageName));
        try (final Cursor cursor = serviceInfoProcessor.query(Uri.EMPTY,
                asStringArray(DiskContract.ServiceInfo.MASTER_PACKAGE_NAME), null, null, null)) {
            if (cursor.moveToFirst()) {
                final String masterPackageName = cursor.getString(0);
                assertEquals(masterPackageName, expectedPackageName);
            } else {
                fail("Empty cursor! Cursor - " + cursor);
            }
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowIfDebug() throws Exception {
        serviceInfoProcessor = new ServiceInfoProcessor(getMockContext(), autouploadDeciderMock,
                mockAnalyzer, true);
        serviceInfoProcessor.query(Uri.EMPTY,
                asStringArray("SOME_UNREGISTERED_PARAM"), null, null, null);
    }

    @Test
    public void shouldReturnIsOldDiskInstalled() throws Exception {
        final boolean mockIsOldDiskInstalled = true;
        when(mockAnalyzer.isOldDiskInstalled()).thenReturn(mockIsOldDiskInstalled);
        try (final Cursor cursor = serviceInfoProcessor.query(Uri.EMPTY,
                asStringArray(DiskContract.ServiceInfo.IS_OLD_DISK_INSTALLED), null, null, null)) {
            if (cursor.moveToFirst()) {
                assertTrue(Booleans.intToBool(cursor.getInt(0)));
            } else {
                fail("Empty cursor! Cursor - " + cursor);
            }
        }

    }
}