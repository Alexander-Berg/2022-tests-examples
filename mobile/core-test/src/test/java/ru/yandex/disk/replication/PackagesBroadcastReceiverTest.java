package ru.yandex.disk.replication;

import android.content.Intent;
import android.net.Uri;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.InjectionUtils;
import ru.yandex.disk.app.DiskServiceInfo;
import ru.yandex.disk.app.DiskServicesAnalyzer;
import ru.yandex.disk.provider.DiskContract;
import ru.yandex.disk.test.AndroidTestCase2;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@Config(manifest = Config.NONE)
public class PackagesBroadcastReceiverTest extends AndroidTestCase2 {

    private DiskServicesAnalyzer analyzer;
    private PackagesBroadcastReceiver packagesBroadcastReceiver;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        analyzer = mock(DiskServicesAnalyzer.class);
        packagesBroadcastReceiver = new PackagesBroadcastReceiver();
        InjectionUtils.setUpInjectionServiceForPackagesBroadcastReceiver(analyzer);
    }

    @Test
    public void mustInvalidateAnalizerOnAddedPackage() {
        final Intent intent = new Intent(DiskContract.DISK_PACKAGE_INSTALLED_ACTION);
        packagesBroadcastReceiver.onReceive(getMockContext(), intent);

        verify(analyzer, times(1)).invalidate();
        verifyNoMoreInteractions(analyzer);
    }

    @Test
    public void mustInvalidateAnalizerOnDeletedPackage() {
        final Intent intent = new Intent(Intent.ACTION_PACKAGE_REMOVED);
        intent.setData(Uri.parse("package:ru.yandex.search"));
        when(analyzer.getAllServices())
                .thenReturn(asList(new DiskServiceInfo(1, "id", "ru.yandex.search"),
                        new DiskServiceInfo(1, "id", "ru.yandex.disk")));
        packagesBroadcastReceiver.onReceive(getMockContext(), intent);

        verify(analyzer, times(1)).getAllServices();
        verify(analyzer, times(1)).invalidate();
        verifyNoMoreInteractions(analyzer);
    }

    @Test
    public void mustNotInvalidateIfPackageNotYandex() {
        final Intent intent = new Intent(Intent.ACTION_PACKAGE_REMOVED);
        intent.setData(Uri.parse("package:some.package"));
        packagesBroadcastReceiver.onReceive(getMockContext(), intent);

        verify(analyzer, times(1)).getAllServices();
        verifyNoMoreInteractions(analyzer);
    }
}